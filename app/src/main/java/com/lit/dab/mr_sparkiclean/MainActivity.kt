package com.lit.dab.mr_sparkiclean

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color.*
import android.graphics.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView

import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.abs

class MainActivity : AppCompatActivity() {
    private val REQUEST_CLEAN = 1
    private val REQUEST_DIRTY = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
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
            when (requestCode) {
                REQUEST_CLEAN -> {
                    val bitmapImg = data?.extras?.get("img") as Bitmap
                    processPhoto("Basic", bitmapImg)
                }
                REQUEST_DIRTY -> {
                    val bitmapImg = data?.extras?.get("img") as Bitmap
                    processPhoto("Basic", bitmapImg)
                }
            }
        }
    }

    private fun processPhoto(method: String, image: Bitmap) {
        val pixels = IntArray(image.height * image.width)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)

        getBlobCoordsByColor(95, parseColor("red"), image, "all")
    }

    private fun getBlobCoordsByColor(threshold: Int, color: Int, image: Bitmap, filterby: String) {
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
                    if (abs(red(pixels[i]) - red(color)) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }

                }
                "green" -> {
                    if (abs(green(color) - green(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }
                }
                "blue" -> {
                    if (abs(blue(color) - blue(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        filteredPixels[i] = pixels[i]
                    }
                }
                "all" -> {
                    if (abs(red(color) - red(pixels[i])) < threshold && abs(green(color) - green(pixels[i])) < threshold && abs(blue(color) - blue(pixels[i])) < threshold) {
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
        val thresh = 30
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
                if ((abs(minX2 - maxX) < thresh || abs(maxX2 - minX) < thresh || abs(minX2 - minX) < thresh || abs(maxX2 - maxX) < thresh) && (abs(minY2 - maxY) < thresh || abs(maxY2 - minY) < thresh || abs(minY2 - minY) < thresh || abs(maxY2 - maxY) < thresh)) {
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


        //Draw blobs
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

            for (i in (minX + 1)..(maxX - 1)) {
                imageCopy[i + minY * width] = parseColor("blue")
                imageCopy[i + maxY * width] = parseColor("blue")
                for (j in (minY + 1)..(maxY - 1)) {
                    imageCopy[minX + j * width] = parseColor("blue")
                    imageCopy[maxX + j * width] = parseColor("blue")
                }
            }
        }

        val displayedImage = createBitmap(imageCopy, image.width, image.height, Bitmap.Config.ARGB_8888)
        val matrix = Matrix()
        matrix.postRotate(90.toFloat())
        val rotatedImage = createBitmap(displayedImage, 0, 0, image.width, image.height, matrix, false)
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageBitmap(rotatedImage)
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
            Log.d("--------","-------------")
            Log.d("b1",pair.first.toString())
            Log.d("b2",pair.second.toString())
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
                    if(!newBlob.contains(i)) {
                        newBlob.add(i)
                    }
                }
                for (i in blobs[b2Index]) {
                    if(!newBlob.contains(i)) {
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

            val thresh = 30
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
                    if ((abs(minX2 - maxX) < thresh || abs(maxX2 - minX) < thresh || abs(minX2 - minX) < thresh || abs(maxX2 - maxX) < thresh) && (abs(minY2 - maxY) < thresh || abs(maxY2 - minY) < thresh || abs(minY2 - minY) < thresh || abs(maxY2 - maxY) < thresh)) {
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

