package com.mosy.morsemessenger

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.Log.d
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.UnsupportedEncodingException

class ChatActivity : AppCompatActivity() {
    var messageList: ArrayList<Message> = ArrayList()
    var messageAdapter: MessageAdapter = MessageAdapter(messageList)
    lateinit var username: String
    private var myService: BluetoothConnectionService? = null
    private var isBound = false
    private var bluetoothService: BluetoothService? = null
    lateinit var mRunnable: Runnable;
    lateinit var mHandler: Handler;

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
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as BluetoothConnectionService.MyLocalBinder
            myService = binder.getService()
            if (isServiceRunning(BluetoothConnectionService::class.java)) {
                d("TEST", "TRUE")
            } else d("TEST", "FALSE")
            setBtService(myService?.getBt())
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    fun setBtService(bt: BluetoothService?) {
        bluetoothService = bt
        bluetoothService?.write(username + "/n")
        bluetoothService?.inChat = true

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        bindService(
            Intent(applicationContext, BluetoothConnectionService::class.java),
            myConnection,
            Context.BIND_AUTO_CREATE
        )

        chatBox.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        chatBox.adapter = messageAdapter
        val intent: Intent = getIntent()
        username = intent.getStringExtra("username")
        d("TEST", username)
        sendButton.setOnClickListener {
            var text: String = textInput.text.toString()
            if (isBound) d("TEST", "TRUE BOUND")
            else d("TEST", "FALSE BOUND")
            var message: Message = Message(1, text)

            //Message aus text an Arduino senden
            bluetoothService?.write(text + "/n");

            //scrolls recyclerView to the bottom
            chatBox.smoothScrollToPosition(messageList.size);

            //Update RecyclerView
            messageList.add(message)
            messageAdapter.notifyItemInserted(messageList.size - 1)

            //delete text in textInput
            textInput.setText("")
        }
        //Check ob Nachrichten da sind und in Messenger packen
        mHandler = Handler()
        mRunnable = Runnable{
            d("BTSTRING", "MSG")
            if(checkForNewMessage()){
                var removeArray = bluetoothService?.textArray!!
                for(i in bluetoothService?.textArray!!){
                    receiveTextFromOtherDevice(i)

                }
                bluetoothService?.textArray = ArrayList()
            }
            mHandler.postDelayed(this.mRunnable, 100)
        }
        mRunnable.run()
    }

    fun receiveUsernameFromOtherDevice(text: String) {
        //TODO: get first message of other device (which is the username) and display it in app header
        var usernameMessage: Message = Message(1, text)
        var username = usernameMessage.text
    }
    fun receiveTextFromOtherDevice(msg : String) {
        if(!msg.isBlank()){
            d("BTSTRING", msg)
            if(msg.length > 1){
                messageList.add(Message(-1, msg))
                messageAdapter.notifyItemInserted(messageList.size - 1)
            }
        }



    }

    fun checkForNewMessage(): Boolean{
        if(bluetoothService != null){
            if(bluetoothService?.textArray!!.size > 0){
                return true
            }
        }
        return false
    }


}




