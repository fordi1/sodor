package com.example.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfPrintAdapter(
    private val context: Context,
    private val pdfFile: File
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val pInfo = PrintDocumentInfo.Builder("pdf_invoice.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(pInfo, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            val input = FileInputStream(pdfFile)
            val output = FileOutputStream(destination?.fileDescriptor)
            
            val buffer = ByteArray(1024)
            var lengthCursor: Int
            while (input.read(buffer).also { lengthCursor = it } > 0) {
                output.write(buffer, 0, lengthCursor)
            }
            
            input.close()
            output.flush()
            output.close()
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            e.printStackTrace()
            callback?.onWriteFailed(e.toString())
        }
    }
}
