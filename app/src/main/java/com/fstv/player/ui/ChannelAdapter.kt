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
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    val old = selectedPosition
                    selectedPosition = pos
                    notifyItemChanged(old)
                    notifyItemChanged(pos)
                    onChannelClick(channels[pos], pos)
                }
            }

            view.setOnFocusChangeListener { v, hasFocus ->
                val pos = adapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return@setOnFocusChangeListener
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
        val channel = channels[position]
        holder.tvName.text = channel.name

        val isSelected = position == selectedPosition
        holder.view.isSelected = isSelected

        // Texto em branco puro para leitura perfeita
        holder.tvName.setTextColor(
            if (isSelected) 0xFF818CF8.toInt() else 0xFFFFFFFF.toInt()
        )

        if (!channel.logoUrl.isNullOrEmpty()) {
            Glide.with(holder.ivLogo.context)
                .load(channel.logoUrl)
                .placeholder(android.R.drawable.ic_menu_slideshow)
                .into(holder.ivLogo)
        } else {
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
