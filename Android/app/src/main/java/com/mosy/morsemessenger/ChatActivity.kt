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
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.UnsupportedEncodingException

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

    //Read message from other device
    //TODO: handler aus MainActivity nutzen und messageRead ausführen !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    fun messageRead(msg: android.os.Message){
        try {
            receiveTextFromOtherDevice(msg.obj as String) //msg.obj ist der mit String Builder erstellte String aus BluetoothService InputStream
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        bindService(Intent(applicationContext, BluetoothConnectionService::class.java), myConnection, Context.BIND_AUTO_CREATE)

        chatBox.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        //set MessageAdapter
        messageList = ArrayList()
        messageAdapter = MessageAdapter(messageList)
        chatBox.adapter = messageAdapter

        //get Username from Intent
        val intent: Intent = getIntent()
        val username: String = intent.getStringExtra("username")

        //Send Username to other device
        bluetoothService?.write(username +"/n");

        //stopService(Intent(applicationContext, BluetoothConnectionService::class.java))

        //Click-Listener for send-Button
        sendButton.setOnClickListener{
            var text: String = textInput.text.toString()
            Log.d("TEST", this.bluetoothService?.getId() + " UFF12")
            if(isBound) Log.d("TEST", "TRUE BOUND")
            else Log.d("TEST", "FALSE BOUND")
            //own id: 0, id of other chat-member: 1
            var message: Message = Message(0, text)
            //var message2: Message = Message(1, bluetoothService?.getId() as String)

            //Message aus text an Arduino senden
            bluetoothService?.write(text +"/n");

            //scrolls recyclerView to the bottom
            chatBox.smoothScrollToPosition(messageList.size);

            //Update RecyclerView
            messageList.add(message)
            messageAdapter.notifyItemInserted(messageList.size - 1)

            //delete text in textInput
            textInput.setText("")
        }
    }

    fun receiveUsernameFromOtherDevice(text: String) {
        //TODO: get first message of other device (which is the username) and display it in app header
        var usernameMessage: Message = Message(1, text)
        var username = usernameMessage.text
    }


    fun receiveTextFromOtherDevice(text: String) {
        var message: Message = Message(1, text)
        messageList.add(message)
        messageAdapter.notifyItemInserted(messageList.size - 1)

    }

}



