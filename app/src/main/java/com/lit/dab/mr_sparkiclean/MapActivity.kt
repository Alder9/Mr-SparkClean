package com.lit.dab.mr_sparkiclean

import android.animation.FloatEvaluator
import android.content.Intent
import android.graphics.Color
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
import kotlinx.android.synthetic.main.map_layout.*
import kotlinx.coroutines.experimental.android.UI
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.experimental.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.xml.transform.Result
import kotlin.collections.ArrayList
import kotlin.math.abs

class MapActivity : AppCompatActivity(){

    lateinit var sparkiImageView: ImageView
    lateinit var mTextView: TextView
    lateinit var mButton: Button

    var obstacleImageViews = ArrayList<ImageView>()
    var o = 0
    var greenImageViews = ArrayList<ImageView>()
    var g = 0
    var blueImageViews = ArrayList<ImageView>()
    var b = 0

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

        var redMap = ControlActivity.mapRed
        var blackMap = ControlActivity.mapBlack
        var greenMap = ControlActivity.mapGreen
        var blueMap = ControlActivity.mapBlue
        var imgWidth = ControlActivity.imgWidth


        populateMap(redMap, greenMap, blueMap, blackMap, imgWidth)

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
            tempGraph[1][3] = 0
            prev = runDijkstras(tempGraph,0)
            Log.i("Dijk, prev:", prev.toString())
            path = reconstructPath(prev,0,12)
            Log.i("Dijk, prev:", path.toString())
        }

//        getObstacleCoords(60.0f,70.0f)
    }

    private val NUM_X_CELLS: Int = 6
    private val NUM_Y_CELLS: Int = 4
    var tempGraph = Array(NUM_Y_CELLS, {IntArray(NUM_X_CELLS)})

    // Conversion functions

