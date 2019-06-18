package com.mosy.morsemessenger

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
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
    var text: String = ""
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
        bluetoothService?.write("USR:" + username )
        Thread.sleep(500);
        bluetoothService?.write("CLK:" + speed )
        bluetoothService?.inChat = true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        unbindService(myConnection)
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
        username = deleteSpaceAtEnd(username)
        speed = intent.getStringExtra("speed")
        Log.i("TEST", username + "d " + speed)

        scrollToBottomWhenKeyboardOpen()

        sendButton.setOnClickListener {
            var textMessage: String = textInput.text.toString()
            if (isBound) d("TEST", "TRUE BOUND")
            else d("TEST", "FALSE BOUND")

            //if last chars are spaces: delete spaces
            text = deleteSpaceAtEnd(textMessage)
            Log.i("TEST", text +"1")

            var message = Message(1, text)

            //Message aus text an Arduino senden
            bluetoothService?.write(text );

            //Update RecyclerView
            messageList.add(message)
            messageAdapter.notifyItemInserted(messageList.size - 1)

            //scrolls recyclerView to the bottom
            chatBox.smoothScrollToPosition(messageList.size);

            //delete text in textInput
            textInput.setText("")
            closeKeyboard()
        }

        //Checks if messages exist and puts them into messenger
        mHandler = Handler()
        mRunnable = Runnable{
            if(checkForNewMessage()) {
                for (text in bluetoothService?.textArray!!) {
                    Log.i("Test",text + " Runnable text")
                    receiveTextFromOtherDevice(text.trim())
                }
                bluetoothService?.textArray = ArrayList()
            }
            if(bluetoothService != null){
                if(!bluetoothService?.enabled!!) bluetoothOff()
            }

            mHandler.postDelayed(this.mRunnable, 100)
        }
        mRunnable.run()

        var filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        this.registerReceiver(mReceiver, filter)

    }

    fun deleteSpaceAtEnd(textMessage: String) : String {
        val charArray : CharArray = textMessage.takeLast(1).toCharArray()
        val lastChar : Char = charArray[0]
        val isSpace : Boolean = Character.isWhitespace(lastChar)
        if (isSpace) {
            text = textMessage.dropLast(1)
            deleteSpaceAtEnd(text)
            return text
        }
        else { return textMessage }
    }

    fun scrollToBottomWhenKeyboardOpen () {
        chatBox.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                chatBox.postDelayed(Runnable {
                    chatBox.smoothScrollToPosition(messageList.size)
                }, 100)
            }
        }
    }

    val mReceiver = object : BroadcastReceiver(){ //Feuert Nachricht bei Verlust BT Verbindung, allerdings braucht es ziemlich lange. (>15 Sekunden)
        override fun onReceive(p0: Context?, p1: Intent?) {
            var action = p1?.action
            var device = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if(ACTION_ACL_DISCONNECTED.equals(action) || ACTION_ACL_DISCONNECT_REQUESTED.equals(action)){
                var intent = Intent()
                intent.putExtra("connectionLostState", 1)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            Log.d("BTSTATE", "CHECK")
        }

    }

    fun bluetoothOff(){
        var intent = Intent()
        intent.putExtra("connectionLostState", 1)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun receiveTextFromOtherDevice(msg : String) {
        if(!msg.isBlank()){
            d("BTSTRING", msg)
            if(msg.length > 1){
                //checks if message is username of chat-partner
                if (msg.contains("USR:", ignoreCase = true)) {
                    //sets username of chat-partner
                    var nameString: String = msg.removePrefix("USR:")
                    nameDisplay.text = nameString
                    return
                }
                if (msg.contentEquals("SENT:")){
                    //SENT: Message was sent successfully. White Arrow
                    messageAdapter.showSENTArrow()
                    return
                }
                if (msg.contentEquals("ACK:")){
                    //ACK: Message was received successfully. Green Arrow
                    messageAdapter.showRECEIVEDArrow()
                    return
                }
                messageList.add(Message(0, msg))
                messageAdapter.notifyItemInserted(messageList.size - 1)

                //scrolls recyclerView to the bottom
                chatBox.smoothScrollToPosition(messageList.size);
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




