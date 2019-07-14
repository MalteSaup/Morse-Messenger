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


const val REQUEST_ENABLE_BT = 1
const val TAG = "Bluetooth"
const val MESSAGE_READ: Int = 0
const val MESSAGE_CONNECTION: Int = 1


class BluetoothService(private val handler: Handler) {

    var textArray: ArrayList<String> = ArrayList()
    var inChat = false
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    val enabled: Boolean? get() = bluetoothAdapter?.isEnabled
    private var connectedThread: ConnectedThread? = null


    fun disable() {
        bluetoothAdapter?.disable()
    }

    //connect Device
    fun connect(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)

        if (device != null) {
            val connectThread = ConnectThread(device)
            connectThread.start()
        }

    }
    //disconnect Device
    fun disconnect(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)

        if (device != null) connectedThread?.cancel()
    }

    fun discover() {
        bluetoothAdapter?.startDiscovery()
    }

    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {

        var socket: BluetoothSocket? = null

        override fun run() {
            var fail = false

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()

                fail = true
            }

            // Establish the bluetooth socket connection.
            try {
                socket?.connect()
            } catch (e: IOException) {
                try {
                    fail = true
                    socket?.close()
                    handler.obtainMessage(MESSAGE_CONNECTION, -1, -1).sendToTarget()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }
            }

            if (!fail) {
                if (socket != null) connectedThread = ConnectedThread(socket!!)

                connectedThread?.start()
                handler.obtainMessage(MESSAGE_CONNECTION, 1, -1, device.name).sendToTarget()
            }

        }

        fun cancel() {
            try {
                if (socket != null) socket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "cancel of socket in ConnectThread failed. " + e.message)
            }
        }

    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        val mmInStream: InputStream = mmSocket.inputStream
        val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()
            val stringBuilder = StringBuilder()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {

                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                stringBuilder.append(mmBuffer.toString(Charsets.UTF_8).substring(0, numBytes))

                if (inChat && stringBuilder.isNotEmpty() && stringBuilder.toString().length > 1 && stringBuilder.endsWith(
                        "\n"
                    )
                ) {
                    textArray.add(stringBuilder.toString())
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
        val inputNoSpecialChars: String = checkSpecialCharacters(input)
        val sendString = inputNoSpecialChars.toUpperCase() + "\n"
        val bytes = sendString.toByteArray() //converts entered String into bytes

        try {
            connectedThread?.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //converts special characters, which morse code does not implement
    private fun checkSpecialCharacters(input: String): String {
        var input2 = input

        input2.toLowerCase()

        input2 = input2.replace("ü", "ue")
        input2 = input2.replace("ä", "ae")
        input2 = input2.replace("ö", "oe")

        input2 = input2.replace("Ü", "Ue")
        input2 = input2.replace("Ä", "Ae")
        input2 = input2.replace("Ö", "Oe")

        input2 = input2.replace("ß", "ss")

        if (input2[0] == 'u' && input2[0] == 's' && input2[0] == 'r' && input2[0] == ':') return input2

        var i = 0

        while (i < input2.length) {
            var character = input2[i].toInt()
            if (character != 32 && character != 44 && character != 46 && character != 63 && character !in 58..47 && character !in 97..122) {
                input2.removeRange(i, i)
            } else i++
        }

        return input2
    }
}

