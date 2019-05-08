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
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException

class MainActivity : AppCompatActivity() {

    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var devicesList : ArrayList<BluetoothDevice>
    private lateinit var btSwitch: Switch;
    private var myService: BluetoothConnectionService? = null
    private var bluetoothService: BluetoothService? = null
    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicesRV.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        devicesList = ArrayList()
        devicesAdapter = DevicesAdapter(devicesList, {device : BluetoothDevice -> onDeviceClicked(device)})
        devicesRV.adapter = devicesAdapter

        bluetoothService = BluetoothService(handler, 42)
        if(!isServiceRunning(BluetoothConnectionService::class.java)){
            startService(Intent(applicationContext, BluetoothConnectionService::class.java))
            bindService(Intent(applicationContext, BluetoothConnectionService::class.java), myConnection, Context.BIND_AUTO_CREATE)
        }
        implementSwitchOnClickListener()
        initializeBluetoothService()
    }

    val handler = object: Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                MESSAGE_READ -> messageRead(msg)
                MESSAGE_CONNECTION -> messageConnection(msg)
            }
        }
    }

    //Read message from other device
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
            Toast.makeText(applicationContext, "Verbindung aufgebaut: ${msg.obj as String}", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(applicationContext, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
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

            /*Wenn man den Switch auf an stellt, wird ein Dialog gezeigt (Zulassen/Ablehnen)
            * TODO: Wenn man auf Ablehnen klickt, muss der Switch wieder auf off gestellt werden */
        }
        onOffTV.text = getString(R.string.switchStatusOn)
    }

    //Turn Bluetooth Off
    fun bluetoothOff(){
        bluetoothService?.disable()
        devicesList.clear()
        devicesAdapter.notifyDataSetChanged()
        onOffTV.text = getString(R.string.switchStatusOff)
    }

    //Shows Bluetooth-Devices in list, which have already been connected in the past
    fun showPairedDevices(view: View){
        if (bluetoothService?.enabled!!) {
            for (device in bluetoothService?.pairedDevices!!)
                devicesList.add(device)
            // add the name to the list
            devicesAdapter.notifyItemInserted(devicesList.size -1)

            Toast.makeText(applicationContext, "Zeige Geräte", Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(applicationContext, "Bluetooth ist aus", Toast.LENGTH_SHORT).show()
    }

    //Find Bluetooth-Devices and show them in List
    fun discoverPairedDevices(view: View){
        if (bluetoothService?.enabled!!) {
            bluetoothService?.discover()
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

    //Click-Listener for disconnecting all Devices //TODO: Code hübsch: Nicht alle Devices müssen getrennt werden, sondern nur das eine verbundene
    fun disconnectDevice (view: View) {
        for ( device in devicesList) {
            val macAddress = device.address
            bluetoothService?.disconnect(macAddress)
            Toast.makeText(applicationContext, "Verbindung getrennt", Toast.LENGTH_SHORT).show()
        }
    }

    //Click-Listener for Device in Devices-RecyclerView. To create connection with Bluetooth-Device.
    private fun onDeviceClicked (device : BluetoothDevice) {
        // Get the device MAC address
        val macAddress = device.address
        Toast.makeText(applicationContext, "Connecting: ${device.name}", Toast.LENGTH_SHORT).show()
        bluetoothService?.connect(macAddress)
    }

    //Click-Listener for toChat-Button. Starts the ChatActivity.
    fun implementChatBtnClickListener(view: View) {
        //TODO: Bugfix: App stürzt momentan ab, wenn man Name eingegeben hat, keine Verbindung steht und der toChat-Button gedrückt wird
        //Can only open new Activity, when nameET is filled and bluetooth-device is connected
        if (!nameET.text.isBlank() && bluetoothService?.enabled!! /*TODO: && Verbindung zu Gerät ist hergestellt)*/) {
            val intent = Intent(this, ChatActivity::class.java)
            myService?.setBt(bluetoothService)
            Log.d("TEST", bluetoothService?.getId() + " UFF")
            intent.putExtra("username", nameET.text)
            startActivity(intent)
        }
        else Toast.makeText(applicationContext, "Name und/oder Geräteverbindung fehlt", Toast.LENGTH_SHORT).show()
    }

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
}

/* TODO:
- App zurück zu Startbildschirm, wenn Verbindung abreißt
- Nachrichten in Datenbank speichern?
- Kommentare am Code !!!!!!!!!!!!!!!!!!
App nimmt Namen-String an und setzt diesen oben in den Balken ein (statt MorseMessenger). Problem: App wird dann automatisch umbenannt. Lösung: Name unter den Balken in neuem extra Balken anzeigen
 */