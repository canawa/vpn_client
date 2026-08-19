package ru.coffeemaniavpn.app.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.LoadBalancer
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.ServerPinger
import ru.coffeemaniavpn.app.util.AppLog

/**
 * NORMAL/BS балансировка с "реальным" health-check через туннель.
 *
 * Идея:
 * - Пока есть рабочий NORMAL_POOL → BS не трогаем.
 * - Если текущий NORMAL перестал проходить health-check → пробуем другой NORMAL.
 * - Только если весь NORMAL_POOL не проходит → пробуем BS_POOL.
 * - На BS_POOL периодически проверяем NORMAL_POOL и возвращаемся автоматически.
 */
object VpnPoolBalancer {
    private val switchMutex = Mutex()
    private var loopJob: kotlinx.coroutines.Job? = null

    private const val HEALTH_CHECK_INTERVAL_MS = 15_000L
    private const val CURRENT_NODE_FAIL_THRESHOLD = 2
    private const val VERIFY_SUCCESS_STREAK = 2
    private const val VERIFY_BETWEEN_CHECKS_MS = 1_500L

    private const val SWITCH_TIMEOUT_MS = 25_000L
    private const val POST_SWITCH_SETTLE_MS = 2_000L

    private const val BS_NORMAL_RECHECK_INTERVAL_MS = 10 * 60 * 1000L

    private var currentFailStreak = 0
    private var lastBsNormalRecheckAtMs: Long = 0L

    fun onVpnStarted() {
        if (loopJob?.isActive == true) return
        loopJob = App.applicationScope.launch(Dispatchers.Default) {
            runLoop()
        }
    }

    fun onVpnStopped() {
        loopJob?.cancel()
        loopJob = null
        currentFailStreak = 0
        lastBsNormalRecheckAtMs = 0L
    }

    private suspend fun runLoop() {
        // Задержка, чтобы core и stats успели стартовать.
        delay(2_000L)
        while (kotlin.coroutines.coroutineContext.isActive) {
            if (!isAutoMode()) {
                onVpnStopped()
                return
            }

            if (VpnManager.status.value != VpnStatus.Started) {
                delay(2_000L)
                continue
            }

            val current = VpnAutoReconnect.connectedNode()
            if (current == null) {
                delay(2_000L)
                continue
            }

            val currentIsBs = LoadBalancer.isBsServer(current)
            val now = System.currentTimeMillis()
            val needNormalRecheckFromBs =
                currentIsBs && (now - lastBsNormalRecheckAtMs >= BS_NORMAL_RECHECK_INTERVAL_MS)

            if (needNormalRecheckFromBs) {
                lastBsNormalRecheckAtMs = now
                val returned = tryReturnToWorkingNormal(forceTest = true)
                if (returned) {
                    currentFailStreak = 0
                    delay(HEALTH_CHECK_INTERVAL_MS)
                    continue
                }
            }

            val ok = checkCurrentNodeHealth()
            if (ok) {
                currentFailStreak = 0
            } else {
                currentFailStreak++
                if (currentFailStreak < CURRENT_NODE_FAIL_THRESHOLD) {
                    delay(HEALTH_CHECK_INTERVAL_MS)
                    continue
                }

                AppLog.w("VpnPoolBalancer current node unhealthy → switching (node=${current.name} bs=$currentIsBs)")
                val switched = if (currentIsBs) {
                    // В BS: сначала пытаемся вернуться в NORMAL, иначе остаёмся в BS
                    tryReturnToWorkingNormal(forceTest = false)
                        || trySwitchToWorkingBs()
                } else {
                    // В NORMAL: пробуем другой NORMAL, BS трогаем только если весь NORMAL даун
                    trySwitchToWorkingNormalThenBs()
                }

                if (!switched) {
                    VpnManager.setError("Нет рабочих серверов")
                }
                currentFailStreak = 0
            }

            delay(HEALTH_CHECK_INTERVAL_MS)
        }
    }

    private suspend fun isAutoMode(): Boolean {
        val prefs = AppPreferences(App.instance)
        return prefs.selectedNodeId.first() == LoadBalancer.AUTO_NODE_ID
    }

    /** Health-check текущего подключения: туннель прокидывает generate_204. */
    private fun checkCurrentNodeHealth(): Boolean {
        val delayMs = XrayCoreManager.measureDelayAny() ?: return false
        return delayMs > 0L
    }

