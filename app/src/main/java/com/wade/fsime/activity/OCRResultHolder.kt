package com.wade.fsime.activity

/**
 * A simple singleton to hold OCR results temporarily between OCRActivity and FsimeService.
 */
object OCRResultHolder {
    var pendingResult: String? = null
}
