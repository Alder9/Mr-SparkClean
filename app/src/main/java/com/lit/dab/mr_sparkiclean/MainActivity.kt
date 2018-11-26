package com.lit.dab.mr_sparkiclean

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color.*
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
                    val imageView = findViewById<ImageView>(R.id.imageViewLeft)
                    processPhoto("Basic", bitmapImg)
                    Log.d(" ", "Captured Photo clean!")
                    imageView.setImageBitmap(bitmapImg)
                }
                REQUEST_DIRTY -> {
                    val bitmapImg = data?.extras?.get("img") as Bitmap
                    val imageView = findViewById<ImageView>(R.id.imageViewRight)
                    processPhoto("Basic", bitmapImg)
                    imageView.setImageBitmap(bitmapImg)
                }
            }
        }
    }

    private fun processPhoto (method: String, image: Bitmap) {
        Log.d("Number of pixels: ", (image.height * image.width).toString())
        val pixels = IntArray(image.height * image.width)
        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight())
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
    }

}
