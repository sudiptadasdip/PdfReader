package com.pdfreader

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var rendererManager: PdfRendererManager
    private lateinit var viewPager: ViewPager2
    private lateinit var pageIndicator: TextView
    private lateinit var toolbar: MaterialToolbar
    private var adapter: PdfPageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        rendererManager = PdfRendererManager(this)
        viewPager = findViewById(R.id.viewPager)
        pageIndicator = findViewById(R.id.pageIndicator)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }

        val fileName = getFileName(uri)
        toolbar.title = fileName

        loadPdf(uri)
    }

    private fun loadPdf(uri: Uri) {
        val file = rendererManager.copyToCache(uri)
        if (file == null) {
            finish()
            return
        }

        val cacheUri = Uri.fromFile(file)

        val pageCount = try {
            rendererManager.open(cacheUri)
        } catch (e: Exception) {
            finish()
            return
        }

        adapter = PdfPageAdapter(pageCount, rendererManager, viewPager)
        viewPager.adapter = adapter

        updatePageIndicator(0, pageCount)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position, pageCount)
            }
        })
    }

    private fun updatePageIndicator(current: Int, total: Int) {
        pageIndicator.text = getString(R.string.page_number, current + 1, total)
    }

    private fun getFileName(uri: Uri): String {
        var name = "PDF"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.clearCache()
        rendererManager.close()
    }
}
