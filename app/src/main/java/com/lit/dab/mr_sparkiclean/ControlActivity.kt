package com.lit.dab.mr_sparkiclean

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import java.util.*

class ControlActivity: AppCompatActivity(){

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var isCleaning: String
        lateinit var m_address: String
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when(item.itemId) {
            R.id.tab1 -> {
                val intent = Intent(this, ControlActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0,0)
                return@OnNavigationItemSelectedListener true
            }

            R.id.tab2 -> {
                val intent = Intent(this, MapActivity::class.java)
                isCleaning = "not"
                intent.putExtra(m_address, m_address)
                intent.putExtra(isCleaning, isCleaning)
                startActivity(intent)
                overridePendingTransition(0,0)
                return@OnNavigationItemSelectedListener true
            }

        }
        false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0,0) // remove animation when back button is pressed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)

        // Prevent crashing when flipping between tabs
        if(intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS) != null) {
            m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        }
        else{
            m_address = intent.getStringExtra(MapActivity.m_address)
        }

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        ConnectToDevice(this).execute()
        control_test_send_1.setOnClickListener {
            sendCommand("0.5000.500") // making it a limit of 5 chars to send per coordinate so 10 in total, 1/2 is x coord, 1/2 is y coord
            val resp = receiveResponse()
        }
        control_test_send_2.setOnClickListener { disconnect() }
        // Clean button pressed, go to map activity
        control_led_disconnect.setOnClickListener {
            val toMap = Intent(this, MapActivity::class.java)
            isCleaning = "cleaning"
            toMap.putExtra(m_address, m_address)
            toMap.putExtra(isCleaning, isCleaning)
            startActivity(toMap)
            overridePendingTransition(0,0)
        }
    }

    fun receiveResponse(): String {
        var responseString: String = ""
        if(m_bluetoothSocket != null) {
            try {
                val response = m_bluetoothSocket!!.inputStream.read()
                responseString = response.toString()
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }

        return responseString
    }

    private fun sendCommand(input: String) {
        if(m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect(){
        try {
            if(m_bluetoothSocket != null){
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        finish()
    }

    private class ConnectToDevice(c: Context): AsyncTask<Void, Void, String> (){
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "Please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if(m_bluetoothSocket == null || !m_isConnected){
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID) // connection between phone and bluetooth device
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery() // stops looking for other devices to connect to it
                    m_bluetoothSocket!!.connect() // know its not null
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSuccess) {
                Log.i("data", "Couldn't connect")
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }
}
