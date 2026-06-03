package online.coffemaniavpn.client.data

/**
 * Человекочитаемый остаток срока подписки: месяцы, недели, дни;
 * при малом остатке — часы (и минуты, если меньше часа).
 */
object SubscriptionExpireFormatter {
    private const val SEC_MINUTE = 60L
    private const val SEC_HOUR = 3_600L
    private const val SEC_DAY = 86_400L
    private const val SEC_WEEK = 7 * SEC_DAY
    private const val SEC_MONTH = 30 * SEC_DAY

    fun formatRemaining(remainingSec: Long): String {
        if (remainingSec <= 0) return "Подписка истекла"

        val parts = buildParts(remainingSec)
        return "Истекает через ${joinParts(parts)}"
    }

    private fun buildParts(remainingSec: Long): List<String> {
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
                parts += formatUnit(minutes.coerceAtLeast(1), "минута", "минуты", "минут")

            remainingSec < SEC_DAY ->
                parts += formatUnit(hours.coerceAtLeast(1), "час", "часа", "часов")

            remainingSec < 2 * SEC_DAY -> {
                val dayCount = (remainingSec / SEC_DAY).coerceAtLeast(1)
                parts += formatUnit(dayCount, "день", "дня", "дней")
                if (hours > 0) {
                    parts += formatUnit(hours, "час", "часа", "часов")
                }
            }

            else -> {
                if (months > 0) {
                    parts += formatUnit(months, "месяц", "месяца", "месяцев")
                }
                if (weeks > 0) {
                    parts += formatUnit(weeks, "неделя", "недели", "недель")
                }
                if (days > 0) {
                    parts += formatUnit(days, "день", "дня", "дней")
                }
                if (parts.isEmpty()) {
                    parts += formatUnit(1, "день", "дня", "дней")
                }
            }
        }

        return parts
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
