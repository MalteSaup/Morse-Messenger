package com.mosy.morsemessenger

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bluetooth_list_item.*
import java.io.UnsupportedEncodingException
import android.content.Intent
import android.view.*

class MainActivity : OptionsMenuActivity() {

    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var devicesList : ArrayList<BluetoothDevice>
    private lateinit var btSwitch: Switch
    private var myService: BluetoothConnectionService? = null
    private var bluetoothService: BluetoothService? = null
    private var isBound = false
    private var isConnected = false
    private lateinit var mRunnable : Runnable
    private lateinit var mHandler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicesRV.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        devicesList = ArrayList()
        devicesAdapter = DevicesAdapter(devicesList, {device : BluetoothDevice -> onDeviceClicked(device)})
        devicesRV.adapter = devicesAdapter

        bluetoothService = BluetoothService(handler)
        if(!isServiceRunning(BluetoothConnectionService::class.java)){
            startService(Intent(applicationContext, BluetoothConnectionService::class.java))
            bindService(Intent(applicationContext, BluetoothConnectionService::class.java), myConnection, Context.BIND_AUTO_CREATE)
        }

        implementSwitchOnClickListener()
        initializeBluetoothService()
        disconnectBtn.isClickable = false

        mHandler = Handler()  //Handler der mit dem mRunnable prüft ob Bluetooth an ist und dementsprechend den Switch an oder aus schaltet
        mRunnable = Runnable{
            if(checkForBluetooth() && !btSwitch.isChecked){
                onOffTV.text = getString(R.string.switchStatusOn)
                btSwitch.isChecked = true
            }
            else if(!checkForBluetooth() && btSwitch.isChecked){
                onOffTV.text = getString(R.string.switchStatusOff)
                btSwitch.isChecked = false
            }
            mHandler.postDelayed(this.mRunnable, 500)
        }
        mRunnable.run()
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

    fun checkForBluetooth() : Boolean{
        if(bluetoothService?.enabled!!) return true
        return false
    }

    //Toast if Bluetoothconnection is established or not
    fun messageConnection(msg: Message){
        if (msg.arg1 == 1) {
            disconnectBtn.isClickable = true
            isConnected = true
            Toast.makeText(applicationContext, "Verbindung aufgebaut: ${msg.obj as String}", Toast.LENGTH_SHORT).show()
        }
        else {
            isConnected = false
            Toast.makeText(applicationContext, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }

    //Checkt ob Hintergrund Service läuft
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
        btSwitch = findViewById(R.id.bluetoothSwitch)
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
        isConnected = false
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

    //Fügt vorhandene Bluetooth Geräte in der Umgebung dem Recycler View hinzu
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

    //Click-Listener for disconnecting all connected Devices
    fun disconnectDevice (view: View) {
        val pairedDevices: Set<BluetoothDevice> = BluetoothAdapter.getDefaultAdapter().bondedDevices
        if (pairedDevices.isNotEmpty() && isConnected) {
            for (device in pairedDevices) {
                val macAddress = device.address
                bluetoothService?.disconnect(macAddress)
                Toast.makeText(applicationContext, "Verbindung getrennt", Toast.LENGTH_SHORT).show()
                isConnected = false
                //Change Icon in RecyclerView-Element
                //bluetoothImage.setImageResource(R.drawable.ic_bluetooth_24dp)
                devicesAdapter.notifyDataSetChanged()
            }
        }
        disconnectBtn.isClickable = false
    }

    //Click-Listener for Device in Devices-RecyclerView. To create connection with Bluetooth-Device.
    private fun onDeviceClicked (device : BluetoothDevice) {
        // Get the device MAC address
        val macAddress = device.address
        Toast.makeText(applicationContext, "Connecting: ${device.name}", Toast.LENGTH_SHORT).show()
        bluetoothService?.connect(macAddress)
        //Change Icon in RecyclerView-Element
        if(device.bondState == BOND_BONDED ) {
            /*TODO: Problem lösen (Auch in DevicesAdapter): Being bonded (paired) with a remote device does not necessarily mean the device is currently connected.
            It just means that the pending procedure was completed at some earlier time, and the link key is still stored locally, ready to use on the next connection*/
            Log.i ("keks" ,device.bondState.toString())
            for(i in 0 until devicesList.size){
                if(devicesList[i] == device){
                    var k = devicesList[i]
                    devicesList[i] = devicesList[0]
                    devicesList[0] = k
                }
                devicesRV.smoothScrollToPosition(0);
                //bluetoothImage.setImageResource(R.drawable.ic_bluetooth_connected_24dp)
                devicesAdapter.notifyDataSetChanged()
            }
        }
    }

    //Click-Listener for toChat-Button. Starts the ChatActivity.
    fun implementChatBtnClickListener(view: View) {

        //Can only open new Activity, when nameET is filled and bluetooth-device is connected
        if (!nameET.text.isBlank() && bluetoothService?.enabled!! && bluetoothService?.pairedDevices!=null && bluetoothService?.pairedDevices!!.isNotEmpty() && isConnected) {
            Log.i("TEST pairedDevices", bluetoothService?.pairedDevices.toString())
            val intent = Intent(this, ChatActivity::class.java)
            myService?.setBt(bluetoothService)
            intent.putExtra("username", nameET.text.toString())
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
- Chat-Button geht nicht immer Bugfix
- Bugfix: Icon, wenn Verbindung steht (siehe oben + Devices Adapter
- Bugfix: Color Switch Farbe
- CLK:INTEGER , setzt den Clockspeed, also die Geschwindigkeit eines Punktes in Millisekunden. Min:10, max: 2000 (Steuern in der App mit Slider)
- SENT: (Nachricht wurde fertig gesendet. Grafisch darstellen durch Haken?)
- ACK: , (Nachricht wurde empfangen. Grafisch darstellen durch zweiten Haken?)
- Dopplungen der Geräte in der Liste vermeiden --> Vorm hinzufügen filtern, ob MAC-Adresse bereits in Liste
- Nachrichten in Datenbank speichern während Verbindung existiert - bei Verbindungsabbruch Datenbank leeren
- Kommentare am Code !!!!!!!!!!!!!!!!!!
 */