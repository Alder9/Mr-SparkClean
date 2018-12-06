package com.lit.dab.mr_sparkiclean

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class ControlActivity: AppCompatActivity(){


    private val REQUEST_CLEAN = 1
    private val REQUEST_DIRTY = 2

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

    fun takeCleanPhoto(view: View) {
        val intent = Intent(this, camera::class.java)
        startActivityForResult(intent, REQUEST_CLEAN)
    }

    fun takeDirtyPhoto(view: View) {
        val intent = Intent(this, camera::class.java)
        startActivityForResult(intent, REQUEST_DIRTY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val blobsByColor: ArrayList<ArrayList<Int>> = ArrayList()
            when (requestCode) {
                REQUEST_CLEAN -> {
                    val bitmapImg = data?.extras?.get("img") as Bitmap
                    processPhoto("Clean", bitmapImg)
                }
                REQUEST_DIRTY -> {
                    val bitmapImg = data?.extras?.get("img") as Bitmap
                    processPhoto("Dirty", bitmapImg)
                }
            }
        }
    }

    private fun processPhoto(method: String, image: Bitmap) {
        val pixels = IntArray(image.height * image.width)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        val blobIndices = getBlobCoordsByColor(110, Color.parseColor("red"), image, "all", method)
        for (i in blobIndices) {
            Log.d("Index: ", i.toString())
        }
    }

    private fun getBlobCoordsByColor(threshold: Int, color: Int, image: Bitmap, filterby: String, method: String): ArrayList<Int> {
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        val filteredPixels = IntArray(width * height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        val imageCopy = pixels.copyOf()

        //Filter image by color
        for (i in pixels.indices) {
            when (filterby) {
                "red" -> {
                    if (Math.abs(Color.red(pixels[i]) - Color.red(color)) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }

                }
                "green" -> {
                    if (Math.abs(Color.green(color) - Color.green(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }
                }
                "blue" -> {
                    if (Math.abs(Color.blue(color) - Color.blue(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }
                }
                "all" -> {
                    if (Math.abs(Color.red(color) - Color.red(pixels[i])) < threshold && Math.abs(Color.green(color) - Color.green(pixels[i])) < threshold && Math.abs(Color.blue(color) - Color.blue(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }
                }
            }
        }

        //Run blob detection...

        val blobs: ArrayList<ArrayList<Int>> = ArrayList()

        for (i in filteredPixels.indices) {
            if (filteredPixels[i] != 0) {
                Log.d(":", "Adding blob...")
                val currentBlob = ArrayList<Int>()
                currentBlob.add(i)
                val x = i % width
                val y = i / width
                getBlob(x, y, width, filteredPixels, currentBlob)
                blobs.add(currentBlob)
                for (idx in currentBlob) {
                    filteredPixels[idx] = 0
                }
            }
        }

        //Filter blobs
        //We know that all blobs will be roughly square, so filter by total blob area
        val remove = ArrayList<ArrayList<Int>>()
        for (blob in blobs) {
            var maxX = 0
            var maxY = 0
            var minX = image.width
            var minY = image.height
            for (i in blob) {
                val x = i % width
                val y = i / width
                if (x < minX) {
                    minX = x
                }
                if (x > maxX) {
                    maxX = x
                }
                if (y < minY) {
                    minY = y
                }
                if (y > maxY) {
                    maxY = y
                }
            }
            val area = (maxX - minX) * (maxY - minY)
            //Maximum allowable indicies in blob is 290... account for some rough edges or non-square shape
            if (area < 50) {
                remove.add(blob)
            }
        }
        blobs.removeAll(remove)

        //Merge blobs

        val merge: ArrayList<Pair<Int, Int>> = ArrayList()
        val thresh = 15
        for (i in blobs.indices) {
            val blob1 = blobs[i]
            var maxX = 0
            var maxY = 0
            var minX = image.width
            var minY = image.height
            for (i in blob1) {
                val x = i % width
                val y = i / width
                if (x < minX) {
                    minX = x
                }
                if (x > maxX) {
                    maxX = x
                }
                if (y < minY) {
                    minY = y
                }
                if (y > maxY) {
                    maxY = y
                }
            }
            for (j in blobs.indices) {
                val blob2 = blobs[j]
                var maxX2 = 0
                var maxY2 = 0
                var minX2 = image.width
                var minY2 = image.height
                for (i in blob2) {
                    val x = i % width
                    val y = i / width
                    if (x < minX2) {
                        minX2 = x
                    }
                    if (x > maxX2) {
                        maxX2 = x
                    }
                    if (y < minY2) {
                        minY2 = y
                    }
                    if (y > maxY2) {
                        maxY2 = y
                    }
                }
                if ((Math.abs(minX2 - maxX) < thresh || Math.abs(maxX2 - minX) < thresh || Math.abs(minX2 - minX) < thresh || Math.abs(maxX2 - maxX) < thresh) && (Math.abs(minY2 - maxY) < thresh || Math.abs(maxY2 - minY) < thresh || Math.abs(minY2 - minY) < thresh || Math.abs(maxY2 - maxY) < thresh)) {
                    var add = true
                    for (pair in merge) {
                        if (pair.first == i || pair.second == i || pair.first == j || pair.second == j) {
                            add = false
                        }
                    }
                    if (add && i != j) {
                        merge.add((Pair(i, j)))
                    }
                }
            }
        }

        //merge blobs
        mergeBlobs(merge, blobs, width, height)


        //Draw blobs, store indices
        val blobIndices: ArrayList<Int> = ArrayList()
        for (blob in blobs) {
            var maxX = 0
            var maxY = 0
            var minX = image.width
            var minY = image.height
            for (i in blob) {
                val x = i % width
                val y = i / width
                if (x < minX) {
                    minX = x
                }
                if (x > maxX) {
                    maxX = x
                }
                if (y < minY) {
                    minY = y
                }
                if (y > maxY) {
                    maxY = y
                }

            }
            val xpos = (maxX+minX)/2
            val ypos = (maxX+minX)/2
            val idx = xpos + ypos*width
            blobIndices.add(idx)
            for (i in (minX + 1)..(maxX - 1)) {
                imageCopy[i + minY * width] = Color.parseColor("blue")
                imageCopy[i + maxY * width] = Color.parseColor("blue")
                for (j in (minY + 1)..(maxY - 1)) {
                    imageCopy[minX + j * width] = Color.parseColor("blue")
                    imageCopy[maxX + j * width] = Color.parseColor("blue")
                }
            }
        }

        //Un-comment to display image

        val displayedImage = Bitmap.createBitmap(imageCopy, image.width, image.height, Bitmap.Config.ARGB_8888)
        val matrix = Matrix()
        matrix.postRotate(90.toFloat())
        val rotatedImage = Bitmap.createBitmap(displayedImage, 0, 0, image.width, image.height, matrix, false)
        val imageView = findViewById<ImageView>(R.id.photo)
        imageView.setImageBitmap(rotatedImage)

        return blobIndices
    }

    private fun getBlob(x: Int, y: Int, width: Int, pixels: IntArray, blob: ArrayList<Int>) {
        val idx = x + y * width

        Log.d("X", x.toString())
        Log.d("Y", y.toString())
        Log.d(":", "---------")

        if (blob.size > 290) {
            Log.d("ERR", "Blob size too large, stopping recursion...")
            return
        }

        blob.add(idx)
        if (x - 1 >= 0 && pixels[idx - 1] != 0 && !blob.contains(idx - 1)) {
            getBlob(x - 1, y, width, pixels, blob)
        }
        if (y - 1 >= 0 && pixels[idx - width] != 0 && !blob.contains(idx - width)) {
            getBlob(x, y - 1, width, pixels, blob)
        }
        if (x + 1 < width && pixels[idx + 1] != 0 && !blob.contains(idx + 1)) {
            getBlob(x + 1, y, width, pixels, blob)
        }
        if (y + 1 < pixels.size / width - 1 && pixels[idx + width] != 0 && !blob.contains(idx + width)) {
            getBlob(x, y + 1, width, pixels, blob)
        }
    }

    private fun mergeBlobs(merge: ArrayList<Pair<Int, Int>>, blobs: ArrayList<ArrayList<Int>>, width: Int, height: Int) {
        Log.d("length of merge: ", merge.size.toString())
        for (pair in merge) {
            Log.d("--------", "-------------")
            Log.d("b1", pair.first.toString())
            Log.d("b2", pair.second.toString())
        }
        if (merge.size == 0) {
            return
        } else {
            val newBlobs = ArrayList<ArrayList<Int>>()
            val remove = ArrayList<ArrayList<Int>>()
            for (pair in merge) {
                val newBlob = ArrayList<Int>()
                val b1Index = pair.first
                val b2Index = pair.second

                for (i in blobs[b1Index]) {
                    if (!newBlob.contains(i)) {
                        newBlob.add(i)
                    }
                }
                for (i in blobs[b2Index]) {
                    if (!newBlob.contains(i)) {
                        newBlob.add(i)
                    }
                }

                remove.add(blobs[b1Index])
                remove.add(blobs[b2Index])
                newBlobs.add(newBlob)
            }

            Log.d("length of blobs, b4: ", blobs.size.toString())

            blobs.removeAll(remove)
            blobs.addAll(newBlobs)

            Log.d("length of blobs, af: ", blobs.size.toString())

            merge.clear()

            val thresh = 15
            for (i in blobs.indices) {
                val blob1 = blobs[i]
                var maxX = 0
                var maxY = 0
                var minX = width
                var minY = height
                for (i in blob1) {
                    val x = i % width
                    val y = i / width
                    if (x < minX) {
                        minX = x
                    }
                    if (x > maxX) {
                        maxX = x
                    }
                    if (y < minY) {
                        minY = y
                    }
                    if (y > maxY) {
                        maxY = y
                    }
                }
                for (j in blobs.indices) {
                    val blob2 = blobs[j]
                    var maxX2 = 0
                    var maxY2 = 0
                    var minX2 = width
                    var minY2 = height
                    for (i in blob2) {
                        val x = i % width
                        val y = i / width
                        if (x < minX2) {
                            minX2 = x
                        }
                        if (x > maxX2) {
                            maxX2 = x
                        }
                        if (y < minY2) {
                            minY2 = y
                        }
                        if (y > maxY2) {
                            maxY2 = y
                        }
                    }
                    if ((Math.abs(minX2 - maxX) < thresh || Math.abs(maxX2 - minX) < thresh || Math.abs(minX2 - minX) < thresh || Math.abs(maxX2 - maxX) < thresh) && (Math.abs(minY2 - maxY) < thresh || Math.abs(maxY2 - minY) < thresh || Math.abs(minY2 - minY) < thresh || Math.abs(maxY2 - maxY) < thresh)) {
                        var add = true
                        for (pair in merge) {
                            if (pair.first == i || pair.second == i || pair.first == j || pair.second == j) {
                                add = false
                            }
                        }
                        if (add && i != j) {
                            merge.add((Pair(i, j)))
                        }
                    }
                }
            }

            mergeBlobs(merge, blobs, width, height)
        }
    }

}
