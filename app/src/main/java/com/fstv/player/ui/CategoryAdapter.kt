package com.fstv.player.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fstv.player.R

data class CategoryItemInfo(
    val name: String,
    val count: Int,
    val icon: String
)

class CategoryAdapter(
    private var categories: List<CategoryItemInfo>,
    private val onCategoryClick: (CategoryItemInfo) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    var selectedPosition = 0

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCount: TextView = view.findViewById(R.id.tvCategoryCount)

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
                    onCategoryClick(categories[pos])
                }
            }

            view.setOnFocusChangeListener { v, hasFocus ->
                val pos = adapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return@setOnFocusChangeListener
                if (hasFocus) {
                    v.scaleX = 1.04f
                    v.scaleY = 1.04f
                    v.elevation = 6f
                } else {
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    v.elevation = 0f
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_sidebar, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cat = categories[position]
        holder.tvIcon.text = cat.icon
        holder.tvName.text = cat.name
        holder.tvCount.text = "${cat.count}"

        val isSelected = position == selectedPosition
        holder.view.isSelected = isSelected
        holder.tvName.setTextColor(
            if (isSelected) 0xFF6366F1.toInt() else 0xFFFFFFFF.toInt()
        )
    }

    override fun getItemCount() = categories.size

    fun updateList(newList: List<CategoryItemInfo>) {
        categories = newList
        selectedPosition = 0
        notifyDataSetChanged()
    }
}