    private suspend fun verifyNodeHealthAfterSwitch(node: ProxyNode): Boolean {
        // На старте измерения могут быть "пустыми": делаем 2 попытки с паузой.
        repeat(VERIFY_SUCCESS_STREAK) { attempt ->
            val current = VpnAutoReconnect.connectedNode()
            if (current?.id != node.id) return false
            if (!checkCurrentNodeHealth()) return false
            if (attempt + 1 < VERIFY_SUCCESS_STREAK) delay(VERIFY_BETWEEN_CHECKS_MS)
        }
        return true
    }

    private suspend fun switchToAndVerify(node: ProxyNode): Boolean {
        if (VpnManager.status.value == VpnStatus.Started) {
            if (VpnAutoReconnect.connectedNode()?.id == node.id) {
                return verifyNodeHealthAfterSwitch(node)
            }
        }

        switchMutex.withLock {
            // Переключаем туннель на node.
            if (VpnManager.status.value == VpnStatus.Started) {
                VpnManager.switchToNode(node)
            } else {
                VpnManager.connect(node)
            }

            val startAt = System.currentTimeMillis()
            while (VpnManager.status.value != VpnStatus.Started &&
                System.currentTimeMillis() - startAt < SWITCH_TIMEOUT_MS
            ) {
                delay(250L)
            }

            if (VpnManager.status.value != VpnStatus.Started) return false
            delay(POST_SWITCH_SETTLE_MS)
            return verifyNodeHealthAfterSwitch(node)
        }
    }

    private suspend fun trySwitchToWorkingNormalThenBs(): Boolean {
        val nodes = AppPreferences(App.instance).nodes.first()
        val normal = nodes
            .filterNot { LoadBalancer.isBsServer(it) }
            .filterNot { LoadBalancer.isRussianServer(it) }
        val bs = nodes
            .filter { LoadBalancer.isBsServer(it) }
            .filterNot { LoadBalancer.isRussianServer(it) }

        if (normal.isNotEmpty()) {
            val chosen = pickWorkingByTesting(normal, label = "NORMAL")
            if (chosen != null) return true
        }
        if (bs.isNotEmpty()) {
            val chosen = pickWorkingByTesting(bs, label = "BS")
            if (chosen != null) return true
        }
        return false
    }

    private suspend fun trySwitchToWorkingBs(): Boolean {
        val nodes = AppPreferences(App.instance).nodes.first()
        val bs = nodes
            .filter { LoadBalancer.isBsServer(it) }
            .filterNot { LoadBalancer.isRussianServer(it) }
        if (bs.isEmpty()) return false
        val chosen = pickWorkingByTesting(bs, label = "BS")
        return chosen != null
    }

    /**
     * В BS режиме периодически пытаемся вернуть NORMAL.
     * forceTest=false означает: вернёмся только если конкретная проверка прошла,
     * а не "попробовали и считаем норм".
     */
    private suspend fun tryReturnToWorkingNormal(forceTest: Boolean): Boolean {
        // forceTest пока не меняет поведение (health-check один и тот же),
        // но оставлено для будущей hysteresis-логики.
        val nodes = AppPreferences(App.instance).nodes.first()
        val normal = nodes
            .filterNot { LoadBalancer.isBsServer(it) }
            .filterNot { LoadBalancer.isRussianServer(it) }
        if (normal.isEmpty()) return false
        val chosen = pickWorkingByTesting(normal, label = "NORMAL-return")
        return chosen != null
    }

    /**
     * Последовательно тестируем кандидатов:
     * - сортировка по TCP-ping (это только "очередь на попытку")
     * - реальная проверка health-check — через туннель (measureDelayAny)
     */
    private suspend fun pickWorkingByTesting(
        candidates: List<ProxyNode>,
        label: String,
    ): ProxyNode? {
        val list = candidates.toList()
        if (list.isEmpty()) return null

        val pingSorted = withContext(Dispatchers.IO) {
            val pings = mutableMapOf<String, PingState>()
            ServerPinger.pingAll(list) { id, state -> pings[id] = state }
            list.sortedBy { node ->
                val ping = pings[node.id]
                (ping as? PingState.Result)?.latencyMs ?: Int.MAX_VALUE
            }
        }

        // По твоей схеме BS включаем только когда НЕ работает весь NORMAL_POOL.
        // Поэтому тестируем кандидатов по очереди до первого success (фактически — весь пул при необходимости).
        val toTry = pingSorted

        AppLog.i("VpnPoolBalancer $label testing candidates=${toTry.joinToString { it.name }}")
        for (node in toTry) {
            val ok = switchToAndVerify(node)
            if (ok) {
                AppLog.i("VpnPoolBalancer $label selected=${node.name}")
                return node
            }
        }
        return null
    }
}

