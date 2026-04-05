package com.example.parallax

import android.content.Context
import android.database.Observable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty

// with help from
// https://dev.to/rdias002/how-to-create-an-expandable-item-in-android-recyclerview-1cja
class SettingData (
    val option: ConfigOption,
    value: Int = 0,
    onValueChanged: (ConfigOption, Int) -> Unit
) {
    var sb: SeekBar? = null

    // this is the fancy schmancy hi-tech wiring between the UI value and the data value!!
    var value: Int by Delegates.observable(value) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onValueChanged(option, newValue)
        }
    }

    // this might call onValueChanged (above), but shouldn't matter
    // is only ever called once, when loading new layer

    fun setSettingValue(amount: Int) {
        this.value = amount
        this.sb?.progress = amount
    }
}

class RecyclerViewImgEditAdapter internal constructor(
    context: Context?,
    private val mData: MutableList<SettingData>
) : RecyclerView.Adapter<RecyclerViewImgEditAdapter.ViewHolder?>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private var expandedItems: ArrayList<SettingData> = arrayListOf()


    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.rv_edit_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, getAdapterPosition())
        }

        fun bind(setting: SettingData) {
            setting.sb = sb

            tvName.text = setting.option.title
            tvVal.text = "${setting.value}"
            sb.progress = setting.value

            val isExpanded = expandedItems.contains(setting)
            if (isExpanded) {
                sb.visibility = View.VISIBLE
            } else {
                sb.visibility = View.GONE
            }

            tvName.setOnClickListener {
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
    fun getItem(id: Int): SettingData {
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
