package com.xpj.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xpj.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDispatch.setText(Prefs.getDispatch(this))

        binding.btnOpen.setOnClickListener {
            val id = binding.etDispatch.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "请输入下发id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.saveDispatch(this, id)
            startActivity(Intent(this, TaskWebActivity::class.java).putExtra("dispatch_id", id))
        }
    }
}
