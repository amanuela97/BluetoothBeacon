package com.example.bluetooth_beacon

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.handler.BleWrapper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() , BleWrapper.BleCallback{

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 2
    private var mScanResults: HashMap<String, ScanResult>? = null
    private var  mScanCallback: ScanCallback? = null
    private var mScanning = false
    private var detectedDevices: ArrayList<ScanResult>? = ArrayList<ScanResult>()
    private lateinit var  mBleWrapper: BleWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        if (checkIfDeviceSupportsBluetooth(mBluetoothAdapter)){
            //check if bluetooth is enabled
            if (mBluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }else{
                //check for access fine location permission and start scanning
                handleLocation()
            }
        }
    }

    private fun handleBluetoothScan(){
        button.setOnClickListener {
            startScan()
        }
    }

    private fun setAdapter(){
        Log.d("DBG", "Device2 :  (${detectedDevices})")
        if (!detectedDevices.isNullOrEmpty()){
            val adaper = BluetoothDeviceListAdapter(this, detectedDevices)
            bluetooth_listView.adapter = adaper
            Log.d("DBG", "task done")
        }
    }

    companion object {
        const val SCAN_PERIOD: Long = 10000
    }

    private fun startScan() {
        Log.d("DBG", "Scan start")
        mScanResults = HashMap()
        mScanCallback = BtleScanCallback()
        val mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val filter: List<ScanFilter>? = null
        // Stops scanning after a pre-defined scan period.
        val mHandler = Handler(Looper.getMainLooper())
        mHandler.postDelayed({ mBluetoothLeScanner.stopScan(mScanCallback) }, SCAN_PERIOD)
        mScanning = true
        Toast.makeText(applicationContext, "Scanning....", Toast.LENGTH_SHORT).show()
        mBluetoothLeScanner!!.startScan(filter, settings, mScanCallback)
    }

    private inner class BtleScanCallback() : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.d("DBG", "BLE Scan Failed with code $errorCode")
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            mScanResults!![deviceAddress] = result
            mBleWrapper = BleWrapper(this@MainActivity, deviceAddress.toString())
            mBleWrapper.addListener(this@MainActivity)
            mBleWrapper.connect(false)
            Log.d("DBG", "Device address: $deviceAddress (${result})")
            detectedDevices?.clear()
            detectedDevices?.add(result)
            setAdapter()

        }
    }

    private inner class BluetoothDeviceListAdapter(
        context: Context,
        private val detectedDevices: ArrayList<ScanResult>?
    ):
        BaseAdapter() {

        private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("ViewHolder")
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            // inflate the layout for each list row
            val p1 = inflater.inflate(R.layout.bluetooth_device_list_item, p2, false)

            // get current device to be displayed
            val currentDevice = detectedDevices?.get(p0)


            // get the TextViews for the device name, address and strength
            val nameTextView = p1.findViewById<TextView>(R.id.name)
            val addressTextView = p1.findViewById<TextView>(R.id.address)
            val strengthTextView = p1.findViewById<TextView>(R.id.strength)


            //sets the text for president name, startDuty, endDuty from the currentPresident object
            if (currentDevice?.device?.name == null){
                nameTextView.text = "N/A"
            }else{
                nameTextView.text = currentDevice.device?.name.toString()
            }
            addressTextView.text = currentDevice?.device?.address.toString()
            strengthTextView.text = currentDevice?.rssi.toString()
            if (!currentDevice?.isConnectable!!){
                isEnabled(p0)
            }

            return p1
        }


        override fun getItem(p0: Int): ScanResult? {
            //returns list president at the specified position
            return detectedDevices?.get(p0)
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            //returns total number of presidents in the list
            if (!detectedDevices.isNullOrEmpty()) {
                return detectedDevices.size
            }
            return 0
        }
    }

    private fun handleLocation(){
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if(!gpsEnabled && !networkEnabled) {
            LocationDialogFragment().show(supportFragmentManager,"TAG")
        }
        handleBluetoothScan()
    }

    class LocationDialogFragment(): DialogFragment(){

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                //  dialog construction
                val builder = AlertDialog.Builder(it)
                builder.setMessage(R.string.alert_message)
                    .setPositiveButton("OK") { _, _ ->
                        // request location action
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                    .setNegativeButton("CANCEL") { _, _ ->
                        Toast.makeText(context, getText(R.string.alert_message), Toast.LENGTH_SHORT).show()
                    }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    private fun checkIfDeviceSupportsBluetooth(bluetoothAdapter: BluetoothAdapter?): Boolean{
        if (bluetoothAdapter == null){
            val txt = "No Bluetooth LE capability"
            Log.d("DBG", txt)
            Toast.makeText(applicationContext, txt, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == Activity.RESULT_OK){
                val msg = "Bluetooth is enabled"
                Log.d("DBG", msg)
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                //location permission
                handleLocation()
            }else if (resultCode == Activity.RESULT_CANCELED){
                val msg = "Bluetooth is not enabled. Application needs this feature"
                Log.d("DBG", msg)
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }
    }



    override fun onDeviceReady(gatt: BluetoothGatt) {
        for (gattService in gatt.services) {
            Log.d("DBG", "Service ${gattService.uuid}")
            if (gattService.uuid == mBleWrapper.HEART_RATE_SERVICE_UUID) {
                Log.d("DBG", "BINGO!!!")
                /* setup the system for the notification messages */
                mBleWrapper.getNotifications(
                    gatt,
                    mBleWrapper.HEART_RATE_SERVICE_UUID,
                    mBleWrapper.HEART_RATE_MEASUREMENT_CHAR_UUID
                )
            }
        }

    }

    override fun onDeviceDisconnected() {
        Log.d("DBG", "Disconnected")
    }

    @SuppressLint("SetTextI18n")
    override fun onNotify(characteristic: BluetoothGattCharacteristic) {
        val flag = characteristic.properties
        Log.i("DBG", "Heart rate flag: $flag")
        val format: Int
        if (flag and 0x01 != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16
            Log.d("DBG", "Heart rate format UINT16.")
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8
            Log.d("DBG", "Heart rate format UINT8.")
        }
        val heartRate = characteristic.getIntValue(format, 1)
        Log.i("DBG", "Heart rate value: $heartRate")
        mBleWrapper.removeListener(this)
        heart_rate_tv.text = "$heartRate BPM"
    }
}