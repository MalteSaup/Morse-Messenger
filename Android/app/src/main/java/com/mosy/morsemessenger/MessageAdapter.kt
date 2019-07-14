package com.mosy.morsemessenger

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.message_list_item.view.*

class MessageAdapter(private val messageList: ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    private var itemVisible: Boolean = false
    private var itemReceived: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val message: Message = messageList[position]
        val paramsLinear = holder.messageLinearLayout?.layoutParams as RelativeLayout.LayoutParams

        holder.textTV?.text = message.text
        holder.textTV?.setPadding(32, 24, 32, 24)

        //change background-image for messages of chat-members
        if (message.id == 0) {
            holder.textTV?.setBackgroundResource(R.drawable.messageleft)
            holder.textTV?.setTextColor(ContextCompat.getColor(holder.textTV.context, R.color.colorTextW))

            paramsLinear.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            paramsLinear.addRule(RelativeLayout.ALIGN_START)

            holder.messageRelativeLayout?.gravity = Gravity.START
        } else {
            holder.textTV?.setBackgroundResource(R.drawable.messageright)
            holder.textTV?.setTextColor(ContextCompat.getColor(holder.textTV.context, R.color.colorTextB))

            paramsLinear.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            paramsLinear.addRule(RelativeLayout.ALIGN_END)

            holder.messageRelativeLayout?.gravity = Gravity.END
        }

        //causes layout update
        holder.messageLinearLayout.layoutParams = paramsLinear

        //send + received Arrows
        if (itemVisible && !itemReceived) {
            holder.checkArrow?.visibility = VISIBLE
            holder.checkArrow?.setImageResource(R.drawable.ic_check_white_24dp)
        } else if (itemVisible && itemReceived) {
            holder.checkArrow?.visibility = VISIBLE
            holder.checkArrow?.setImageResource(R.drawable.ic_check_green_24dp)
        } else holder.checkArrow?.visibility = INVISIBLE

        itemVisible = false
        itemReceived = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.message_list_item, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    //get Index of last Message with Id 1 in RecyclerView
    private fun getIndex(): Int {

        var ownMessageList: ArrayList<Message> = ArrayList()
        var index = 0

        for (message in messageList) {
            if (message.id == 1) {
                ownMessageList.add(message)
            }
        }

        if (ownMessageList.isNotEmpty()) {
            index = (messageList).indexOf(ownMessageList.last())
        }
        return index
    }

    fun showSENTArrow() {
        val index = getIndex()

        itemVisible = true
        itemReceived = false
        notifyItemChanged(index)
    }

    fun showRECEIVEDArrow() {
        val index = getIndex()

        itemVisible = true
        itemReceived = true
        notifyItemChanged(index)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTV: TextView? = itemView.textTV
        val messageLinearLayout: LinearLayout? = itemView.messageLinearLayout
        val messageRelativeLayout: RelativeLayout? = itemView.messageRelativeLayout
        var checkArrow: ImageView? = itemView.checkArrow
    }
}