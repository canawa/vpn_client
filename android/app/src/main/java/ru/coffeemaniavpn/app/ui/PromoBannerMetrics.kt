package ru.coffeemaniavpn.app.ui

/**
 * Расчёт размеров промо-баннера под доступную область экрана.
 * Чистая функция — покрыта unit-тестами для phone/tablet/landscape.
 */
object PromoBannerMetrics {
    const val MIN_HEIGHT_DP = 72f
    const val PHONE_MAX_HEIGHT_DP = 160f
    const val TABLET_MAX_HEIGHT_DP = 140f
    const val EXPANDED_MAX_HEIGHT_DP = 128f
    const val SHORT_MAX_HEIGHT_DP = 96f

    const val TABLET_MAX_WIDTH_DP = 420f
    const val MAX_HEIGHT_FRACTION = 0.22f
    const val TABLET_WIDTH_FRACTION = 0.6f

    const val SHORT_SCREEN_HEIGHT_DP = 480f
    const val MEDIUM_WIDTH_DP = 600f
    const val EXPANDED_WIDTH_DP = 840f

    data class Spec(
        /** null — растягивать на всю ширину родителя */
        val maxWidthDp: Float?,
        val maxHeightDp: Float,
    )

    data class FittedSize(
        val widthDp: Float,
        val heightDp: Float,
    )

    fun compute(widthDp: Float, heightDp: Float): Spec {
        val width = widthDp.coerceAtLeast(0f)
        val height = heightDp.coerceAtLeast(0f)

        val absoluteCap = when {
            width >= EXPANDED_WIDTH_DP -> EXPANDED_MAX_HEIGHT_DP
            width >= MEDIUM_WIDTH_DP -> TABLET_MAX_HEIGHT_DP
            height > 0f && height < SHORT_SCREEN_HEIGHT_DP -> SHORT_MAX_HEIGHT_DP
            else -> PHONE_MAX_HEIGHT_DP
        }

        val fractionalCap = if (height > 0f) {
            height * MAX_HEIGHT_FRACTION
        } else {
            absoluteCap
        }

        val maxHeight = minOf(absoluteCap, fractionalCap)
            .coerceIn(MIN_HEIGHT_DP, PHONE_MAX_HEIGHT_DP)

        val maxWidth = if (width >= MEDIUM_WIDTH_DP) {
            minOf(TABLET_MAX_WIDTH_DP, width * TABLET_WIDTH_FRACTION)
        } else {
            null
        }

        return Spec(maxWidthDp = maxWidth, maxHeightDp = maxHeight)
    }

    /**
     * Вписывает баннер в maxW×maxH с сохранением aspect (width/height),
     * чтобы контейнер совпадал с картинкой — без «ушей» и выхода за края.
     */
    fun fitInside(maxWidthDp: Float, maxHeightDp: Float, aspectRatio: Float): FittedSize {
        val maxW = maxWidthDp.coerceAtLeast(0f)
        val maxH = maxHeightDp.coerceAtLeast(0f)
        if (aspectRatio <= 0f || !aspectRatio.isFinite()) {
            return FittedSize(widthDp = maxW, heightDp = maxH)
        }
        if (maxW <= 0f || maxH <= 0f) {
            return FittedSize(0f, 0f)
        }

        val heightIfFullWidth = maxW / aspectRatio
        return if (heightIfFullWidth <= maxH) {
            FittedSize(widthDp = maxW, heightDp = heightIfFullWidth)
        } else {
            FittedSize(widthDp = maxH * aspectRatio, heightDp = maxH)
        }
    }

    fun resolveContentWidthDp(availableWidthDp: Float, spec: Spec): Float =
        spec.maxWidthDp?.let { minOf(it, availableWidthDp) } ?: availableWidthDp
}
