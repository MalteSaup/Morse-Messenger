package com.mosy.morsemessenger

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class DevicesAdapter(val devicesList : ArrayList<BluetoothDevice>) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device : BluetoothDevice = devicesList[position]
        holder?.nameTV?.text = device.name
        holder?.macTV?.text = device.address
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.bluetooth_list_item, parent, false)
        return ViewHolder (v)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTV = itemView.findViewById(R.id.deviceNameTV) as TextView
        val macTV = itemView.findViewById(R.id.deviceMacTV) as TextView
    }
}