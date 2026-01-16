package com.example.mobile_uber_fight.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobile_uber_fight.activities.AuthentificationActivity
import com.example.mobile_uber_fight.databinding.FragmentSettingsBinding
import com.example.mobile_uber_fight.databinding.DialogChangePasswordBinding
import com.example.mobile_uber_fight.models.UserSettings
import com.example.mobile_uber_fight.repositories.AuthRepository
import com.example.mobile_uber_fight.repositories.SettingsRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsRepository = SettingsRepository()
    private val authRepository = AuthRepository()

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
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEditProfile.setOnClickListener {
            showToast("Modifier le profil (Bientôt disponible)")
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
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
            logoutUser()
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

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        
        val intent = Intent(requireContext(), AuthentificationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
        
        showToast("Déconnexion réussie")
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Mettre à jour", null)
            .setNegativeButton("Annuler") { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val oldPass = dialogBinding.etOldPassword.text.toString().trim()
            val newPass = dialogBinding.etNewPassword.text.toString().trim()
            val confirmPass = dialogBinding.etConfirmNewPassword.text.toString().trim()

            var isValid = true

            if (oldPass.isEmpty()) {
                dialogBinding.tilOldPassword.error = "Veuillez saisir votre mot de passe actuel"
                isValid = false
            } else {
                dialogBinding.tilOldPassword.error = null
            }

            if (newPass.length < 6) {
                dialogBinding.tilNewPassword.error = "Le mot de passe doit faire au moins 6 caractères"
                isValid = false
            } else {
                dialogBinding.tilNewPassword.error = null
            }

            if (newPass != confirmPass) {
                dialogBinding.tilConfirmNewPassword.error = "Les mots de passe ne correspondent pas"
                isValid = false
            } else {
                dialogBinding.tilConfirmNewPassword.error = null
            }

            if (isValid) {
                authRepository.updatePassword(oldPass, newPass)
                    .addOnSuccessListener {
                        showToast("Mot de passe mis à jour avec succès")
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        if (e.message?.contains("password", ignoreCase = true) == true) {
                            dialogBinding.tilOldPassword.error = "Mot de passe actuel incorrect"
                        } else {
                            showToast("Erreur: ${e.message}")
                        }
                    }
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
