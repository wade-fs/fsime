/*
  Copyright 2021 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.utilities

fun <V, K> invertMap(forwardMap: Map<K, V>): Map<V, K> {
    val inverseMap: MutableMap<V, K> = HashMap()
    for ((key, value) in forwardMap) {
        inverseMap[value] = key
    }
    return inverseMap
}