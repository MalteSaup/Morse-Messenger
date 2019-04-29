package com.mosy.morsemessenger

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {

    lateinit var messageList: ArrayList<Message>
    lateinit var messageAdapter: MessageAdapter
    private var myService: BluetoothConnectionService? = null
    private var isBound = false
    private var bluetoothService: BluetoothService? = null

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder
        ) {
            val binder = service as BluetoothConnectionService.MyLocalBinder
            myService = binder.getService()
            if(isServiceRunning(BluetoothConnectionService::class.java)){
                Log.d("TEST", "TRUE")
            }
            else Log.d("TEST", "FALSE")
            setBtService(myService?.getBt())
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    fun setBtService(bt: BluetoothService?) {
        bluetoothService = bt
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        bindService(Intent(applicationContext, BluetoothConnectionService::class.java), myConnection, Context.BIND_AUTO_CREATE)
        chatBox.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false) as RecyclerView.LayoutManager?
        messageList = ArrayList()
        messageAdapter = MessageAdapter(messageList)
        chatBox.adapter = messageAdapter


        //stopService(Intent(applicationContext, BluetoothConnectionService::class.java))

        //Click-Listener for send-Button
        sendButton.setOnClickListener{
            var text: String = textInput.text.toString()
            Log.d("TEST", this.bluetoothService?.getId() + " UFF12")
            if(isBound) Log.d("TEST", "TRUE BOUND")
            else Log.d("TEST", "FALSE BOUND")
            //TODO: String aus Bluetooth-Übertragung bekommen und in message umwandeln mit id 1
            //own id: 0, id of other chat-member: 1
            var message: Message = Message(0, text)
            //var message2: Message = Message(1, bluetoothService?.getId() as String)

            //Message aus text an Arduino senden
            bluetoothService?.write(text +"/n");

            //scrolls recyclerView to the bottom
            chatBox.smoothScrollToPosition(messageList.size);

            //Update RecyclerView
            messageList.add(message)
            //messageList.add(message2)
            messageAdapter?.notifyItemInserted(messageList.size - 1)

            //delete text in textInput
            textInput.setText("")

        }

        //TODO: ViewModel und App-interne Datenbank erstellen. Dort Chatverläufe speichern anhand von MAC-Adresse und Message.

    }


    fun sendTextToOtherDevice(text: String) {
        //stackoverflow.com/questions/5030150/passing-a-bluetooth-connection-to-a-new-activity
        // stackoverflow.com/questions/22573301/how-to-pass-a-handler-from-activity-to-service
        //stackoverflow.com/questions/17568470/holding-android-bluetooth-connection-through-multiple-activities
        //stackoverflow.com/questions/40308008/pass-the-bluetooth-connection-between-activities-in-android-studio
        //github.com/socketio/socket.io-client-java/issues/219


        //github.com/sunsided/android-bluetoothspp/blob/master/src/de/widemeadows/android/bluetoothspptest/BluetoothService.java
        // developer.android.com/guide/topics/connectivity/bluetooth
        //de.wikibooks.org/wiki/Googles_Android/_Bluetooth
    }

}



