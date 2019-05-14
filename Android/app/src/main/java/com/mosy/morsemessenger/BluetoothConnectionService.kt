package com.mosy.morsemessenger

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.Toast
import java.io.UnsupportedEncodingException

//Hintergrund Service welcher die Bluetooth Verbindung zwischen den Activits überreicht
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
        return super.onStartCommand(intent, flags, startId)
    }

    fun setBt(bluetoothService: BluetoothService?){
        this.bluetoothService = bluetoothService
    }

    fun getBt(): BluetoothService? {
        return bluetoothService
    }
}

