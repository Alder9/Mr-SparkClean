package com.lit.dab.mr_sparkiclean

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color.*
import android.graphics.ColorSpace
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView

import kotlinx.android.synthetic.main.activity_main.*
import java.io.Console
import java.lang.Math.abs
import java.lang.Math.floor

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
        if(resultCode == RESULT_OK) {
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

    private fun processPhoto (method: String, image: Bitmap) {
        Log.d("Number of pixels: ", (image.height * image.width).toString())
        val pixels = IntArray(image.height * image.width)
        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight())
        /*
        for (pixel in pixels) {
            Log.d("Pixel: ", pixel.toString())
            val alpha = alpha(pixel)
            val red = red(pixel)
            val green = green(pixel)
            val blue = blue(pixel)
            Log.d("A: ", alpha.toString())
            Log.d("R: ", red.toString())
            Log.d("G: ", green.toString())
            Log.d("B: ", blue.toString())
        }
        */
        val filteredImage = IntArray(image.height*image.width)
        getCoordsByColor(95, parseColor("red"), image, "red", filteredImage)
    }

    private fun getCoordsByColor(threshold: Int, color: Int, image: Bitmap, filterby: String, coloredPixels: IntArray) {
        val width = image.width
        val height = image.height
        val pixels = IntArray(height * width)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.getHeight())
        for(i in pixels.indices) {
            when(filterby) {
                "red" ->
                {
                    if (abs(red(pixels[i]) - red(color)) < threshold) {
                        //Allow this pixel into our final image
                        coloredPixels[i] = pixels[i]
                    }

                }
                "green" ->
                {
                    if (abs(green(color) - green(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        coloredPixels[i] = pixels[i]
                    }
                }
                "blue" ->
                {
                    if (abs(blue(color) - blue(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        coloredPixels[i] = pixels[i]
                    }
                }
                "all" ->
                {
                    if (abs(red(color) - red(pixels[i])) < threshold &&  abs(green(color) - green(pixels[i])) < threshold && abs(blue(color) - blue(pixels[i])) < threshold) {
                        //Allow this pixel into our final image
                        coloredPixels[i] = pixels[i]
                    }
                }
            }
        }


        //Run blob detection...

        val blobs: ArrayList<IntArray> = ArrayList()

        val savedImage = coloredPixels.copyOf()
        for(i in coloredPixels.indices) {
            if (coloredPixels[i] != 0) {
                Log.d(":", "Adding blob...")
                val currentBlob = IntArray(width * height)
                currentBlob[i] = coloredPixels[i]
                val x = i % width
                val y = i / width
                getBlob(x, y, width, coloredPixels, currentBlob)
                blobs.add(currentBlob)
                var maxX = 0
                var maxY = 0
                var minX = image.width
                var minY = image.height
                for (i in currentBlob.indices) {
                    val x = i % width
                    val y = i / width
                    if (currentBlob[i] != 0 && x < minX) {
                        minX = x
                    }
                    if (currentBlob[i] != 0 && x > maxX) {
                        maxX = x
                    }
                    if (currentBlob[i] != 0 && y < minY) {
                        minY = y
                    }
                    if (currentBlob[i] != 0 && y > maxY) {
                        maxY = y
                    }
                    if (currentBlob[i] != 0) {
                        coloredPixels[i] = 0
                    }
                }
                Log.d("Maxx: ", maxX.toString())
                Log.d("Minx: ", minX.toString())
                Log.d("Maxy: ", maxY.toString())
                Log.d("Miny: ", minY.toString())
            }
        }
        val currentBlob = IntArray(width * height)
        getBlob(0, 0, width, coloredPixels, currentBlob)
        blobs.add(currentBlob)

        //Rest is working

        //Filter blobs

        //Get coordinates per blob
        for (blob in blobs) {
            var maxX = 0
            var maxY = 0
            var minX = image.width
            var minY = image.height
            for (i in blob.indices) {
                val x = i % width
                val y = i / width
                if (blob[i] != 0 && x < minX) {
                    minX = x
                }
                if (blob[i] != 0 && x > maxX) {
                    maxX = x
                }
                if (blob[i] != 0 && y < minY) {
                    minY = y
                }
                if (blob[i] != 0 && y > maxY) {
                    maxY = y
                }

            }
            /*Log.d("Maxx: ", maxX.toString())
            Log.d("Minx: ", minX.toString())
            Log.d("Maxy: ", maxY.toString())
            Log.d("Miny: ", minY.toString())*/

            for (i in (minX + 1)..(maxX - 1)) {
                coloredPixels[i + minY * width] = parseColor("blue")
                coloredPixels[i + maxY * width] = parseColor("blue")
                for (j in (minY + 1)..(maxY - 1)) {
                    coloredPixels[minX + j * width] = parseColor("blue")
                    coloredPixels[maxX + j * width] = parseColor("blue")
                }
            }
        }
        val displayedImage = createBitmap(savedImage, image.width, image.height, Bitmap.Config.ARGB_8888)
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageBitmap(displayedImage)
    }

    private fun getBlob(x: Int, y: Int, width: Int, pixels: IntArray, blob: IntArray) {
        val idx = x + y*width

        Log.d("X", x.toString())
        Log.d("Y", y.toString())
        Log.d(":", "---------")

        blob[idx] = pixels[idx]
        if (x-1 >= 0 && pixels[idx-1] != 0 && blob[idx-1] == 0) {getBlob(x - 1, y, width, pixels, blob)}
        if (y-1 >= 0 && pixels[idx-width] != 0 && blob[idx-width] == 0) {getBlob(x, y - 1, width, pixels, blob)}
        if (x+1 <= width && pixels[idx+1] != 0 && blob[idx+1] == 0) {getBlob(x + 1, y, width, pixels, blob)}
        if (y+1 <= pixels.size/width - 1 && pixels[idx+width] != 0 && blob[idx+width] == 0) {getBlob(x, y + 1, width, pixels, blob)}

    }

}
