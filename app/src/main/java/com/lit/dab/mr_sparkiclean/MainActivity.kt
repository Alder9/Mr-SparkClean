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

        getCoordsByColor(95, parseColor("red"), image, "red")
    }

    private fun getCoordsByColor(threshold: Int, color: Int, image: Bitmap, filterby: String) {
        val pixels = IntArray(image.height * image.width)
        val coloredPixels = IntArray(image.height * image.width)
        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight())
        for(i in pixels.indices) {
            when(filterby) {
                "red" ->
                {
                    if (abs(red(pixels[i]) - red(color)) > threshold) {
                        //Allow this pixel into our final image
                        coloredPixels[i] = pixels[i]
                    }

                }
                /*"green" ->
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
                }*/
            }
        }
        val width = image.width
        var minX = 0
        var minY = 0
        var maxX = image.width
        var maxY = image.height
        for (i in coloredPixels.indices) {
            val x = i%width
            val y = i/width
            if (coloredPixels[i] != 0 && x < minX) {
                minX = x
            }
            if (coloredPixels[i] != 0 && x > maxX) {
                maxX = x
            }
            if (coloredPixels[i] != 0 && y < minY) {
                minY = y
            }
            if (coloredPixels[i] != 0 && y > maxY) {
                maxY = y
            }

        }
        Log.d("Maxx: ", maxX.toString())
        Log.d("Minx: ", minX.toString())
        Log.d("Maxy: ", maxY.toString())
        Log.d("Miny: ", minY.toString())

        /*for (i in minX+1..maxX-1) {
            for(j in minY+1..maxY-1) {
                coloredPixels[i + j * width] = parseColor("blue")
            }
        }*/

        val coloredBitmap = createBitmap(coloredPixels, image.width, image.height, Bitmap.Config.ARGB_8888)
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageBitmap(coloredBitmap)
    }

}
