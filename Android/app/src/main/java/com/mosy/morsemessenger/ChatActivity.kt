package com.mosy.morsemessenger

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {

    lateinit var messageList: ArrayList<Message>
    lateinit var messageAdapter: MessageAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatBox.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(messageList)
        chatBox.adapter = messageAdapter

        //Click-Listener for send-Button
        sendButton.setOnClickListener{
            var text: String = textInput.text.toString()

            //TODO: String aus Bluetooth-Übertragung bekommen und in message umwandeln mit id 1
            //own id: 0, id of other chat-member: 1
            var message: Message = Message(0, text)
            var message2: Message = Message(1, "HUHUUUUUUUUUUUUUUUUUUUUUUUUUU")

            //TODO: Message aus text an Arduino senden
            sendTextToOtherDevice(text)

            //scrolls recyclerView to the bottom
            chatBox.smoothScrollToPosition(messageList.size);

            //Update RecyclerView
            messageList.add(message)
            messageList.add(message2)
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



