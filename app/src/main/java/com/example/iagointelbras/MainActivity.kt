package com.example.iagointelbras

import android.app.Activity
import android.os.Bundle
import android.view.View

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this).apply { setBackgroundColor(0xFFFFFFFF.toInt()) })
    }
}
