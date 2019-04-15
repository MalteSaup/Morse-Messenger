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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //create LayoutManager
        devicesRV.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        devicesList = ArrayList()
        devicesAdapter = DevicesAdapter(devicesList)
        devicesRV.adapter = devicesAdapter

        giveSwitchOnClickListener()

        //TODO: ItemClickListener auf RecyclerView implementieren
        initializeBluetoothService()
    }

    //TODO: On/Off mit Switch implementieren

    fun giveSwitchOnClickListener(){
        btSwitch = findViewById(R.id.bluetoothSwitch);
        btSwitch.setOnClickListener{
            if(btSwitch.isChecked){
                if (bluetoothService?.enabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
                onOffTV.text = "on"
            }
            else{
                bluetoothService?.disable()
                onOffTV.text = "off"
            }
        }
    }

    /*fun bluetoothOn(view: View){
        if (bluetoothService?.enabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        onOffTV.text = R.string.switchStatusOn as String
    }

    fun bluetoothOff(view: View){
        bluetoothService?.disable()
        onOffTV.text = R.string.switchStatusOff as String
    }*/

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

    fun discoverPairedDevices(view: View){
        bluetoothService?.discover()
        Toast.makeText(baseContext, "Discovering Paired Devices", Toast.LENGTH_SHORT).show()
        devicesList.clear()
        devicesAdapter?.notifyDataSetChanged()
        registerReceiver(blReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

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

    fun messageRead(msg: Message){
        try {
            //textViewMessage.text = msg.obj as String
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    fun messageConnection(msg: Message){
        if (msg.arg1 == 1) {
            Toast.makeText(applicationContext, "Connected to Device: ${msg.obj as String}", Toast.LENGTH_SHORT)
        }
        else {
            Toast.makeText(applicationContext, "Connection Failed", Toast.LENGTH_SHORT)
        }
    }

    private val deviceClickListener = object: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            Toast.makeText(applicationContext, "Connecting", Toast.LENGTH_SHORT).show()

            val info = (view as TextView).text
            val macAddress = info.substring(info.length - 17)
            bluetoothService?.connect(macAddress)
        }

    }

    //TODO: ONClick für "Zum Chat"-Button (hier starten der neuen Activity (oder evtl doch Fragment?))
    //funktioniert nur, wenn Name ausgefüllt und Bluetooth-Verbindung zu einem Device steht


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
