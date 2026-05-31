package com.pdfreader

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPageAdapter(
    private val pageCount: Int,
    private val rendererManager: PdfRendererManager,
    private val viewPager: ViewPager2
) : RecyclerView.Adapter<PdfPageAdapter.PageHolder>() {

    private val cache = mutableMapOf<Int, Bitmap>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var jobs = mutableMapOf<Int, Job>()

    inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.pageImage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val cached = cache[position]
        if (cached != null) {
            holder.progressBar.visibility = View.GONE
            holder.imageView.setImageBitmap(cached)
            return
        }

        holder.progressBar.visibility = View.VISIBLE
        holder.imageView.setImageBitmap(null)

        val width = holder.imageView.context.resources.displayMetrics.widthPixels
        val height = (width * 1.414).toInt()

        jobs[position]?.cancel()
        jobs[position] = scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    rendererManager.renderPage(position, width, height)
                }
                cache[position] = bitmap
                holder.progressBar.visibility = View.GONE
                holder.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.progressBar.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = pageCount

    override fun onViewDetachedFromWindow(holder: PageHolder) {
        super.onViewDetachedFromWindow(holder)
        val pos = holder.layoutPosition
        if (pos != RecyclerView.NO_POSITION) {
            jobs[pos]?.cancel()
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
