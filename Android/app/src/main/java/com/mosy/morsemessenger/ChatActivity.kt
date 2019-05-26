package com.mosy.morsemessenger

import android.app.ActivityManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.content.*
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.util.Log.d
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*
import android.view.inputmethod.InputMethodManager


class ChatActivity : OptionsMenuActivity() {
    var messageList: ArrayList<Message> = ArrayList()
    var messageAdapter: MessageAdapter = MessageAdapter(messageList)
    lateinit var username: String
    lateinit var speed : String
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

        //TODO: nur am Anfang senden, wenn beide Chat-Partner mit ihren Arduinos verbunden sind
        //send username to chat-partner + speed to arduino
        bluetoothService?.write("USR:" + username + "/n")
        bluetoothService?.write("CLK:" + speed + "/n")
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
        speed = intent.getStringExtra("speed")
        Log.i("TEST", username + " " + speed)

        sendButton.setOnClickListener {
            var text: String = textInput.text.toString()
            if (isBound) d("TEST", "TRUE BOUND")
            else d("TEST", "FALSE BOUND")
            var message = Message(1, text)

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
            if(checkForNewMessage()) {
                for (i in bluetoothService?.textArray!!) {
                    receiveTextFromOtherDevice(i.trim())
                }
                bluetoothService?.textArray = ArrayList()
            }
            mHandler.postDelayed(this.mRunnable, 100)
        }
        mRunnable.run()

        var filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(mReceiver, filter)
    }

    val mReceiver = object : BroadcastReceiver(){ //Feuert Nachricht bei Verlust BT Verbindung, allerdings braucht es ziemlich lange. (>15 Sekunden)
        override fun onReceive(p0: Context?, p1: Intent?) {
            var action = p1?.action
            var device = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if(ACTION_ACL_DISCONNECTED.equals(action)) Log.d("BTSTATE", "HALLO !!!!!!")
            Log.d("BTSTATE", "CHECK")
        }

    }
    fun receiveTextFromOtherDevice(msg : String) {
        if(!msg.isBlank()){
            d("BTSTRING", msg)
            if(msg.length > 1){
                //checks if message is username of chat-partner
                if (msg.contains("USR:", ignoreCase = true)) {
                    //sets username of chat-partner
                    var nameString: String = msg.removePrefix("usr:")
                    nameDisplay.text = nameString
                    return
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




