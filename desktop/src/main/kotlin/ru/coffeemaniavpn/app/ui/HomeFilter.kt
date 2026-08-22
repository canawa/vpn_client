package ru.coffeemaniavpn.app.ui

import ru.coffeemaniavpn.app.data.HomeFilterOrder

sealed interface HomeFilter {
    data object All : HomeFilter
    data class Category(val category: ServerCategory) : HomeFilter
}

fun homeFilterFromId(id: String): HomeFilter? = when (id) {
    HomeFilterOrder.ALL_ID -> HomeFilter.All
    else -> ServerCategory.entries.find { it.name == id && it != ServerCategory.AUTO }
        ?.let { HomeFilter.Category(it) }
}
