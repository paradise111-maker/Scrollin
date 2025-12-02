package com.example.scrollin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PerksActivity : AppCompatActivity() {

    private lateinit var rvPerks: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perks)

        rvPerks = findViewById(R.id.rvPerks)
        rvPerks.layoutManager = GridLayoutManager(this, 2)

        // TODO: Create and set adapter
    }
}