package ru.coffeemaniavpn.app.data

import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.R
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

    private fun locale(): Locale = Locale.getDefault()

    private fun updatedAtFormat(): SimpleDateFormat =
        SimpleDateFormat("dd.MM.yyyy HH:mm", locale())

    private fun calendarDateFormat(): SimpleDateFormat =
        if (locale().language == "ru") {
            SimpleDateFormat("d MMMM yyyy 'г.'", Locale.forLanguageTag("ru"))
        } else {
            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
        }

    fun formatCalendarDate(expireEpochSec: Long): String =
        calendarDateFormat().format(Date(expireEpochSec * 1_000L))

    fun formatRemaining(remainingSec: Long): String {
        if (remainingSec <= 0) {
            return App.instance.getString(R.string.subscription_expired)
        }
        val parts = if (locale().language == "ru") {
            buildPartsRu(remainingSec)
        } else {
            buildPartsEn(remainingSec)
        }
        return App.instance.getString(R.string.subscription_expires_in, joinParts(parts))
    }

    fun formatUpdatedAt(epochMs: Long): String? {
        if (epochMs <= 0L) return null
        return App.instance.getString(
            R.string.subscription_updated_at,
            updatedAtFormat().format(Date(epochMs)),
        )
    }

    private fun buildPartsRu(remainingSec: Long): List<String> {
        if (remainingSec >= SEC_DAY) {
            val days = remainingSec / SEC_DAY
            return listOf(formatUnitRu(days, "день", "дня", "дней"))
        }

        val hours = remainingSec / SEC_HOUR
        val minutes = ((remainingSec % SEC_HOUR) + SEC_MINUTE - 1) / SEC_MINUTE

        return when {
            hours <= 0L -> listOf(
                formatUnitRu(minutes.coerceAtLeast(1), "минута", "минуты", "минут"),
            )
            minutes <= 0L -> listOf(
                formatUnitRu(hours, "час", "часа", "часов"),
            )
            else -> listOf(
                formatUnitRu(hours, "час", "часа", "часов"),
                formatUnitRu(minutes, "минута", "минуты", "минут"),
            )
        }
    }

    private fun buildPartsEn(remainingSec: Long): List<String> {
        if (remainingSec >= SEC_DAY) {
            val days = remainingSec / SEC_DAY
            return listOf(formatUnitEn(days, "day", "days"))
        }

        val hours = remainingSec / SEC_HOUR
        val minutes = ((remainingSec % SEC_HOUR) + SEC_MINUTE - 1) / SEC_MINUTE

        return when {
            hours <= 0L -> listOf(
                formatUnitEn(minutes.coerceAtLeast(1), "minute", "minutes"),
            )
            minutes <= 0L -> listOf(
                formatUnitEn(hours, "hour", "hours"),
            )
            else -> listOf(
                formatUnitEn(hours, "hour", "hours"),
                formatUnitEn(minutes, "minute", "minutes"),
            )
        }
    }

    private fun formatUnitRu(
        count: Long,
        one: String,
        few: String,
        many: String,
    ): String = "$count ${pluralFormRu(count, one, few, many)}"

    private fun formatUnitEn(count: Long, one: String, many: String): String {
        val unit = if (count == 1L) one else many
        return "$count $unit"
    }

    private fun pluralFormRu(count: Long, one: String, few: String, many: String): String {
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
        2 -> if (locale().language == "ru") {
            "${parts[0]} и ${parts[1]}"
        } else {
            "${parts[0]} and ${parts[1]}"
        }
        else -> if (locale().language == "ru") {
            parts.dropLast(1).joinToString(", ") + " и ${parts.last()}"
        } else {
            parts.dropLast(1).joinToString(", ") + " and ${parts.last()}"
        }
    }
}
