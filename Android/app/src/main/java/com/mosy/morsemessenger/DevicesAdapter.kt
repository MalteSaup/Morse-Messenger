package com.mosy.morsemessenger


import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.bluetooth_list_item.view.*

class DevicesAdapter(private val devicesList: ArrayList<BluetoothDevice>, private val clickListener: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    var isConnected: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devicesList[position], clickListener)

        if (!isConnected) {
            holder.bluetoothImage.setImageResource(R.drawable.ic_bluetooth_24dp)
            Log.i("DEVICE", "not Connected 24dp")
        } else {
            holder.bluetoothImage.setImageResource(R.drawable.ic_bluetooth_connected_24dp)
            Log.i("DEVICE", "isConnected connected_24dp")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.bluetooth_list_item, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    fun changeIcon(device: BluetoothDevice, isConnectedOut: Boolean) {
        var index = devicesList.indexOf(device)
        isConnected = isConnectedOut
        notifyItemChanged(index)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var bluetoothImage = itemView.bluetoothImage

        fun bind(device: BluetoothDevice, clickListener: (BluetoothDevice) -> Unit) {
            itemView.deviceNameTV.text = device.name
            itemView.deviceMacTV.text = device.address.toString()
            itemView.setOnClickListener { clickListener(device) }
        }
    }
}