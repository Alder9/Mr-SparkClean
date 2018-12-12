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
import kotlinx.android.synthetic.main.activity_camera.*
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
import android.content.Context
import android.support.constraint.ConstraintSet
import org.jetbrains.anko.find
import kotlin.math.*

class MapActivity : AppCompatActivity(){

    private val NUM_X_CELLS: Int = 6
    private val NUM_Y_CELLS: Int = 4
    var tempGraph = Array(NUM_Y_CELLS, {IntArray(NUM_X_CELLS)})
    var counter = 0

    lateinit var sparkiImageView: ImageView
    lateinit var mTextView: TextView
    lateinit var mButton: Button
    lateinit var obstacleImageView: ImageView
    lateinit var greenImageView: ImageView
    lateinit var blueImageView: ImageView

    var currentIndex: Int = -1

    var o = 0
    var g = 0
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

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        sparkiImageView = findViewById(R.id.sparki)
        mTextView = findViewById(R.id.textCleaning)
        mButton = findViewById(R.id.startButton)
        if (isCleaning == "cleaning") {
            mTextView.setText("Now Cleaning")
        }

        setUpDijkstras(tempGraph)
        populateMap(redMap, greenMap, blueMap, blackMap, imgWidth)

        mButton.setOnClickListener {
            pickUpObjs(tempGraph)
        }

//        getObstacleCoords(60.0f,70.0f)
    }

    private fun isClean(graph: Array<IntArray>) : Boolean {
        var objFound = false
        for (arr in graph) {
            for (value in arr) {
                if (value != 0 && value != 1) {
                    objFound = true
                }
            }
        }
        if (objFound) {
            return false
        }
        return true
    }

    private fun getNextObject(graph: Array<IntArray>): Int {
        for (y in 0..NUM_Y_CELLS-1) {
            for (x in 0..NUM_X_CELLS-1) {
                if (graph[y][x] != 0 && graph[y][x] != 1) {
                    graph[y][x] = 0
                    val idx = x + y * NUM_X_CELLS
                    return idx
                }
            }
        }
        return 0
    }

    private fun findBin(graph: Array<IntArray>, bins: MutableList<Pair<Int,Boolean>>, sourceVertex: Int): Int{
        val i: Int = sourceVertex % NUM_X_CELLS
        val j: Int = sourceVertex / NUM_X_CELLS
        val s = ij_to_xy(i,j)
        var dist: Float
        var minDist: Pair<Float, Int> = Pair(1000F, 0)
        for(i in bins.indices){
            if(bins[i].second) {
                var k = bins[i].first % NUM_X_CELLS
                var l = bins[i].first / NUM_X_CELLS
                val d = ij_to_xy(k, l)
                dist = sqrt((s.first - d.first).pow(2) + (s.second - d.second).pow(2))
                if (dist < minDist.first) {
                    minDist =  Pair(dist, bins[i].first)
                }
            }
        }
        for(j in bins.indices) {
            if(bins[i].first == minDist.second) {
                var newBin = Pair(bins[i].first, false)
                bins.remove(bins[i])
                bins.add(i, newBin)
                return minDist.second
            }
        }
        return 0
    }

    private fun pickUpObjs(graph: Array<IntArray>){
        var sourceVertex = 0
        val bins: MutableList<Pair<Int,Boolean>> = MutableList(0){Pair(0, false)}
        for (y in 0..NUM_Y_CELLS-1) {
            for (x in 0..NUM_X_CELLS-1) {
                if ((y==0 || y == 3) && graph[y][x] == 0) {
                    //Make new bin
                    val idx = x + y*NUM_X_CELLS
                    bins.add(Pair(idx, true))
                }
            }
        }

        while (!isClean(graph)) {

            val destVertex = getNextObject(graph)
            val prev = runDijkstras(graph, sourceVertex)
            var path = reconstructPath(prev, sourceVertex, destVertex)

            val xyPath: MutableList<Pair<Float, Float>> = ArrayList()
            for(i in path.indices){
                if(path[i] != -1){
                    val ind = vertex_index_to_ij_coordinates(path[i])
                    val ii: Int? = ind.get(1) as? Int
                    val jj: Int? = ind.get(2) as? Int
                    val xypair = ij_to_xy(ii!!,jj!!)
                    //Log.i("Dijk, i", ii.toString())
                    //Log.i("Dijk, j", jj.toString())
                    //Log.i("Dijk, pair", xypair.toString())
                    xyPath.add(xypair)

                }
            }
            Log.i("Dijk, path", xyPath.toString())

            runBlocking{
                val job = async {sendPath(xyPath)}
                job.await()
                Log.d("1:","first")
            }
            Log.d("2:","second")

            updateMap() //????????????????????????



            sourceVertex = destVertex
            val binVertex = findBin(graph, bins, sourceVertex)
            path = reconstructPath(prev, sourceVertex, binVertex)
            val xyPath2: MutableList<Pair<Float, Float>> = ArrayList()
            for(i in path.indices){
                if(path[i] != -1){
                    val ind = vertex_index_to_ij_coordinates(path[i])
                    val ii: Int? = ind.get(1) as? Int
                    val jj: Int? = ind.get(2) as? Int
                    val xypair = ij_to_xy(ii!!,jj!!)
                    //Log.i("Dijk, i", ii.toString())
                    //Log.i("Dijk, j", jj.toString())
                    //Log.i("Dijk, pair", xypair.toString())
                    xyPath2.add(xypair)

                }
            }
            Log.i("Dijk, path", xyPath.toString())

            runBlocking {
                val job = async {sendPath(xyPath2)}
                job.await()
                Log.d("1:","first")
            }
            Log.d("2:","second")

            sourceVertex = binVertex
        }
    }

    // Dijkstra's for path planning
    private fun setUpDijkstras(graph: Array<IntArray>): Array<IntArray>{
        for(j in 0..NUM_Y_CELLS-1) {
            for(i in 0..NUM_X_CELLS-1){
                graph[j][i] = 0
            }
        }
        return graph
    }

    private fun ij_to_xy(i: Int, j: Int): Pair<Float, Float> {
        val x: Float = ((i) * (0.72) / NUM_X_CELLS).toFloat()
        val y: Float = ((j) * (0.48) / NUM_Y_CELLS).toFloat()

        return Pair(x,y)
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

    private fun updateMap(){
        greenImageView = findViewById(R.id.greenObj1)
        greenImageView.setBackgroundResource(R.drawable.white)
        greenImageView = findViewById(R.id.greenObj2)
        greenImageView.setBackgroundResource(R.drawable.white)
        greenImageView = findViewById(R.id.greenObj3)
        greenImageView.setBackgroundResource(R.drawable.white)

        blueImageView = findViewById(R.id.blueObj1)
        blueImageView.setBackgroundResource(R.drawable.white)
        blueImageView = findViewById(R.id.blueObj2)
        blueImageView.setBackgroundResource(R.drawable.white)
        blueImageView = findViewById(R.id.blueObj3)
        blueImageView.setBackgroundResource(R.drawable.white)

        var redMap = ControlActivity.mapRed
        var greenMap = ControlActivity.mapGreen
        var blueMap = ControlActivity.mapBlue
        var imgWidth = ControlActivity.imgWidth

        repopulate(redMap, greenMap, blueMap, imgWidth, tempGraph)
    }

    private fun repopulate(cornerMap: ArrayList<Int>, objGreen: ArrayList<Int>, objBlue: ArrayList<Int>, imgWidth: Int, tempGraph: Array<IntArray>){
        val x1 = cornerMap[0].rem(imgWidth)
        val y1 = cornerMap[0] / imgWidth
        val x2 = cornerMap[1].rem(imgWidth)
        val y2 = cornerMap[1] / imgWidth
        val xP = minOf(x1, x2)
        val yP = minOf(y1, y2)
        val mapWidth = abs(x1 - x2)
        val mapHeight = abs(y1 - y2)
        g = 0
        b = 0

        if(!objGreen.isEmpty()){
            for(i in objGreen){
                val xGrn = i % imgWidth
                val yGrn = i / imgWidth
                val iIdx = (xGrn-xP).toFloat()/(mapWidth).toFloat() * 6
                val jIdx = (yGrn - yP).toFloat()/(mapHeight).toFloat() * 4
                Log.d("jIdx: ", jIdx.toString())
                Log.d("iIdx: ", iIdx.toString())
                if(tempGraph[floor(jIdx).roundToInt()][floor(iIdx).roundToInt()] == 3){
                    drawGreenObj(floor(iIdx).roundToInt(), floor(jIdx).roundToInt())
                }
            }
        }

        if(!objBlue.isEmpty()){
            for(i in objBlue){
                val xBlu = i % imgWidth
                val yBlu = i / imgWidth
                val iIdx = (xBlu-xP).toFloat()/(mapWidth).toFloat() * 6
                val jIdx = (yBlu - yP).toFloat()/(mapHeight).toFloat() * 4
                Log.d("jIdx: ", jIdx.toString())
                Log.d("iIdx: ", iIdx.toString())
                if(tempGraph[floor(jIdx).roundToInt()][floor(iIdx).roundToInt()] == 2){
                    drawBlueObj(floor(iIdx).roundToInt(), floor(jIdx).roundToInt())
                }
            }
        }
    }

    private fun populateMap(cornerMap: ArrayList<Int>, objGreen: ArrayList<Int>, objBlue: ArrayList<Int>, obstacles: ArrayList<Int>, imgWidth: Int){
        val x1 = cornerMap[0].rem(imgWidth)
        val y1 = cornerMap[0] / imgWidth
        val x2 = cornerMap[1].rem(imgWidth)
        val y2 = cornerMap[1] / imgWidth

        val xP = minOf(x1, x2)
        val yP = minOf(y1, y2)
        Log.d("x1: ", x1.toString())
        Log.d("y1:", y1.toString())
        Log.d("x2:", x2.toString())
        Log.d("y2:", y2.toString())
        Log.d("imgwidth: ", imgWidth.toString())
        Log.d("xp: ", xP.toString())
        Log.d("yp: ", yP.toString())
        val mapWidth = abs(x1 - x2)
        val mapHeight = abs(y1 - y2)

        if(!objGreen.isEmpty()){
            for(i in objGreen){
                val xGrn = i % imgWidth
                val yGrn = i / imgWidth
                val iIdx = (xGrn-xP).toFloat()/(mapWidth).toFloat() * 6
                val jIdx = (yGrn - yP).toFloat()/(mapHeight).toFloat() * 4
                Log.d("jIdx: ", jIdx.toString())
                Log.d("iIdx: ", iIdx.toString())
                tempGraph[floor(jIdx).roundToInt()][floor(iIdx).roundToInt()] = 3
                drawGreenObj(floor(iIdx).roundToInt(), floor(jIdx).roundToInt())
            }
        }

        if(!objBlue.isEmpty()){
            for(i in objBlue){
                val xBlu = i % imgWidth
                val yBlu = i / imgWidth
                val iIdx = (xBlu-xP).toFloat()/(mapWidth).toFloat() * 6
                val jIdx = (yBlu - yP).toFloat()/(mapHeight).toFloat() * 4
                Log.d("jIdx: ", jIdx.toString())
                Log.d("iIdx: ", iIdx.toString())
                tempGraph[floor(jIdx).roundToInt()][floor(iIdx).roundToInt()] = 2
                drawBlueObj(floor(iIdx).roundToInt(), floor(jIdx).roundToInt())
            }
        }

        if(!obstacles.isEmpty()) {
            for (i in obstacles) {
                val xOb = i % imgWidth
                val yOb = i / imgWidth
                val iIdx = (xOb-xP).toFloat()/(mapWidth).toFloat() * 6
                val jIdx = (yOb - yP).toFloat()/(mapHeight).toFloat() * 4
                Log.d("jIdx: ", jIdx.toString())
                Log.d("iIdx: ", iIdx.toString())
                tempGraph[floor(jIdx).roundToInt()][floor(iIdx).roundToInt()] = 1
                drawObstacle(floor(iIdx).roundToInt(), floor(jIdx).roundToInt())
            }
        }

        for(i in tempGraph.indices){
            for(j in tempGraph[i].indices) {
                Log.d("Graph index:", tempGraph[i][j].toString())
            }
        }
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
        if(!dOk!!) {
            return (255)
        }

        val sI: Int? = s.get(1) as? Int
        val dI: Int? = d.get(1) as? Int

        val sJ: Int? = s.get(2) as? Int
        val dJ: Int? = d.get(2) as? Int

        //Log.i("Dijk, si/j", Pair(sI,sJ).toString())
        //Log.i("Dijk, di/j", Pair(dI,dJ).toString())

        areNeighboring = (abs(sI!!-dI!!) + abs(sJ!!-dJ!!) <= 1)

        //Log.i("Dijk, neigh", areNeighboring.toString())
        //Log.i("Dijk, ret", graph[dJ][dI].toString())

        if(graph[dJ][dI] != 0){
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
            //Log.i("Dijk, min", minIndex.toString())
            if(minIndex < 0){
                break
            }
            currentVertex = minIndex
            q_cost.remove(minIndex)

            // Check neighbors
            for (i in 0..NUM_Y_CELLS*NUM_X_CELLS-1) {
                var alt: Int = -1
                travelCost = getTravelCost(graph, currentVertex, i)
                //Log.i("Dijk, travel", travelCost.toString())
                if (travelCost == 255) {
                    continue
                }
                alt = dist[currentVertex] + travelCost
                // Log.i("Dijk, alt",alt.toString())
                if(alt < dist[i]){
                    dist[i] = alt
                    //Log.i("Dijk, dist",dist.toString())
                    //Log.i("Dijk, i",i.toString())
                    //Log.i("Dijk, qcost",q_cost.toString())
                    prev[i] = currentVertex
                }
            }
        }
        //Log.i("Dijk, dist",dist.toString())
        //Log.i("Dijk, qcost",q_cost.toString())
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

    var busy = false
    var index = 0

    private fun sendPath(temp: MutableList<Pair<Float,Float>>) {
        Log.i("Dijk, send", "Now Sending!!")
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
        sendPath(temp)
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
                    counter += 1
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
        if(o == 0){
            obstacleImageView = findViewById(R.id.obstacle1)
        }else if(o == 1){
            obstacleImageView = findViewById(R.id.obstacle2)
        }else if(o == 2){
            obstacleImageView = findViewById(R.id.obstacle3)
        }else{
            obstacleImageView = findViewById(R.id.obstacle4)
        }
        val obstacleParams = obstacleImageView.layoutParams as ConstraintLayout.LayoutParams

        val iI: Int = i as Int
        val jI: Int = j as Int


        obstacleParams.horizontalBias = iI*0.199F
        obstacleParams.verticalBias = jI*0.335F
        obstacleImageView.setBackgroundResource(R.drawable.obstacle)
        obstacleImageView.layoutParams = obstacleParams
        o++
    }

    private fun drawBlueObj(i: Any, j: Any){
        if(b == 0){
            blueImageView = findViewById(R.id.blueObj1)
        }else if(b == 1){
            blueImageView = findViewById(R.id.blueObj2)
        }else{
            blueImageView = findViewById(R.id.blueObj3)
        }
        val blueParams = blueImageView.layoutParams as ConstraintLayout.LayoutParams

        val iI: Int = i as Int
        val jI: Int = j as Int


        blueParams.horizontalBias = iI*0.199F
        blueParams.verticalBias = jI*0.335F
        blueImageView.setBackgroundResource(R.drawable.blueobj)
        blueImageView.layoutParams = blueParams
        b++
    }

    private fun drawGreenObj(i: Any, j: Any){
        if(g == 0){
            greenImageView = findViewById(R.id.greenObj1)
        }else if(g == 1){
            greenImageView = findViewById(R.id.greenObj2)
        }else{
            greenImageView = findViewById(R.id.greenObj3)
        }
        val greenParams = greenImageView.layoutParams as ConstraintLayout.LayoutParams

        val iI: Int = i as Int
        val jI: Int = j as Int


        greenParams.horizontalBias = iI*0.199F
        greenParams.verticalBias = jI*0.335F
        greenImageView.setBackgroundResource(R.drawable.greenobj)
        greenImageView.layoutParams = greenParams
        g++
    }
}