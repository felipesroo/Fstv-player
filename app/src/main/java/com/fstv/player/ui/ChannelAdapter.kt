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
    private val onChannelClick: (ChannelItem) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivLogo: ImageView = view.findViewById(R.id.ivLogo)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    val old = selectedPosition
                    selectedPosition = pos
                    notifyItemChanged(old)
                    notifyItemChanged(selectedPosition)
                    onChannelClick(channels[pos])
                }
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
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
        holder.itemView.setBackgroundColor(
            if (isSelected) 0x336366F1 else 0x00000000
        )
        holder.tvName.setTextColor(
            if (isSelected) 0xFF6366F1.toInt() else 0xFFFFFFFF.toInt()
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
        selectedPosition = 0
        notifyDataSetChanged()
    }

    fun setSelected(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }
}
