package com.example.bluetooth_beacon

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 2
    private val LOCATION_PERMISSION_CODE = 1
    private var mScanResults: HashMap<String, ScanResult>? = null
    private var  mScanCallback: ScanCallback? = null
    private var mScanning = false

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
                askForLocationPermission()
            }
        }
    }

    private fun handleBluetoothScan(){
        button.setOnClickListener {
            startScan()
            Toast.makeText(applicationContext,"Scanning....", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        const val SCAN_PERIOD: Long = 5000
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
        val mHandler = Handler(Looper.myLooper()!!)
        mHandler.postDelayed({mBluetoothLeScanner.stopScan(mScanCallback)}, SCAN_PERIOD)
        mScanning = true
        mBluetoothLeScanner!!.startScan(filter, settings, mScanCallback)
    }

    private inner class BtleScanCallback : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.d("DBG", "BLE Scan Failed with code $errorCode")
        }
        @RequiresApi(Build.VERSION_CODES.O)
        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            mScanResults!![deviceAddress] = result
            Log.d("DBG", "Device address: $deviceAddress (${result.isConnectable})")
        }
    }

    private fun askForLocationPermission(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.d("DBG", "No fine location access")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        }else{
            handleBluetoothScan()
        }
    }

    private fun checkIfDeviceSupportsBluetooth(bluetoothAdapter: BluetoothAdapter?): Boolean{
        if (bluetoothAdapter == null){
            val txt = "No Bluetooth LE capability"
            Log.d("DBG", txt)
            Toast.makeText(applicationContext,txt,Toast.LENGTH_SHORT).show()
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
                Toast.makeText(applicationContext,msg,Toast.LENGTH_LONG).show()
                //check for access fine location permission
                askForLocationPermission()
            }else if (resultCode == Activity.RESULT_CANCELED){
                val msg = "Bluetooth is not enabled. Application needs this feature"
                Log.d("DBG", msg)
                Toast.makeText(applicationContext,msg,Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty()){
                Toast.makeText(applicationContext,"Location permission granted!", Toast.LENGTH_SHORT).show()
                handleBluetoothScan()
            }else{
                Toast.makeText(applicationContext,"Location permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}