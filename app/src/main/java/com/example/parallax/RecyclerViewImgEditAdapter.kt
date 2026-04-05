package com.example.parallax

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates

// with help from
// https://dev.to/rdias002/how-to-create-an-expandable-item-in-android-recyclerview-1cja
class ConfigOptionDataUI (
    val option: ConfigOption,
    value: Int = 0,
    private val onValueChanged: (ConfigOption, Int) -> Unit,
    private val recyclerAdapter: RecyclerViewImgEditAdapter
) {
    var position = -1

    // this is the fancy schmancy hi-tech wiring between the UI value and the data value!!
    // mainActivity value is changed by the change in seekbar value
    var value: Int by Delegates.observable(value) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onValueChanged(option, newValue)
        }
    }

    // to be called externally- when loading new layer
    fun setSettingValue(amount: Int) {
        Log.i("__walpMain", "layer: $option at position: $position")

        val isValueChanged = value != amount
        value = amount

        // update UI
        if (isValueChanged) {
            recyclerAdapter.notifyItemChanged(position)
            Log.i("__walpMain", "change layer $option, value: $value")
        }
    }
}

class RecyclerViewImgEditAdapter internal constructor(
    context: Context?,
    private val mData: MutableList<ConfigOptionDataUI>
) : RecyclerView.Adapter<RecyclerViewImgEditAdapter.ViewHolder?>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private var expandedItems: ArrayList<ConfigOptionDataUI> = arrayListOf()


    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.rv_edit_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        mData[position].position = position
        holder.bind(mData[position])
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }


    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var tvName: TextView = itemView.findViewById(R.id.tvSettingLabel)
        var tvVal: TextView = itemView.findViewById(R.id.tvSettingValue)
        var sb: SeekBar = itemView.findViewById(R.id.sbSetting)
        var titleBar: LinearLayout = itemView.findViewById(R.id.llSettingTitleBar)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, getAdapterPosition())
        }

        fun bind(setting: ConfigOptionDataUI) {
            tvName.text = setting.option.title
            tvVal.text = "${setting.value}"
            sb.progress = setting.value

            val isExpanded = expandedItems.contains(setting)
            if (isExpanded) {
                sb.visibility = View.VISIBLE
            } else {
                sb.visibility = View.GONE
            }

            titleBar.setOnClickListener {
                if (expandedItems.contains(setting)) {
                    expandedItems.remove(setting)
                } else {
                    expandedItems.add(setting)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            sb.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekbar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    tvVal.text = "$progress"
                }

                override fun onStartTrackingTouch(p0: SeekBar) {}

                override fun onStopTrackingTouch(p0: SeekBar) {
                    setting.value = p0.progress
                }

            })
        }

    }

    // convenience method for getting data at click position
    fun getItem(id: Int): ConfigOptionDataUI {
        return mData[id]
    }
//    fun getItemValue(id: Int):

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        this.mClickListener = itemClickListener
    }

    fun setBitmapSettings() {

    }

    fun setInteractiveGifSettings() {

    }

    fun setGifSettings() {

    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }
}
