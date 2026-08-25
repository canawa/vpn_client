package work.bavshield.vpn.data

import android.content.res.Resources
import work.bavshield.vpn.R

object SubscriptionExpireFormatter {
    private const val SEC_MINUTE = 60L
    private const val SEC_HOUR = 3_600L
    private const val SEC_DAY = 86_400L
    private const val SEC_WEEK = 7 * SEC_DAY
    private const val SEC_MONTH = 30 * SEC_DAY

    fun formatRemaining(resources: Resources, remainingSec: Long): String {
        if (remainingSec <= 0) return resources.getString(R.string.subscription_expired)

        val parts = buildParts(resources, remainingSec)
        return resources.getString(R.string.expires_in, joinParts(resources, parts))
    }

    private fun buildParts(resources: Resources, remainingSec: Long): List<String> {
        var sec = remainingSec
        val months = sec / SEC_MONTH
        sec %= SEC_MONTH
        val weeks = sec / SEC_WEEK
        sec %= SEC_WEEK
        val days = sec / SEC_DAY
        sec %= SEC_DAY
        val hours = sec / SEC_HOUR
        sec %= SEC_HOUR
        val minutes = (sec + SEC_MINUTE - 1) / SEC_MINUTE

        val parts = mutableListOf<String>()

        when {
            remainingSec < SEC_HOUR ->
                parts += formatUnit(resources, R.plurals.duration_minutes, minutes.coerceAtLeast(1))

            remainingSec < SEC_DAY ->
                parts += formatUnit(resources, R.plurals.duration_hours, hours.coerceAtLeast(1))

            remainingSec < 2 * SEC_DAY -> {
                val dayCount = (remainingSec / SEC_DAY).coerceAtLeast(1)
                parts += formatUnit(resources, R.plurals.duration_days, dayCount)
                if (hours > 0) {
                    parts += formatUnit(resources, R.plurals.duration_hours, hours)
                }
            }

            else -> {
                if (months > 0) {
                    parts += formatUnit(resources, R.plurals.duration_months, months)
                }
                if (weeks > 0) {
                    parts += formatUnit(resources, R.plurals.duration_weeks, weeks)
                }
                if (days > 0) {
                    parts += formatUnit(resources, R.plurals.duration_days, days)
                }
                if (parts.isEmpty()) {
                    parts += formatUnit(resources, R.plurals.duration_days, 1)
                }
            }
        }

        return parts
    }

    private fun formatUnit(resources: Resources, pluralsRes: Int, count: Long): String {
        val quantity = count.toInt().coerceAtMost(Int.MAX_VALUE)
        return resources.getQuantityString(pluralsRes, quantity, quantity)
    }

    private fun joinParts(resources: Resources, parts: List<String>): String = when (parts.size) {
        1 -> parts[0]
        2 -> resources.getString(R.string.list_and, parts[0], parts[1])
        else -> {
            val head = parts.dropLast(1).joinToString(resources.getString(R.string.list_comma))
            resources.getString(R.string.list_and, head, parts.last())
        }
    }
}