//    var tempGraph = arrayOf(intArrayOf())

    // Dijkstra's for path planning
    private fun setUpDijkstras(graph: Array<IntArray>): Array<IntArray>{
        for(j in 0..NUM_Y_CELLS-1) {
            for(i in 0..NUM_X_CELLS-1){
                graph[j][i] = 1
            }
        }
        graph[0][0] = 0
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

    private fun populateMap(cornerMap: ArrayList<Int>, objGreen: ArrayList<Int>, objBlue: ArrayList<Int>, obstacles: ArrayList<Int>, imgWidth: Int){
        val x1 = cornerMap[0] % imgWidth
        val y1 = cornerMap[0] / imgWidth
        val x2 = cornerMap[1] % imgWidth
        val y2 = cornerMap[1] / imgWidth

        val xP = minOf(x1, x2)
        val yP = minOf(y1, y2)

        val mapWidth = abs(x1 - x2)
        val mapHeight = abs(y1 - y2)

        for(i in objGreen){
            val xGrn = objGreen[i] % imgWidth
            val yGrn = objGreen[i] / imgWidth
            val iIdx = (xGrn-xP)/(mapWidth) * NUM_X_CELLS
            val jIdx = (yGrn - yP)/(mapHeight) * NUM_Y_CELLS
            tempGraph[iIdx][jIdx] = 2
            drawGreenObj(iIdx, jIdx)
        }

        for(i in objBlue){
            val xBlu = objBlue[i] % imgWidth
            val yBlu = objBlue[i] / imgWidth
            val iIdx = (xBlu-xP)/(mapWidth) * NUM_X_CELLS
            val jIdx = (yBlu - yP)/(mapHeight) * NUM_Y_CELLS
            tempGraph[iIdx][jIdx] = 2
            drawBlueObj(iIdx, jIdx)
        }

        for(i in obstacles){
            val xOb = obstacles[i] % imgWidth
            val yOb = obstacles[i] / imgWidth
            val iIdx = (xOb-xP)/(mapWidth) * NUM_X_CELLS
            val jIdx = (yOb - yP)/(mapHeight) * NUM_Y_CELLS
            tempGraph[iIdx][jIdx] = 1
            drawObstacle(iIdx, jIdx)
        }

        for(i in 0..15){
            Log.d("Graph index:", tempGraph[i].toString())
        }
    }

//    private fun isNotEmpty(arr: MutableList<Int>, len: Int): Boolean{
//        for(i in 0..len) {
//            if (arr[i] >= 0) {
//                return (true)
//            }
//        }
//        return(false)
//    }

//    private fun getMinIndex(arr: MutableList<Int>, len: Int): Int{
//        var minIndex = 0
//        Log.i("Dijk, arr",arr.toString())
//        for(i in 0..len-1){
//            if((arr[i] < arr[minIndex] && arr[i] >= 0)){
//                Log.i("Dijk, minIndex",i.toString())
//                minIndex = i
//            }
//        }
//        if(arr[minIndex] == -1){
//            return -1
//        }
//        return minIndex
//
//    }

    private fun getTravelCost(graph: Array<IntArray>, vertexSource: Int, vertexDest: Int): Int {
        val areNeighboring: Boolean
        val s : List<Any> = vertex_index_to_ij_coordinates(vertexSource)
        val d : List<Any> = vertex_index_to_ij_coordinates(vertexDest)

        val sOk: Boolean? = s.first() as? Boolean
        val dOk: Boolean? = d.first() as? Boolean

        if(!sOk!!){
            return(255)
        }
        if(!dOk!!) {
            return (255)
        }

        val sI: Int? = s.get(1) as? Int
        val dI: Int? = d.get(1) as? Int

        val sJ: Int? = s.get(2) as? Int
        val dJ: Int? = d.get(2) as? Int

        Log.i("Dijk, si/j", Pair(sI,sJ).toString())
        Log.i("Dijk, di/j", Pair(dI,dJ).toString())

        areNeighboring = (abs(sI!!-dI!!) + abs(sJ!!-dJ!!) <= 1)

        Log.i("Dijk, neigh", areNeighboring.toString())
        Log.i("Dijk, ret", graph[dJ][dI].toString())

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
        val q_cost: Queue<Int> = ArrayDeque<Int>()
        val dist: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}
        val prev: MutableList<Int> = MutableList(NUM_Y_CELLS*NUM_X_CELLS) {0}

        var currentVertex: Int = -1
        var travelCost: Int = -1
        var minIndex: Int = -1

        for(j in 0..NUM_Y_CELLS*NUM_X_CELLS-1) {
            dist[j] = 255
            prev[j] = -1
            q_cost.add(j)
        }
        dist[sourceVertex] = 0

        while(!q_cost.isEmpty()) {
            minIndex = q_cost.min()!!
            Log.i("Dijk, min", minIndex.toString())
            if(minIndex < 0){
                break
            }
            currentVertex = minIndex
            q_cost.remove(minIndex)

            // Check neighbors
            for (i in 0..NUM_Y_CELLS*NUM_X_CELLS-1) {
                var alt: Int = -1
                travelCost = getTravelCost(graph, currentVertex, i)
                Log.i("Dijk, travel", travelCost.toString())
                if (travelCost == 255) {
                    continue
                }
                alt = dist[currentVertex] + travelCost
                Log.i("Dijk, alt",alt.toString())
                if(alt < dist[i]){
                    dist[i] = alt
                    Log.i("Dijk, dist",dist.toString())
                    Log.i("Dijk, i",i.toString())
                    Log.i("Dijk, qcost",q_cost.toString())
                    prev[i] = currentVertex
                }
            }
        }
        Log.i("Dijk, dist",dist.toString())
        Log.i("Dijk, qcost",q_cost.toString())
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
        Log.i("Dijk, path", path.toString())
        var finalPath = MutableList(lastIndex+1) { 0 }
        for(i in 0..lastIndex-1){
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

    //function to draw an obstacle once we have found them
    private fun drawObstacle(i: Any, j: Any){
        obstacleImageViews.add(ImageView(this))

        val iI: Int = i as Int
        val jI: Int = j as Int

        mapLayout.addView(obstacleImageViews[i])

        obstacleImageViews[o].x = iI*66.6F
        obstacleImageViews[o].y = jI*66.6F
        obstacleImageViews[o].layoutParams.height = 66
        obstacleImageViews[o].layoutParams.width = 66
        obstacleImageViews[o].setBackgroundColor(Color.BLACK)

        o++
    }

    private fun drawBlueObj(i: Any, j: Any){
        val iI: Int = i as Int
        val jI: Int = j as Int

        blueImageViews[b].x = iI*66.6F
        blueImageViews[b].y = jI*66.6F
        blueImageViews[b].layoutParams.height = 66
        obstacleImageViews[b].layoutParams.width = 66
        obstacleImageViews[b].setBackgroundColor(Color.BLUE)

        b++
    }

    private fun drawGreenObj(i: Any, j: Any){
        val iI: Int = i as Int
        val jI: Int = j as Int

        greenImageViews[b].x = iI*66.6F
        greenImageViews[b].y = jI*66.6F
        greenImageViews[b].layoutParams.height = 66
        greenImageViews[b].layoutParams.width = 66
        greenImageViews[b].setBackgroundColor(Color.GREEN)
    }
}