package com.wade.MathParser.exception

import com.wade.MathParser.Utils

open class MathParserException : Exception {
    val source: String?
    var localMessage: String = ""
    val index: Int

    constructor(src: String?, index: Int, message: String?) : super(
        message + generateMessages(
            src,
            index
        )
    ) {
        if (message != null) {
            localMessage = message
        }
        source = src
        this.index = index
    }

    constructor(source: String?, message: String, cause: Throwable?) : super(message, cause) {
        localMessage = message
        this.source = source
        index = -1
    }

    val cursor: String
        get() = getCursor(index)

    companion object {
        private fun getCursor(index: Int): String {
            return Utils.repeat(' ', index - 1) + "^"
        }

        private fun generateMessages(src: String?, index: Int): String {
            return if (index == -1 || src == null || src.isEmpty()) "" else "\n\t" + src + "\n\t" + getCursor(
                index
            )
        }
    }
}