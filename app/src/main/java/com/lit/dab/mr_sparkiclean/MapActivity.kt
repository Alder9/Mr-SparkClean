package com.lit.dab.mr_sparkiclean

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
import android.text.BoringLayout
import android.util.Half.toFloat
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
import kotlin.math.abs

class MapActivity : AppCompatActivity(){

    lateinit var sparkiImageView: ImageView
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
//            sendPath()
//            runDijkstras()
            val prev: MutableList<Int>
            val path: MutableList<Int>
            setUpDijkstras(tempGraph)
            tempGraph[1][0] = 0
            tempGraph[1][1] = 0
            prev = runDijkstras(tempGraph,0)
            Log.i("Prev", prev.toString())
            path = reconstructPath(prev,0,6)
            Log.i("Prev", path.toString())
        }
    }

    private val NUM_X_CELLS: Int = 6
    private val NUM_Y_CELLS: Int = 4
    var tempGraph = Array(NUM_Y_CELLS, {IntArray(NUM_X_CELLS)})

    // Conversion functions

//    var tempGraph = arrayOf(intArrayOf())

    // Dijkstra's for path planning
    private fun setUpDijkstras(graph: Array<IntArray>): Array<IntArray>{
        for(j in 0..NUM_Y_CELLS) {
            for(i in 0..NUM_X_CELLS){
                graph[j][i] = 1
            }
            graph[0][0] = 0
        }
        return graph
    }

    private fun vertex_index_to_ij_coordinates(vertexIndex: Int): List<Any>{
        val i: Int = vertexIndex % NUM_X_CELLS
        val j: Int = vertexIndex / NUM_X_CELLS

        if(i<0 || j < 0 || i >= NUM_X_CELLS || j>= NUM_Y_CELLS){
            val pckLst: MutableList<Any> = mutableListOf(false, i, j)
            return(pckLst)
        }
        else{
            val pckLst: MutableList<Any> = mutableListOf(true, i, j)
            return(pckLst)
        }

    }

    private fun ij_coordinates_to_vertex_index(i: Int, j: Int): Int{
        return(j*NUM_X_CELLS + i)
    }

    private fun isNotEmpty(arr: MutableList<Int>, len: Int): Boolean{
        for(i in 0..len) {
            if (arr[i] >= 0) {
                return (true)
            }
        }
        return(false)
    }

    private fun getMinIndex(arr: MutableList<Int>, len: Int): Int{
        var minIndex = 0
        for(i in 0..len){
            if(arr[i] < 0 || (arr[i] < arr[minIndex] && arr[i] >= 0)){
                minIndex = i
            }
        }
        if(arr[minIndex] == -1){
            return -1
        }
        return minIndex

    }

    private fun getTravelCost(graph: Array<IntArray>, vertexSource: Int, vertexDest: Int): Int {
        val areNeighboring: Boolean
        val s : List<Any> = vertex_index_to_ij_coordinates(vertexSource)
        val d : List<Any> = vertex_index_to_ij_coordinates(vertexDest)

        val sOk: Boolean? = s.first() as? Boolean
        val dOk: Boolean? = d.first() as? Boolean

        if(!sOk!!){
            return(255)
        }
        if(!dOk!!){
            return(255)
        }

        val sI: Int? = s.get(2) as? Int
        val dI: Int? = d.get(2) as? Int

        val sJ: Int? = s.get(3) as? Int
        val dJ: Int? = d.get(3) as? Int

        areNeighboring = (abs(sI!!-dI!!)+ abs(sJ!!-dJ!!) <= 1)

        if(graph[dJ][dI] == 0){
            return 255
        }
        if(areNeighboring){
            return 1
        }
        else{
            return 255
        }
    }

    private fun runDijkstras(graph: Array<IntArray>, sourceVertex: Int): MutableList<Int>{
        val q_cost: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}
        val dist: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}
        val prev: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}

        var currentVertex: Int = -1
        var currentCost: Int = -1
        var travelCost: Int = -1
        var minIndex: Int = -1

        for(j in 0..NUM_Y_CELLS*NUM_X_CELLS) {
            q_cost[j] = -1
            dist[j] = 255
            prev[j] = -1
        }
        dist[sourceVertex] = 0
        q_cost[sourceVertex] = 0

        while(isNotEmpty(q_cost,NUM_Y_CELLS*NUM_X_CELLS)) {
            minIndex = getMinIndex(q_cost, NUM_Y_CELLS*NUM_X_CELLS)
            if(minIndex < 0){
                break
            }
            currentVertex = minIndex
            currentCost = q_cost[currentVertex]
            q_cost[currentVertex] = -1
            for (i in 0..NUM_Y_CELLS*NUM_X_CELLS) {
                var alt: Int = -1
                travelCost = getTravelCost(graph, currentVertex, i)
                if (travelCost == 255) {
                    continue
                }
                alt = dist[currentVertex] + travelCost
                if(alt < dist[i]){
                    dist[i] = alt
                    q_cost[i] = alt
                    prev[i] = currentVertex
                }
            }
        }
        return(prev)
    }

    private fun reconstructPath(prev: MutableList<Int>, sourceVertex: Int, destVertex: Int): MutableList<Int>{
        var path: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}
        var lastIndex: Int = 0
        path[lastIndex++] = destVertex
        var lastVertex: Int = prev[destVertex]
        while(lastVertex != -1){
            path[lastIndex++] = lastVertex
            lastVertex = prev[lastVertex]
        }
        var finalPath = MutableList(lastIndex+1) { 0 }
        for(i in 0..lastIndex){
            finalPath[i] = path[lastIndex-1-i]
        }
        finalPath[lastIndex] = -1
        return finalPath
    }

    private fun setUpBins(): MutableList<Pair<Int,Boolean>>{
        var bins: MutableList<Pair<Int,Boolean>> = mutableListOf<Pair<Int,Boolean>>()
        var bin1 = Pair(0,true)
        var bin2 = Pair(0,true)
        var bin3 = Pair(0,true)
        var bin4 = Pair(0,true)
        var bin5 = Pair(0,true)
        var bin6 = Pair(0,true)
        bins.add(bin1)
        bins.add(bin2)
        bins.add(bin3)
        bins.add(bin4)
        bins.add(bin5)
        bins.add(bin6)

        return bins
    }

    private fun findBin(graph: Array<IntArray>, bins: MutableList<Pair<Int,Boolean>>, sourceVertex: Int): Int{
        for(i in bins){
            if(i.second){
                return i.first
            }
        }
        return 0
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
    private fun drawObstacle(){

    }




}