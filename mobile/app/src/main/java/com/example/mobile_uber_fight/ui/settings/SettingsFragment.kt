package com.example.mobile_uber_fight.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobile_uber_fight.databinding.FragmentSettingsBinding
import com.example.mobile_uber_fight.models.UserSettings
import com.example.mobile_uber_fight.repositories.SettingsRepository
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsRepository = SettingsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        settingsRepository.getUserSettings().addOnSuccessListener { settings ->
            settings?.let {
                binding.switchNotifications.isChecked = it.notificationsEnabled
                binding.switchDarkMode.isChecked = it.darkModeEnabled
            }
        }.addOnFailureListener { e ->
            Log.e("SettingsFragment", "Error loading settings", e)
            showToast("Erreur lors du chargement des paramètres: ${e.message}")
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            showToast("Modifier le profil (Bientôt disponible)")
        }

        binding.btnChangePassword.setOnClickListener {
            showToast("Changer le mot de passe (Bientôt disponible)")
        }

        binding.btnManagePayment.setOnClickListener {
            showToast("Gérer les moyens de paiement (Bientôt disponible)")
        }

        binding.btnHelpCenter.setOnClickListener {
            showToast("Centre d'aide")
        }

        binding.btnTerms.setOnClickListener {
            showToast("Conditions d'utilisation")
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            showToast("Déconnexion réussie")
            activity?.finish()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.updateNotificationPreference(isChecked)
                .addOnFailureListener { e -> 
                    Log.e("SettingsFragment", "Error updating notifications", e)
                    showToast("Erreur de mise à jour: ${e.message}") 
                }
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.updateDarkModePreference(isChecked)
                .addOnFailureListener { e -> 
                    Log.e("SettingsFragment", "Error updating dark mode", e)
                    showToast("Erreur de mise à jour: ${e.message}") 
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
