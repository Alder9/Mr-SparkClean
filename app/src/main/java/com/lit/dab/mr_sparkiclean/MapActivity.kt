package com.lit.dab.mr_sparkiclean

import android.animation.FloatEvaluator
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.provider.Contacts
import android.support.annotation.MainThread
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.experimental.android.UI
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.experimental.*
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import javax.xml.transform.Result

class MapActivity : AppCompatActivity(){

    lateinit var sparkiImageView: ImageView
    lateinit var obstacleImageView: ImageView
    lateinit var mTextView: TextView
    lateinit var mButton: Button

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

        sparkiImageView = findViewById(R.id.sparki)
        mTextView = findViewById(R.id.textCleaning)
        mButton = findViewById(R.id.startButton)
        if (isCleaning == "cleaning") {
            mTextView.setText("Now Cleaning")
        }

        mButton.setOnClickListener {
            sendPath()
        }

        getObstacleCoords(60.0f,70.0f)
    }

    val temp = listOf(Pair(0.5F, 0.5F), Pair(0.7F, 0.7F))
    var busy = false
    var index = 0

    private fun sendPath() {
        if(index == temp.size){
            index = 0
            return
        }
        val coords = temp.get(index)
        launch (UI) {
            while(busy){
                delay(2000)
            }
            busy = true
            Log.i("Pair", coords.toString())
            val job = async { sendCoordinateAndUpdateSparki(coords.first, coords.second) }
            job.await()
        }
        index++
        sendPath()
    }

    private fun sendCoordinateAndUpdateSparki(x: Float, y: Float) {
        async(UI){
            try{
                busy = true
                var resp = ""
                val x_string = x.toString()
                val y_string = y.toString()

                val padding_x: String = "0".repeat(5 - x_string.length)
                val padding_y: String = "0".repeat(5 - y_string.length)
                val stringToSend = x.toString() + padding_x + y.toString() + padding_y
                val work1 = async(CommonPool) { sendCommand(stringToSend) }
                work1.await()
                val work2 = async(CommonPool) {receiveResponse()}
                val result = work2.await()
                Log.i("Coordinate", "Response1: " + result)
                Log.i("Coordinate", "Sending Coordinate")
                Log.i("Coordinate", "Response2: " + result)

                if(result == "97"){
                    setSparkiMapPose(x,1-y)
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }
            finally {
                busy = false
            }

        }

    }

    suspend fun sendCommand(input: String) {
        if(ControlActivity.m_bluetoothSocket != null) {
            try {
                ControlActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun receiveResponse(): String {
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
    private fun setSparkiMapPose(xbias: Float, ybias: Float) {
        Log.i("Coordinate", "test")
        val sparkiParams = sparkiImageView.layoutParams as ConstraintLayout.LayoutParams
        sparkiParams.horizontalBias = xbias
        sparkiParams.verticalBias = ybias
        sparkiImageView.layoutParams = sparkiParams

        Log.i("Reponse", xbias.toString())
    }

    // function to draw an obstacle once we have found them
    private fun drawObstacle(xbias: Float, ybias: Float){
        val obstacleParams = obstacleImageView.layoutParams as ConstraintLayout.LayoutParams
        obstacleParams.horizontalBias = xbias
        obstacleParams.verticalBias = ybias
        obstacleImageView.layoutParams = obstacleParams
    }

    private fun getObstacleCoords(x: Float, y: Float){
        drawObstacle(x, y)
    }

    val tempGraph = Array(4, {IntArray(4)})

    // Dijkstra's for path planning
    private fun runDijkstras(graph: Array<Int>, sourceVertex: Int){

    }
}