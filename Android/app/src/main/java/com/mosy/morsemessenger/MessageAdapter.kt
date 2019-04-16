package com.mosy.morsemessenger

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MessageAdapter(val messageList : ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message : Message = messageList[position]
        holder?.textTV?.text = message.text
        holder?.idTV?.text = message.id as String
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.bluetooth_list_item, parent, false)
        return ViewHolder (v)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTV = itemView.findViewById(R.id.textTV) as TextView
        val idTV = itemView.findViewById(R.id.idTV) as TextView
    }
}