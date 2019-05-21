package com.mosy.morsemessenger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.bluetooth_list_item.view.*

class DevicesAdapter(val devicesList : ArrayList<BluetoothDevice>, val clickListener: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devicesList[position], clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.bluetooth_list_item, parent, false)
        return ViewHolder (v)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind (device : BluetoothDevice, clickListener: (BluetoothDevice) -> Unit) {
            itemView.deviceNameTV.text = device.name
            itemView.deviceMacTV.text = device.address.toString()
            if (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                itemView.bluetoothImage.setImageResource(R.drawable.ic_bluetooth_connected_24dp)
            } else {
                itemView.bluetoothImage.setImageResource(R.drawable.ic_bluetooth_24dp)
            }
            itemView.setOnClickListener{clickListener (device)}
        }

    }
}