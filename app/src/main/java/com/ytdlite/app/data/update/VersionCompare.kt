package com.ytdlite.app.data.update

/** Сравнение версий вида «0.1.0», «2026.11.01» или «v1.2.3-nightly». */
object VersionCompare {

    /** true, если [latest] новее [current] (сравнение по числовым частям). */
    fun isNewer(latest: String, current: String): Boolean {
        val a = latest.removePrefix("v").trim().split('.', '-', '+')
        val b = current.removePrefix("v").trim().split('.', '-', '+')
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrNull(i)?.toIntOrNull() ?: 0
            val right = b.getOrNull(i)?.toIntOrNull() ?: 0
            if (left != right) return left > right
        }
        return false
    }
}
