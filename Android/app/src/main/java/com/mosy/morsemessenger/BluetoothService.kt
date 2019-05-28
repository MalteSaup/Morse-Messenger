package com.mosy.morsemessenger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import android.widget.Toast
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
    var textArray: ArrayList<String> = ArrayList()
    var inChat = false
    var message = Message(1,"")
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    init{}
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

    fun disconnect(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device != null) {
            Log.i(TAG, device.toString() + " disconnect 1")
            connectedThread?.cancel()
        }
    }

    fun discover(){
        bluetoothAdapter?.startDiscovery()
    }

    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {
        var socket: BluetoothSocket? = null

        override fun run(){
            var fail = false

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
                }
            }

            if (fail == false) {
                if (socket != null) connectedThread = ConnectedThread(socket!!)
                connectedThread?.start()
                handler.obtainMessage(MESSAGE_CONNECTION, 1, -1, device.name).sendToTarget()
            }

        }

        fun cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.")
                if (socket != null) socket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "cancel of socket in ConnectThread failed. " + e.message)
            }

        }

    }
    private var connectedThread: ConnectedThread? = null

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        val mmInStream: InputStream = mmSocket.inputStream
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
                    Log.i("LEERZEICHEN0", "Input" + mmBuffer.toString())
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
                stringBuilder.append(mmBuffer.toString(Charsets.UTF_8).substring(0, numBytes))
                Log.i("LEERZEICHEN01", "Input" + stringBuilder.toString())
                if(inChat){
                   if(stringBuilder.isNotEmpty() && stringBuilder.toString().length > 1  /*&& stringBuilder.endsWith("/r")*/){
                       textArray.add(stringBuilder.toString())
                       Log.i("LEERZEICHEN", stringBuilder.toString())
                       textArray[textArray.size-1].replace("/n", "")
                       Log.i("LEERZEICHEN2", textArray[textArray.size-1])
                       stringBuilder.clear()

                   }
                   else Log.d("BTSTRING", "STRINGBUILDER")
                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
                //SENT: Message was sent. Check in MessageWindo
                //mmOutStream.write("SENT".toByteArray())
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
        var inputNoSpecialChars = checkSpecialCharacters(input)
        var x = inputNoSpecialChars.toUpperCase() + "/n"
        val bytes = x.toByteArray()           //converts entered String into bytes
        try {
            connectedThread?.write(bytes)
        } catch (e: IOException) {
        }
    }

    fun checkSpecialCharacters (input: String): String {
        var input2 = input

        input2 = input2.replace("ü", "ue")
        input2= input2.replace("ä", "ae")
        input2= input2.replace("ö", "oe")

        input2 = input2.replace("Ü", "Ue")
        input2= input2.replace("Ä", "Ae")
        input2= input2.replace("Ö", "Oe")

        input2= input2.replace("ß", "ss")
        return input2

    }
}

