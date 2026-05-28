package com.indicswipe.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable

class GifAdapter(private val onGifSelected: (String) -> Unit) : RecyclerView.Adapter<GifAdapter.GifViewHolder>() {

    private var gifUrls: List<String> = emptyList()

    fun setGifs(urls: List<String>) {
        this.gifUrls = urls
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gif, parent, false)
        return GifViewHolder(view)
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        val url = gifUrls[position]
        val density = holder.imageView.context.resources.displayMetrics.density
        val radiusPx = (12 * density).toInt()
        
        Glide.with(holder.imageView.context)
            .asGif()
            .load(url)
            .centerCrop()
            .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), com.bumptech.glide.load.resource.bitmap.RoundedCorners(radiusPx))
            .placeholder(R.drawable.bg_search_bar)
            .into(holder.imageView)
        
        holder.itemView.setOnClickListener { onGifSelected(url) }
    }

    override fun getItemCount(): Int = gifUrls.size

    class GifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.gif_image_view)
    }
}