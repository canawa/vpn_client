package ru.coffeemaniavpn.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Остаток срока подписки:
 * — от суток и больше: только дни;
 * — меньше суток: часы и/или минуты.
 */
object SubscriptionExpireFormatter {
    private const val SEC_MINUTE = 60L
    private const val SEC_HOUR = 3_600L
    private const val SEC_DAY = 86_400L

    private val updatedAtFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))
    private val calendarDateFormat = SimpleDateFormat("d MMMM yyyy 'г.'", Locale.forLanguageTag("ru"))

    fun formatCalendarDate(expireEpochSec: Long): String =
        calendarDateFormat.format(Date(expireEpochSec * 1_000L))

    fun formatRemaining(remainingSec: Long): String {
        if (remainingSec <= 0) return "Подписка истекла"

        val parts = buildParts(remainingSec)
        return "Истекает через ${joinParts(parts)}"
    }

    fun formatUpdatedAt(epochMs: Long): String? {
        if (epochMs <= 0L) return null
        return "Обновлено: ${updatedAtFormat.format(Date(epochMs))}"
    }

    private fun buildParts(remainingSec: Long): List<String> {
        if (remainingSec >= SEC_DAY) {
            val days = remainingSec / SEC_DAY
            return listOf(formatUnit(days, "день", "дня", "дней"))
        }

        val hours = remainingSec / SEC_HOUR
        val minutes = ((remainingSec % SEC_HOUR) + SEC_MINUTE - 1) / SEC_MINUTE

        return when {
            hours <= 0L -> listOf(
                formatUnit(minutes.coerceAtLeast(1), "минута", "минуты", "минут"),
            )
            minutes <= 0L -> listOf(
                formatUnit(hours, "час", "часа", "часов"),
            )
            else -> listOf(
                formatUnit(hours, "час", "часа", "часов"),
                formatUnit(minutes, "минута", "минуты", "минут"),
            )
        }
    }

    private fun formatUnit(
        count: Long,
        one: String,
        few: String,
        many: String,
    ): String = "$count ${pluralForm(count, one, few, many)}"

    private fun pluralForm(count: Long, one: String, few: String, many: String): String {
        val mod10 = count % 10
        val mod100 = count % 100
        return when {
            mod100 in 11L..14L -> many
            mod10 == 1L -> one
            mod10 in 2L..4L -> few
            else -> many
        }
    }

    private fun joinParts(parts: List<String>): String = when (parts.size) {
        1 -> parts[0]
        2 -> "${parts[0]} и ${parts[1]}"
        else -> parts.dropLast(1).joinToString(", ") + " и ${parts.last()}"
    }
}
