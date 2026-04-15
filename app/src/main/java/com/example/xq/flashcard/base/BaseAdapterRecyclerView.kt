package com.example.xq.flashcard.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.xq.flashcard.utils.ex.clickSafe

abstract class BaseAdapterRecyclerView<T, VB : ViewBinding>
@JvmOverloads constructor(dataList: MutableList<T>? = null) :
    RecyclerView.Adapter<BaseViewHolder<VB>>() {

    private var binding: VB? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        binding = inflateBinding(LayoutInflater.from(parent.context), parent)
        return BaseViewHolder(requireNotNull(binding)).apply {
            bindViewClick(this, viewType)
        }
    }

    fun setOnClickItem(listener: ((item: T?, position: Int) -> Unit)? = null) {
        setOnClickItem = listener
    }

    private var setOnClickItem: ((item: T?, position: Int) -> Unit)? = null
    private var setOnLongClickItem: ((view: View, item: T?, position: Int) -> Unit)? = null
    open fun bindViewClick(viewHolder: BaseViewHolder<VB>, viewType: Int) {
        viewHolder.itemView.clickSafe {
            val position = viewHolder.adapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return@clickSafe
            }
            setOnClickItem?.invoke(dataList.getOrNull(position), position)
        }

        viewHolder.itemView.setOnLongClickListener {
            val position = viewHolder.adapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return@setOnLongClickListener true
            }
            setOnLongClickItem?.invoke(it, dataList.getOrNull(position), position)
            true
        }
    }

    var dataList: MutableList<T> = dataList ?: arrayListOf()
        internal set

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        bindData(holder.binding, dataList[position], position)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<VB>,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        bindData(holder.binding, dataList[position], position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, parent: ViewGroup): VB

    protected abstract fun bindData(binding: VB, item: T, position: Int)

    open fun setData(@IntRange(from = 0) index: Int, data: T) {
        if (index >= this.dataList.size) {
            return
        }
        this.dataList[index] = data
        notifyItemChanged(index)
    }

    open fun removeAt(@IntRange(from = 0) position: Int) {
        if (position >= dataList.size) {
            return
        }
        this.dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    open fun remove(data: T) {
        val index = this.dataList.indexOf(data)
        if (index == -1) {
            return
        }
        removeAt(index)
    }

    @SuppressLint("NotifyDataSetChanged")
    open fun clearData() {
        dataList.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    open fun setDataList(data: Collection<T>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    open fun setDataListNew(data: Collection<T>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    open fun setDataListWithAction(data: Collection<T>, action: () -> Unit) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
        action()
    }

    open fun addDataList(data: Collection<T>) {
        dataList.addAll(data)
        notifyItemRangeInserted(dataList.size, data.size)
    }

}
