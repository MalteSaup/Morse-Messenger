package com.mosy.morsemessenger

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.widget.Toast
import java.io.UnsupportedEncodingException


class BluetoothConnectionService : Service() {

    var bluetoothService: BluetoothService? = null
    private val mBinder = MyLocalBinder()

    inner class MyLocalBinder : Binder() {
        fun getService() : BluetoothConnectionService? {
            return this@BluetoothConnectionService
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(applicationContext, "Gestartet", Toast.LENGTH_SHORT).show()
        bluetoothService = BluetoothService(handler)
        return super.onStartCommand(intent, flags, startId)
    }
    val handler = object: Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                MESSAGE_READ -> messageRead(msg)
                MESSAGE_CONNECTION -> messageConnection(msg)
            }
        }
    }


    //TODO: Kommentar. was macht das?
    fun messageRead(msg: Message){
        try {
            //textViewMessage.text = msg.obj as String
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    //TODO: Kommentar. was macht das?
    fun messageConnection(msg: Message){
        if (msg.arg1 == 1) {
            Toast.makeText(applicationContext, "Verbindung aufgebaut: ${msg.obj as String}", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(applicationContext, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }






}

