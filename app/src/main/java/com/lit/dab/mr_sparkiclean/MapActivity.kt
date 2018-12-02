package com.lit.dab.mr_sparkiclean

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.io.IOException

class MapActivity : AppCompatActivity(){


    companion object {
        lateinit var m_address: String
        lateinit var isCleaning: String
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when(item.itemId) {
            R.id.tab1 -> {
                val intent = Intent(this, ControlActivity::class.java)
                intent.putExtra(m_address, m_address)
                startActivity(intent)
                overridePendingTransition(0,0)
                return@OnNavigationItemSelectedListener true
            }

            R.id.tab2 -> {
                val intent = Intent(this, MapActivity::class.java)
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
        setContentView(R.layout.map_layout)
        m_address = intent.getStringExtra(ControlActivity.m_address) // get the m_address that ControlActivity is expecting in onCreate()
        isCleaning = intent.getStringExtra(ControlActivity.isCleaning)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val sparkiImageView: ImageView = findViewById(R.id.sparki)
        val mTextView: TextView = findViewById(R.id.textCleaning)
        if(isCleaning == "cleaning"){
            mTextView.setText("Now Cleaning")
            sendCoordinate(0.345F, 0.300F, sparkiImageView)
        }
    }

    private fun sendPath(coordArray: Array<Float>) {

    }

    private fun sendCoordinate(x: Float, y: Float, sparkiImageView: ImageView) {
        val x_string = x.toString()
        val y_string = y.toString()

        val padding_x: String = "0".repeat(5 - x_string.length)
        val padding_y: String = "0".repeat(5 - y_string.length)
        val stringToSend = x.toString() + padding_x + y.toString() + padding_y
        sendCommand(stringToSend)
        val resp = receiveResponse()

        if(resp == "97"){
            Log.i("Reponse", resp)
            setSparkiMapPose(sparkiImageView, x, 1-y)
        }
    }

    private fun sendCommand(input: String) {
        if(ControlActivity.m_bluetoothSocket != null) {
            try {
                ControlActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun receiveResponse(): String {
        var responseString: String = ""
        if(ControlActivity.m_bluetoothSocket != null) {
            try {
                val response = ControlActivity.m_bluetoothSocket!!.inputStream.read()
                responseString = response.toString()
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }

        return responseString
    }

    // function to move sparki around the map
    private fun setSparkiMapPose(sparki: ImageView, xbias: Float, ybias: Float) {
        val sparkiParams = sparki.layoutParams as ConstraintLayout.LayoutParams
        sparkiParams.horizontalBias = xbias
        sparkiParams.verticalBias = ybias
    }

    // function to draw an obstacle once we have found them
    private fun drawObstacle(){

    }

    val tempGraph = Array(4, {IntArray(4)})

    // Dijkstra's for path planning
    private fun runDijkstras(graph: Array<Int>, sourceVertex: Int){

    }
}