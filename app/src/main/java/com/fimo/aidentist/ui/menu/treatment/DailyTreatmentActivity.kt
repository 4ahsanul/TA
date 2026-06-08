package com.fimo.aidentist.ui.menu.treatment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fimo.aidentist.databinding.ActivityDailyTreatmentBinding

class DailyTreatmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyTreatmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyTreatmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
}
