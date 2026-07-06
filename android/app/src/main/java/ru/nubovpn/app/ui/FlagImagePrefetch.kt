package ru.nubovpn.app.ui

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest

object FlagImagePrefetcher {
    private const val LIST_FLAG_PX = 80

    fun prefetch(context: Context, flags: Collection<String>) {
        val loader = context.imageLoader
        flags.distinct().forEach { flag ->
            val cacheKey = FlagUtils.emojiToCountryCode(flag) ?: flag
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(FlagUtils.flagImageUrl(flag, pixelWidth = LIST_FLAG_PX))
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .build(),
            )
        }
    }
}
