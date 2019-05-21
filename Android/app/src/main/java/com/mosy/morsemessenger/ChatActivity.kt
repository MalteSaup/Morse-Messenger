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
import android.app.Activity
import android.view.inputmethod.InputMethodManager


class ChatActivity : AppCompatActivity() {
    var messageList: ArrayList<Message> = ArrayList()
    var messageAdapter: MessageAdapter = MessageAdapter(messageList)
    lateinit var username: String
    private var myService: BluetoothConnectionService? = null
    private var isBound = false
    private var bluetoothService: BluetoothService? = null
    lateinit var mRunnable: Runnable
    lateinit var mHandler: Handler


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
        Log.i("TEST", username)

        //TODO: nur am Anfang senden, wenn beide Chat-Partner mit ihren Arduinos verbunden sind
        //send username to chat-partner
        bluetoothService?.write("ThisIsTheName3795568" + username + "/n")

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
            closeKeyboard()
        }

        //Checks if messages exist and puts them into messenger
        mHandler = Handler()
        mRunnable = Runnable{
            d("BTSTRING", "MSG")
            if(checkForNewMessage()){
                for(i in bluetoothService?.textArray!!){
                    receiveTextFromOtherDevice(i)
                }
                bluetoothService?.textArray = ArrayList()
            }
            mHandler.postDelayed(this.mRunnable, 100)
        }
        mRunnable.run()
    }

    fun receiveTextFromOtherDevice(msg : String) {
        if(!msg.isBlank()){
            d("BTSTRING", msg)
            if(msg.length > 1){
                //checks if message is username of chat-partner
                if (msg.contains("ThisIsTheName3795568", ignoreCase = true)) {
                    //sets username of chat-partner
                    val nameString: String = msg.removePrefix("ThisIsTheName3795568")
                    nameDisplay.text = nameString
                }
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

    //To close keyboard of EditTexts
    fun closeKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view
        val view = currentFocus
        if (view != null) {
            //Grab the correct window token from view.
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }


}




