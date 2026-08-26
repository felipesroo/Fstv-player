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
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos >= 0 && pos < categories.size) {
                    val old = selectedPosition
                    selectedPosition = pos
                    if (old >= 0 && old < categories.size) notifyItemChanged(old)
                    notifyItemChanged(pos)
                    onCategoryClick(categories[pos])
                }
            }

            view.setOnFocusChangeListener { v, hasFocus ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION || pos < 0 || pos >= categories.size) return@setOnFocusChangeListener
                if (hasFocus) {
                    v.scaleX = 1.03f
                    v.scaleY = 1.03f
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
        if (position < 0 || position >= categories.size) return
        val cat = categories[position]
        holder.tvIcon.text = cat.icon
        holder.tvName.text = cat.name
        holder.tvCount.text = "${cat.count}"

        val isSelected = position == selectedPosition
        holder.view.isSelected = isSelected

        holder.tvName.setTextColor(0xFFFFFFFF.toInt())
        holder.tvIcon.setTextColor(0xFFFFFFFF.toInt())
        holder.tvCount.setTextColor(if (isSelected) 0xFF818CF8.toInt() else 0xAAFFFFFF.toInt())
    }

    override fun getItemCount() = categories.size

    fun updateList(newList: List<CategoryItemInfo>) {
        categories = newList
        selectedPosition = 0
        notifyDataSetChanged()
    }
}
