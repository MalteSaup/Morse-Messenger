package com.mosy.morsemessenger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException

class MainActivity : AppCompatActivity() {

    private lateinit var devicesAdapter: DevicesAdapter
    private var bluetoothService: BluetoothService? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var devicesList : ArrayList<BluetoothDevice>
    private lateinit var btSwitch: Switch;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //create LayoutManager
        devicesRV.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        devicesList = ArrayList()
        devicesAdapter = DevicesAdapter(devicesList)
        devicesRV.adapter = devicesAdapter

        implementSwitchOnClickListener()
        initializeBluetoothService()
    }

    fun initializeBluetoothService(){
        val handler = object: Handler() {
            override fun handleMessage(msg: Message) {
                when(msg.what){
                    MESSAGE_READ -> messageRead(msg)
                    MESSAGE_CONNECTION -> messageConnection(msg)
                }
            }
        }
        bluetoothService = BluetoothService(handler)

        //Check if Bluetooth is already enabled on device.
        if (bluetoothService?.enabled == true) {
            onOffTV.text = getString(R.string.switchStatusOn)
            btSwitch.isChecked = true
        }
    }

    //Listener for Switch-State-Change. To enable and disable bluetooth on device.
    fun implementSwitchOnClickListener(){
        btSwitch = findViewById(R.id.bluetoothSwitch);
        btSwitch.setOnClickListener{
            if(btSwitch.isChecked){
                bluetoothOn()
            }
            else{
                bluetoothOff()
            }
        }
    }

    //Turn Bluetooth On
    fun bluetoothOn(){
        if (bluetoothService?.enabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        onOffTV.text = getString(R.string.switchStatusOn)
    }

    //Turn Bluetooth Off
    fun bluetoothOff(){
        bluetoothService?.disable()
        devicesList.clear()
        devicesAdapter?.notifyDataSetChanged()
        onOffTV.text = getString(R.string.switchStatusOff)
    }

    //Shows Bluetooth-Devices in List
    fun showPairedDevices(view: View){
        if (bluetoothService?.enabled!!) {
            for (device in bluetoothService?.pairedDevices!!)
                devicesList.add(device)
            // add the name to the list
            devicesAdapter?.notifyItemInserted(devicesList.size -1)

            Toast.makeText(applicationContext, "Show Paired Devices", Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
    }

    //Find Bluetooth-Devices and show them in List
    fun discoverPairedDevices(view: View){
        if (bluetoothService?.enabled!!) {
            bluetoothService?.discover()
            Toast.makeText(baseContext, "Discovering Paired Devices", Toast.LENGTH_SHORT).show()
            devicesList.clear()
            devicesAdapter?.notifyDataSetChanged()
            registerReceiver(blReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } else
            Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
    }

    //TODO: Kommentar. was macht das?
    private val blReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                devicesList.add(device)
                // add the name to the list
                devicesAdapter?.notifyItemInserted(devicesList.size -1)
            }
        }
    }

    //TODO: Kommentar. was macht das?
    fun messageRead(msg: Message){
        try {
            //textViewMessage.text = msg.obj as String
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    //TODO: Kommentar. was macht das?
    fun messageConnection(msg: Message){
        if (msg.arg1 == 1) {
            Toast.makeText(applicationContext, "Connected to Device: ${msg.obj as String}", Toast.LENGTH_SHORT)
        }
        else {
            Toast.makeText(applicationContext, "Connection Failed", Toast.LENGTH_SHORT)
        }
    }

    //Click-Listener for Device in Devices-RecyclerView. To create connection with Bluetooth-Device.
    private val deviceClickListener = object: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            Toast.makeText(applicationContext, "Connecting", Toast.LENGTH_SHORT).show()

            val info = (view as TextView).text
            val macAddress = info.substring(info.length - 17)
            bluetoothService?.connect(macAddress)
        }
    }

    //Click-Listener for toChat-Button. Starts the ChatActivity.
    fun implementChatBtnClickListener(view: View) {

        //Can only open new Activity, when nameET is filled and bluetooth-device is connected
        //TODO !nameET.text.equals("") funktioniert irgendwie nicht
        if (!nameET.text.equals("") && bluetoothService?.enabled!! /*TODO: && Verbindung zu Gerät ist hergestellt)*/) {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }


    }
    //TODO: ItemClickListener auf RecyclerView implementieren
    /*fun onClick (view: View) {
        val itemPos = devicesRV.getChildLayoutPosition(view)
        val item = devicesList.get(itemPos) as String
        // Get the device MAC address, which is the last 17 chars in the View
        val info = (view as TextView).text
        val macAddress = info.substring(info.length - 17)
        Toast.makeText(applicationContext, "Connecting", Toast.LENGTH_SHORT).show()
        bluetoothService?.connect(macAddress)
    }*/
}
