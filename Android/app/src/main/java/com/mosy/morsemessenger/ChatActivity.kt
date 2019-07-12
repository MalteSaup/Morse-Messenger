package com.mosy.morsemessenger

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar


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

            // If the service is running then return true
            if (serviceClass.name == service.service.className) {
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
            setBtService(myService?.getBt())

            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    fun setBtService(bt: BluetoothService?) {
        bluetoothService = bt
        
        //send username to chat-partner + speed to arduino
        bluetoothService?.write("USR:" + username )
        Thread.sleep(500)

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

        val intent: Intent = intent
        username = intent.getStringExtra("username")
        username = deleteSpaceAtEnd(username)

        scrollToBottomWhenKeyboardOpen()
        initializeSeekBar()
        initializeSendButton()
        initializeRepeatThatButton()

        //Checks if messages exist and puts them into messenger
        mHandler = Handler()
        mRunnable = Runnable{
            if(checkForNewMessage()) {
                for (text in bluetoothService?.textArray!!) {
                    receiveTextFromOtherDevice(text.trim())
                }
                bluetoothService?.textArray = ArrayList()
            }
            if(bluetoothService != null && !bluetoothService?.enabled!!) bluetoothOff()

            mHandler.postDelayed(this.mRunnable, 100)
        }
        mRunnable.run()

        val filter = IntentFilter()
        filter.addAction(ACTION_ACL_DISCONNECTED)
        filter.addAction(ACTION_ACL_DISCONNECT_REQUESTED)
        this.registerReceiver(mReceiver, filter)

    }

    private fun deleteSpaceAtEnd(textMessage: String) : String {

        val charArray : CharArray = textMessage.takeLast(1).toCharArray()
        val lastChar : Char = charArray[0]
        val isSpace : Boolean = Character.isWhitespace(lastChar)

        return if (isSpace) {
            text = textMessage.dropLast(1)
            deleteSpaceAtEnd(text)
            text
        } else {
            textMessage
        }
    }

    private fun initializeSeekBar() {

        sendSpeedSBChat.max = 200
        sendSpeedTVChat.text = sendSpeedSBChat.progress.toString() + " ms"

        sendSpeedSBChat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //minimum = 10; (Because api-level <26 sendSpeedSB.min does not work)
                if (progress <10) {
                    sendSpeedSBChat.post(Runnable { sendSpeedSBChat.progress = 10 })
                }
                sendSpeedTVChat.text = progress.toString() + " ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                speed = sendSpeedSBChat.progress.toString()
                Log.i("TEST", "onStoptrackingTouch  ${speed}")
                bluetoothService?.write("CLK: ${speed}")
            }
        })
    }

    private fun initializeSendButton(){
        sendButton.setOnClickListener {

            if (textInput.text.toString().trim().isNotEmpty()) {
                var inputString = textInput.text.toString().trim()
                //filters special characters except .,? öüä
                var re = Regex("[^A-Za-z0-9. ,?öüäß]")
                inputString = re.replace(inputString,"")

                if(inputString.isNotEmpty()){
                    val textMessage: String = inputString

                    //if last chars are spaces: delete spaces
                    text = deleteSpaceAtEnd(textMessage)

                    val message = Message(1, text)

                    //Message aus text an Arduino senden
                    bluetoothService?.write(text)

                    //Update RecyclerView
                    messageList.add(message)
                    messageAdapter.notifyItemInserted(messageList.size - 1)

                    //scrolls recyclerView to the bottom
                    chatBox.smoothScrollToPosition(messageList.size)


                }

            }

            //delete text in textInput
            textInput.setText("")
            closeKeyboard()
        }
    }

    private fun initializeRepeatThatButton() {
        repeatThatBtn.setOnClickListener {
            bluetoothService?.write("::")
        }
    }

    private fun scrollToBottomWhenKeyboardOpen () {
        chatBox.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                chatBox.postDelayed({
                    chatBox.smoothScrollToPosition(messageList.size)
                }, 100)
            }
        }
    }

    // Sends message if bluetooth-connection is lost
    val mReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = p1?.action
            var device = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if(ACTION_ACL_DISCONNECTED.equals(action) || ACTION_ACL_DISCONNECT_REQUESTED.equals(action)){
                val intent = Intent()
                intent.putExtra("connectionLostState", 1)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun bluetoothOff(){
        val intent = Intent()
        intent.putExtra("connectionLostState", 1)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun receiveTextFromOtherDevice(msg : String) {

        var msg2 = msg

        if(!msg2.isBlank()){

            if(msg2.length > 1){
                Log.i("Message1", msg2.toString() +" start")


                if (msg2.contentEquals("SENT:") ){

                    //SENT: Message was sent successfully. White arrow
                    if(messageList.isNotEmpty()) {
                        messageAdapter.showSENTArrow()
                    }
                    return
                }
                if (msg2.contentEquals("ACK:")){

                    //ACK: Message was received successfully. Green arrow
                    if(messageList.isNotEmpty()) {
                        messageAdapter.showRECEIVEDArrow()
                    }
                    return
                }
                if(msg2.contains("SENT:") || msg2.contains("ACK:")){
                    Log.i("Message1", msg + " before removeACK")
                    msg2 = msg2.replace("SENT:", "")
                    msg2 = msg2.replace("ACK:", "")
                    msg2 = msg2.replace("\n", "")
                    Log.i("Message1", msg2 + " after removeACK")
                    if(msg2.trim().isEmpty()) return
                }

                //checks if message is username of chat-partner
                if (msg2.contains("USR:", ignoreCase = true)) {

                    //sets username of chat-partner
                    var nameString: String = msg2.removePrefix("USR:")
                    nameDisplay.text = nameString
                    return
                }

                Log.i("Message1", msg2.toString() +" ende")

                messageList.add(Message(0, msg2))
                messageAdapter.notifyItemInserted(messageList.size - 1)

                //scrolls recyclerView to the bottom
                chatBox.smoothScrollToPosition(messageList.size)
            }
        }
    }

    private fun checkForNewMessage(): Boolean{
        if(bluetoothService != null){
            if(bluetoothService?.textArray!!.size > 0){
                return true
            }
        }
        return false
    }

    //To close keyboard of EditTexts
    private fun closeKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view
        val view = currentFocus
        if (view != null) {
            //Grab the correct window token from view.
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}





