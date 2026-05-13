// 這邊盡量不做 UI 處理，把事件都放在 View 中
package com.wade.fsime.custom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.wade.fsime.R

class CustomAdapter(context: Context, @JvmField var dataSet: ArrayList<String>) :
    ArrayAdapter<String>(context, R.layout.input_table_item, dataSet) {
    fun addItem(item: String) {
        dataSet.add(item)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        dataSet.removeAt(position)
        notifyDataSetChanged()
    }

    fun clean() {
        val listIterator = dataSet.listIterator()
        while (listIterator.hasNext()) {
            listIterator.next()
            listIterator.remove()
        }
    }

    fun setItems(composes: ArrayList<String>) {
        clean()
        dataSet = composes
        notifyDataSetChanged()
    }

    fun setItem(pos: Int, item: String) {
        dataSet[pos] = item
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): String {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private class ViewHolder {
        var compose_item: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var currentItemView = convertView
        val viewHolder: ViewHolder // view lookup cache stored in tag
        if (currentItemView == null) {
            val inflater = LayoutInflater.from(context)
            currentItemView = inflater.inflate(R.layout.input_table_item, parent, false)
            viewHolder = ViewHolder()
            viewHolder.compose_item = currentItemView.findViewById(R.id.compose_item)
            currentItemView.tag = viewHolder
        } else {
            viewHolder = currentItemView.tag as ViewHolder
        }
        viewHolder.compose_item!!.text = getItem(position)
        return currentItemView!!
    }
}