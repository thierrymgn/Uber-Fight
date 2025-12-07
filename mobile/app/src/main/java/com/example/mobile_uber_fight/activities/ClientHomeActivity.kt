package com.example.mobile_uber_fight.activities

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityClientHomeBinding
import com.example.mobile_uber_fight.repositories.FightRepository

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientHomeBinding
    private val fightRepository = FightRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnOrderFight.setOnClickListener {
            handleOrderFightClick()
        }
    }

    private fun handleOrderFightClick() {
        val address = binding.etAddress.text.toString().trim()

        if (address.isEmpty()) {
            binding.tilAddress.error = "L'adresse est requise pour commander un duel"
            return
        }
        binding.tilAddress.error = null // Clear error

        setLoadingState(true)

        val selectedRadioButtonId = binding.rgFightType.checkedRadioButtonId
        val fightType = findViewById<RadioButton>(selectedRadioButtonId).text.toString()

        // Hardcoded location for now (Paris, France)
        val latitude = 48.8566
        val longitude = 2.3522

        fightRepository.createFightRequest(
            address = address,
            lat = latitude,
            lng = longitude,
            fightType = fightType,
            onSuccess = {
                setLoadingState(false)
                Toast.makeText(this, "Votre commande de duel a été envoyée !", Toast.LENGTH_LONG).show()
                binding.etAddress.text?.clear()
            },
            onFailure = { exception ->
                setLoadingState(false)
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnOrderFight.isEnabled = !isLoading
    }
}
