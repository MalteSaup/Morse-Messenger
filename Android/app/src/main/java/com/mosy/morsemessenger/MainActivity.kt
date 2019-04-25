package com.mosy.morsemessenger

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException

class MainActivity : AppCompatActivity() {

    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var devicesList : ArrayList<BluetoothDevice>
    private lateinit var btSwitch: Switch;
    private var myService: BluetoothConnectionService? = null
    private var isBound = false

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder
        ) {
            val binder = service as BluetoothConnectionService.MyLocalBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(applicationContext, BluetoothConnectionService::class.java)
        if(!isServiceRunning(BluetoothConnectionService::class.java)){
            startService(intent)

        }
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        devicesRV.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        devicesList = ArrayList()
        devicesAdapter = DevicesAdapter(devicesList, {device : BluetoothDevice -> onDeviceClicked(device)})
        devicesRV.adapter = devicesAdapter

        implementSwitchOnClickListener()
        initializeBluetoothService()
    }
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
    fun initializeBluetoothService(){


        //Check if Bluetooth is already enabled on device.
        if (myService?.bluetoothService?.enabled == true) {
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
        if (myService?.bluetoothService?.enabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            /*Wenn man den Switch auf an stellt, wird ein Dialog gezeigt (Zulassen/Ablehnen)
            * TODO: Wenn man auf Ablehnen klickt, muss der Switch wieder auf off gestellt werden */
        }
        onOffTV.text = getString(R.string.switchStatusOn)
    }

    //Turn Bluetooth Off
    fun bluetoothOff(){
        myService?.bluetoothService?.disable()
        devicesList.clear()
        devicesAdapter.notifyDataSetChanged()
        onOffTV.text = getString(R.string.switchStatusOff)
    }

    //Shows Bluetooth-Devices in list, which have already been connected in the past
    fun showPairedDevices(view: View){
        if (myService?.bluetoothService?.enabled!!) {
            for (device in myService?.bluetoothService?.pairedDevices!!)
                devicesList.add(device)
            // add the name to the list
            devicesAdapter.notifyItemInserted(devicesList.size -1)

            Toast.makeText(applicationContext, "Zeige Geräte", Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(applicationContext, "Bluetooth ist aus", Toast.LENGTH_SHORT).show()
    }

    //Find Bluetooth-Devices and show them in List
    fun discoverPairedDevices(view: View){
        if (myService?.bluetoothService?.enabled!!) {
            myService?.bluetoothService?.discover()
            Toast.makeText(baseContext, "Suche Geräte", Toast.LENGTH_SHORT).show()
            devicesList.clear()
            devicesAdapter.notifyDataSetChanged()
            registerReceiver(blReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } else
            Toast.makeText(applicationContext, "Bluetooth ist aus", Toast.LENGTH_SHORT).show()
    }

    //TODO: Kommentar. was macht das?
    private val blReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                devicesList.add(device)
                // add the name to the list
                devicesAdapter.notifyItemInserted(devicesList.size -1)
            }
        }
    }


    //Click-Listener for Device in Devices-RecyclerView. To create connection with Bluetooth-Device.
    private fun onDeviceClicked (device : BluetoothDevice) {
        // Get the device MAC address
        val macAddress = device.address
        Toast.makeText(applicationContext, "Connecting: ${device.name}", Toast.LENGTH_SHORT).show()
        myService?.bluetoothService?.connect(macAddress)
    }

    //Click-Listener for toChat-Button. Starts the ChatActivity.
    fun implementChatBtnClickListener(view: View) {
        //Can only open new Activity, when nameET is filled and bluetooth-device is connected
        if (!nameET.text.isBlank() && myService?.bluetoothService?.enabled!! /*TODO: && Verbindung zu Gerät ist hergestellt)*/) {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
        else Toast.makeText(applicationContext, "Name und/oder Geräteverbindung fehlt", Toast.LENGTH_SHORT).show()
    }
}