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
        initializeSeekBar()
        disconnectBtn.isClickable = false

        mHandler = Handler()  //to check with mRunnable, if bluetooth is enabled; switches Switch on/off
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

    //Toast if Bluetooth-Connection is established or not
    fun messageConnection(msg: Message){
        if (msg.arg1 == 1) {
            disconnectBtn.isClickable = true
            isConnected = true
            bluetoothImage.setImageResource(R.drawable.ic_bluetooth_connected_24dp)
            Toast.makeText(applicationContext, "Verbindung aufgebaut: ${msg.obj as String}", Toast.LENGTH_SHORT).show()
        }
        else {
            isConnected = false
            Toast.makeText(applicationContext, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }

    //Check if background service is running
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
            for (device in bluetoothService?.pairedDevices!!) {
               devicesList.add(device)
            }
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

    //Inserts nearby bluetooth-devices into recyclerview
    private val blReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if(!devicesList.contains(device)){
                    // add the name to the list if it is not already in the list
                    devicesList.add(device)
                    devicesAdapter.notifyItemInserted(devicesList.size -1)
                }
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
                bluetoothImage.setImageResource(R.drawable.ic_bluetooth_24dp)
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

            //places clicked device on first position in list
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
            intent.putExtra("speed", sendSpeedSB.progress.toString())
            startActivity(intent)
        }
        else Toast.makeText(applicationContext, "Name und/oder Geräteverbindung fehlt", Toast.LENGTH_SHORT).show()
    }

    fun initializeSeekBar() {

        sendSpeedSB.max = 500
        sendSpeedTV.text = sendSpeedSB.progress.toString() + " ms"

        sendSpeedSB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //minimum = 10; (Because api-level <26 sendSpeedSB.min does not work)
                if (progress <10) {
                    sendSpeedSB.post(Runnable { sendSpeedSB.progress = 10 })
                }
                sendSpeedTV.text = progress.toString() + " ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
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
Prioritäten: 1= sehr wichtig
- 1 ankommende Nachricht nicht doppelt bzw. inkl. letzter Nachricht senden
- 1 App zurück zu Startbildschirm, wenn Verbindung abreißt
- 1 Chat-Button geht nicht immer Bugfix--> Bug immer nur beim ersten Start der App
- 1 Kein Leerzeichen an Ende des Strings beim senden. Vor allem Problem, bei Autovervollständigung
- 2 Symbol richtig angezeigt bei Verbindung --> getestet, funktioniert nicht bzw. nur bei der ersten Verbindung --> Exception wird angezeigt
- 3 SENT: (Nachricht wurde fertig gesendet. Grafisch darstellen durch Haken?) siehe write in Bluetooth-Service
- 3 ACK: , (Nachricht wurde empfangen. Grafisch darstellen durch zweiten Haken?)
- Bugfix: Color Switch Farbe
- Nachrichten in Datenbank speichern während Verbindung existiert - bei Verbindungsabbruch Datenbank leeren
- Kommentare am Code !!!!!!!!!!!!!!!!!!
*/