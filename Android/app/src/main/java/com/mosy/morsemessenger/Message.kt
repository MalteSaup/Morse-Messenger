package com.mosy.morsemessenger

class Message(id: Number, text: String) {

    lateinit var id: Number
    lateinit var text: String

    init{
        this.id = id
        this.text = text
    }

}