package com.mosy.morsemessenger

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

class MessageAdapter(val messageList : ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    var itemVisible: Boolean = false
    var itemReceived: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message : Message = messageList[position]
        val paramsLinear = holder.messageLinearLayout?.layoutParams as RelativeLayout.LayoutParams
        holder.textTV?.text = message.text
        holder.textTV?.setPadding(32,24,32,24)

        //change background-image for messages of chat-members
        if (message.id == 0) {
            holder.textTV?.setBackgroundResource( R.drawable.messageleft)
            holder.textTV?.setTextColor(ContextCompat.getColor(holder.textTV.context , R.color.colorTextW))
            paramsLinear.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            paramsLinear.addRule(RelativeLayout.ALIGN_START)
            holder.messageRelativeLayout?.gravity = Gravity.START
        } else {
            holder.textTV?.setBackgroundResource( R.drawable.messageright)
            holder.textTV?.setTextColor(ContextCompat.getColor(holder.textTV.context , R.color.colorTextB))
            paramsLinear.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            paramsLinear.addRule(RelativeLayout.ALIGN_END)
            holder.messageRelativeLayout?.gravity = Gravity.END
        }
        holder.messageLinearLayout.layoutParams = paramsLinear //causes layout update


        if(itemVisible && !itemReceived) {
            holder.checkArrow?.visibility = VISIBLE
        }
        if(itemVisible && itemReceived){
            holder.checkArrow?.visibility = VISIBLE
            holder.checkArrow?.setImageResource(R.drawable.ic_check_green_24dp)
        }
        itemVisible = false
        itemReceived = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.message_list_item, parent, false)
        return ViewHolder (v)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    //get Index of last Message with Id 1 in RecyclerView
    private fun getIndex(): Int {
        var ownMessageList = (messageList.filter { message -> message.id == 1})
        var index = (messageList).indexOf(ownMessageList.last())
        Log.i("MessageList", (messageList).indexOf(ownMessageList.last()).toString())
        return index
    }

    fun showSENTArrow(){
        var index = getIndex()
        itemVisible = true
        itemReceived = false
        Log.i("MessageList", "SENT")
        notifyItemChanged(index)
    }

    fun showRECEIVEDArrow () {
        var index = getIndex()
        itemVisible = true
        itemReceived = true
        Log.i("MessageList", "ACK")
        notifyItemChanged(index)
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTV = itemView.findViewById(R.id.textTV) as? TextView
        val messageLinearLayout = itemView.findViewById(R.id.messageLinearLayout) as? LinearLayout
        val messageRelativeLayout = itemView.findViewById(R.id.messageRelativeLayout) as? RelativeLayout
        var checkArrow = itemView.findViewById(R.id.checkArrow) as? ImageView
    }
}