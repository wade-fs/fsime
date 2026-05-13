/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.utilities

import java.util.regex.Pattern

object Stringy {
    @JvmStatic
    fun isAscii(string: String): Boolean {
        return string.matches("\\p{ASCII}*".toRegex())
    }

    @JvmStatic
    fun removePrefixRegex(prefixRegex: String, string: String): String {
        return string.replaceFirst("^$prefixRegex".toRegex(), "")
    }

    @JvmStatic
    fun removeSuffixRegex(suffixRegex: String, string: String): String {
        return string.replaceFirst("$suffixRegex$".toRegex(), "")
    }

    @JvmStatic
    fun removePrefix(prefix: String?, string: String): String {
        return removePrefixRegex(Pattern.quote(prefix), string)
    }

    /*
    Get the first (unicode) code point.
  */
    @JvmStatic
    fun getFirstCodePoint(string: String): Int {
        return string.codePointAt(0)
    }

    /*
    Convert a string to a list of (unicode) code points.
  */
    @JvmStatic
    fun toCodePointList(string: String): List<Int> {
        val codePointList: MutableList<Int> = ArrayList()
        val charCount = string.length
        var charIndex = 0
        while (charIndex < charCount) {
            val codePoint = string.codePointAt(charIndex)
            codePointList.add(codePoint)
            charIndex += Character.charCount(codePoint)
        }
        return codePointList
    }

    /*
    Add the (unicode) code points of a string to a set
  */
    fun addCodePointsToSet(string: String, set: MutableSet<Int?>) {
        val charCount = string.length
        var charIndex = 0
        while (charIndex < charCount) {
            val codePoint = string.codePointAt(charIndex)
            set.add(codePoint)
            charIndex += Character.charCount(codePoint)
        }
    }

    /*
    Convert a code point to a string
  */
    fun toString(codePoint: Int): String {
        return String(Character.toChars(codePoint))
    }

    /*
    Convert a string to a list of (unicode) characters.
  */
    fun toCharacterList(string: String): List<String> {
        val characterList: MutableList<String> = ArrayList()
        val codePointCount = string.codePointCount(0, string.length)
        for (codePointIndex in 0 until codePointCount) {
            characterList.add(
                string.substring(
                    string.offsetByCodePoints(0, codePointIndex),
                    string.offsetByCodePoints(0, codePointIndex + 1)
                )
            )
        }
        return characterList
    }

    /*
    Sunder a string into two at the first occurrence of a delimiter.
  */
    fun sunder(string: String, delimiter: String): Array<String> {
        val delimiterIndex = string.indexOf(delimiter)
        val delimiterLength = delimiter.length
        val substringBeforeDelimiter: String
        val substringAfterDelimiter: String
        if (delimiterIndex < 0) {
            substringBeforeDelimiter = string
            substringAfterDelimiter = ""
        } else {
            substringBeforeDelimiter = string.substring(0, delimiterIndex)
            substringAfterDelimiter = string.substring(delimiterIndex + delimiterLength)
        }
        return arrayOf(substringBeforeDelimiter, substringAfterDelimiter)
    }
}