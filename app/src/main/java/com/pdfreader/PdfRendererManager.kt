package com.pdfreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererManager(private val context: Context) {

    private var renderer: PdfRenderer? = null
    private var currentUri: Uri? = null

    val pageCount: Int get() = renderer?.pageCount ?: 0

    suspend fun open(uri: Uri): Int = withContext(Dispatchers.IO) {
        close()
        currentUri = uri
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        renderer = PdfRenderer(fd!!)
        renderer!!.pageCount
    }

    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap = withContext(Dispatchers.IO) {
        val page = renderer!!.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmap
    }

    fun close() {
        renderer?.close()
        renderer = null
        currentUri = null
    }

    fun copyToCache(uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val cacheFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
            FileOutputStream(cacheFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            return cacheFile
        } catch (e: Exception) {
            return null
        }
    }
}
