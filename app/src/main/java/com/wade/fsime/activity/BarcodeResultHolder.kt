package com.wade.fsime.activity

/**
 * A simple singleton to hold Barcode results temporarily between BarcodeActivity and FsimeService.
 */
object BarcodeResultHolder {
    var pendingResult: String? = null
}
