package com.indicswipe.app

import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

sealed class EmojiItem {
    data class Header(val title: String, val categoryId: Int) : EmojiItem()
    data class Emoji(val code: String, val isKaomoji: Boolean = false) : EmojiItem()

    fun id(): String = when(this) {
        is Header -> "header_$title"
        is Emoji -> "emoji_$code"
    }
}

class EmojiAdapter(
    private val onEmojiClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<EmojiItem> = emptyList()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_EMOJI = 1
    }

    private var headerTextColor: Int = 0x88888888.toInt()
    
    fun updateTheme(textColor: Int) {
        this.headerTextColor = textColor
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is EmojiItem.Header -> TYPE_HEADER
            is EmojiItem.Emoji -> TYPE_EMOJI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_emoji, parent, false)
            EmojiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is EmojiItem.Header) {
            val tv = holder.itemView as TextView
            tv.text = item.title
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            tv.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            tv.setTextColor(headerTextColor)
            tv.alpha = 0.6f
            tv.setPadding(dpToPx(tv, 16), dpToPx(tv, 20), 0, dpToPx(tv, 8))
        } else if (holder is EmojiViewHolder && item is EmojiItem.Emoji) {
            holder.emojiText.text = item.code

            holder.emojiText.textSize = if (item.isKaomoji) 14f else 26f
            holder.emojiText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            holder.emojiText.setTextColor(headerTextColor)

            if (item.isKaomoji) {
                holder.emojiText.maxLines = 1
                holder.emojiText.isSingleLine = true
            } else {
                holder.emojiText.maxLines = 1
                holder.emojiText.isSingleLine = false
            }
            holder.itemView.setOnClickListener { onEmojiClick(item.code) }
        }
    }

    private fun dpToPx(view: View, dp: Int): Int = 
        (dp * view.resources.displayMetrics.density).toInt()

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): EmojiItem? = if (position in items.indices) items[position] else null

    fun submitList(newItems: List<EmojiItem>) {
        val diffResult = DiffUtil.calculateDiff(EmojiDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    fun getCategoryIdAt(position: Int): Int? {
        if (position < 0 || position >= items.size) return null
        for (i in position downTo 0) {
            val item = items[i]
            if (item is EmojiItem.Header) return item.categoryId
        }
        return null
    }

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: TextView = itemView.findViewById(android.R.id.text1)
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class EmojiDiffCallback(
        private val oldList: List<EmojiItem>,
        private val newList: List<EmojiItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = 
            oldList[oldPos].id() == newList[newPos].id()
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = 
            oldList[oldPos] == newList[newPos]
    }
}