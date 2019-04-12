package com.mosy.morsemessenger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

// https://github.com/bauerjj/Android-Simple-Bluetooth-Example/tree/master/app

const val REQUEST_ENABLE_BT = 1
const val TAG = "Bluetooth"
const val MESSAGE_READ: Int = 0
const val MESSAGE_CONNECTION: Int = 1
const val MESSAGE_STATUS: Int = 2

class BluetoothService (private val handler: Handler) {
    // https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
    // Hint: If you are connecting to a Bluetooth serial board then try using the
    // well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
    // However if you are connecting to an Android peer then please generate your own unique UUID.
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val enabled: Boolean?
        get() = bluetoothAdapter?.isEnabled

    fun disable(){
        if (bluetoothAdapter != null) bluetoothAdapter.disable()
    }

    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    fun connect(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device != null) {
            val connectThread = ConnectThread(device)
            connectThread.start()
        }
    }

    fun discover(){
        bluetoothAdapter?.startDiscovery()
    }

    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {
        override fun run(){
            var fail = false

            var socket: BluetoothSocket? = null

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                fail = true

                //Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show()
            }

            // Establish the Bluetooth socket connection.
            try {
                socket?.connect()
            } catch (e: IOException) {
                try {
                    fail = true
                    socket?.close()
                    handler.obtainMessage(MESSAGE_CONNECTION, -1, -1).sendToTarget()
                } catch (e2: IOException) {
                    //insert code to deal with this
                    // Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show()
                }

            }

            if (fail == false) {
                if (socket != null) connectedThread = ConnectedThread(socket)
                connectedThread?.start()
                handler.obtainMessage(MESSAGE_CONNECTION, 1, -1, device.name).sendToTarget()
            }

        }

    }
    private var connectedThread: ConnectedThread? = null

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()
            var stringBuilder = StringBuilder()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
                stringBuilder.append(mmBuffer.toString(Charsets.UTF_8).substring(0, numBytes))
                if (stringBuilder.endsWith("\r\n")){
                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        stringBuilder.toString())
                    readMsg.sendToTarget()
                    stringBuilder.clear()
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
            }
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
    fun write(input: String) {
        val bytes = input.toByteArray()           //converts entered String into bytes
        try {
            connectedThread?.write(bytes)
        } catch (e: IOException) {
        }
    }

}

