package com.mosy.morsemessenger

import android.app.Activity
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getColor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MessageAdapter(val messageList : ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message : Message = messageList[position]
        holder.textTV?.text = message.text
        holder.textTV?.setPadding(32,24,32,24)

        //change background-image for messages of other chat-member
        if (message.id == 1) {
            holder.textTV?.setBackgroundResource( R.drawable.messageleft)
            holder.textTV?.setTextColor(ContextCompat.getColor(holder.textTV.context , R.color.colorTextB))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.message_list_item, parent, false)
        return ViewHolder (v)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTV = itemView.findViewById(R.id.textTV) as? TextView
    }
}