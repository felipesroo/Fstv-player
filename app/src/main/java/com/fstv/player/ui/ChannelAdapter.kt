package com.fstv.player.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fstv.player.R
import com.fstv.player.utils.ChannelItem

class ChannelAdapter(
    private var channels: List<ChannelItem>,
    private val onChannelClick: (ChannelItem, Int) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    var selectedPosition = -1

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivLogo: ImageView = view.findViewById(R.id.ivLogo)

        init {
            view.isFocusable = true
            view.isFocusableInTouchMode = false

            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos >= 0 && pos < channels.size) {
                    val old = selectedPosition
                    selectedPosition = pos
                    if (old >= 0 && old < channels.size) notifyItemChanged(old)
                    notifyItemChanged(pos)
                    onChannelClick(channels[pos], pos)
                }
            }

            view.setOnFocusChangeListener { v, hasFocus ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION || pos < 0 || pos >= channels.size) return@setOnFocusChangeListener
                if (hasFocus) {
                    v.scaleX = 1.04f
                    v.scaleY = 1.04f
                    v.elevation = 8f
                } else {
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    v.elevation = 0f
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= channels.size) return
        val channel = channels[position]
        holder.tvName.text = channel.name

        val isSelected = position == selectedPosition
        holder.view.isSelected = isSelected

        holder.tvName.setTextColor(
            if (isSelected) 0xFF818CF8.toInt() else 0xFFFFFFFF.toInt()
        )

        try {
            if (!channel.logoUrl.isNullOrEmpty()) {
                Glide.with(holder.ivLogo.context)
                    .load(channel.logoUrl)
                    .placeholder(android.R.drawable.ic_menu_slideshow)
                    .error(android.R.drawable.ic_menu_slideshow)
                    .into(holder.ivLogo)
            } else {
                holder.ivLogo.setImageResource(android.R.drawable.ic_menu_slideshow)
            }
        } catch (e: Exception) {
            holder.ivLogo.setImageResource(android.R.drawable.ic_menu_slideshow)
        }
    }

    override fun getItemCount() = channels.size

    fun updateList(newList: List<ChannelItem>) {
        channels = newList
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
