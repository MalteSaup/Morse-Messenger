package com.mosy.morsemessenger

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_main.*

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

    }


    fun sendButtonListener(){
        sendButton.setOnClickListener{
            var text: String = textEingabe.text as String
            //TODO NACHRICHT SENDEN VIA BT UND HINZUFÜGEN ZU RECYCLER VIEW
            var message: Message = Message(2, "Hal")
            messageList.add(message)
            messageAdapter?.notifyItemInserted(messageList.size - 1)
        }
    }
}



