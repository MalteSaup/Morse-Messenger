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


}



