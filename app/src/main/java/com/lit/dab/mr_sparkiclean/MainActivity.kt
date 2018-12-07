package com.lit.dab.mr_sparkiclean

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.widget.Button


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        supportActionBar?.setTitle(null)
        val sparkiLogo: Button = findViewById(R.id.sparkibutton)
        sparkiLogo.setOnClickListener {
            val intent = Intent (this, SelectDeviceActivity::class.java)
            startActivity(intent)
        }
    }
}