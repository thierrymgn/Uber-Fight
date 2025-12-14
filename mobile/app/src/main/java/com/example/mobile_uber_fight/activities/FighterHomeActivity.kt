package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobile_uber_fight.adapter.FightAdapter
import com.example.mobile_uber_fight.databinding.ActivityFighterHomeBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository

class FighterHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFighterHomeBinding
    private lateinit var fightAdapter: FightAdapter
    private val fightRepository = FightRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFighterHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenForActiveFight()
    }

    private fun listenForActiveFight() {
        fightRepository.listenToMyActiveFight(
            onFightFound = {
                if (it != null) {
                    navigateToFightDetails(it)
                } else {
                    setupRecyclerView()
                    listenForPendingFights()
                }
            },
            onFailure = {
                Toast.makeText(this, "Erreur: ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupRecyclerView() {
        fightAdapter = FightAdapter(emptyList()) { fight ->
            acceptFightOffer(fight)
        }
        binding.rvFights.apply {
            adapter = fightAdapter
            layoutManager = LinearLayoutManager(this@FighterHomeActivity)
        }
    }

    private fun listenForPendingFights() {
        setLoadingState(true)
        fightRepository.listenToPendingFights(
            onUpdate = {
                setLoadingState(false)
                updateUi(it)
            },
            onFailure = { exception ->
                setLoadingState(false)
                Toast.makeText(this, "Erreur: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateUi(fights: List<Fight>) {
        if (fights.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvFights.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvFights.visibility = View.VISIBLE
            fightAdapter.updateFights(fights)
        }
    }

    private fun acceptFightOffer(fight: Fight) {
        fightRepository.acceptFight(fight.id,
            onSuccess = {
                Toast.makeText(this, "Mission acceptée !", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                Toast.makeText(this, "Échec de l'acceptation: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun navigateToFightDetails(fight: Fight) {
        val intent = Intent(this, FightDetailsActivity::class.java).apply {
            putExtra("FIGHT_EXTRA", fight)
        }
        startActivity(intent)
        finish()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
