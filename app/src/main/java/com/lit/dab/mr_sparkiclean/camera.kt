package com.lit.dab.mr_sparkiclean

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore

class camera : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        dispatchTakePictureIntent(1)

    }

    private fun dispatchTakePictureIntent(requestCode: Int)
    {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //val valid: Boolean = takePicture -> takePicture.resolveActivity(packageManager)?
        startActivityForResult(takePicture, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1 && resultCode == RESULT_OK)
        {
            val bitmapImage = data?.extras?.get("data") as Bitmap
            val resultIntent = Intent()
            resultIntent.putExtra("img", bitmapImage)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

}
