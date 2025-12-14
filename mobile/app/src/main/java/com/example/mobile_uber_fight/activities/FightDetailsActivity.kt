package com.example.mobile_uber_fight.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityFightDetailsBinding
import com.example.mobile_uber_fight.models.Fight

class FightDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFightDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFightDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Note: Pour une app de production, il faudrait passer l'ID et re-fetcher les données
        // pour s'assurer qu'elles sont à jour. Pour ce cas, passer l'objet est acceptable.
        val fight = intent.getParcelableExtra<Fight>("FIGHT_EXTRA")

        if (fight == null) {
            Toast.makeText(this, "Erreur: Impossible de charger les détails du combat.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.fight = fight
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnAction.setOnClickListener {
            Toast.makeText(this, "Action à implémenter !", Toast.LENGTH_SHORT).show()
        }
    }
}
