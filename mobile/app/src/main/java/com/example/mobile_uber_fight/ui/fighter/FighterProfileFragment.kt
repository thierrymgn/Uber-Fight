package com.example.mobile_uber_fight.ui.fighter

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.DialogEditProfileBinding
import com.example.mobile_uber_fight.repositories.UserRepository
import com.example.mobile_uber_fight.databinding.FragmentFighterProfileBinding

class FighterProfileFragment : Fragment() {

    private var _binding: FragmentFighterProfileBinding? = null
    private val binding get() = _binding

    private val userRepository = UserRepository()

    private var currentUsername: String = ""
    private var currentEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFighterProfileBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding?.btnEditProfile?.setOnClickListener {
            showEditProfileDialog()
        }

        binding?.btnSettings?.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
    }

    private fun loadUserProfile() {
        userRepository.getCurrentUser(
            onSuccess = { user ->
                if (_binding == null) return@getCurrentUser
                if (user != null) {
                    currentUsername = user.username
                    currentEmail = user.email
                    binding?.tvFullName?.text = currentUsername.ifEmpty { "-" }
                    binding?.tvEmail?.text = user.email.ifEmpty { "-" }

                    val roleText = when (user.role.uppercase()) {
                        "CLIENT" -> "Statut: Client"
                        "FIGHTER" -> "Statut: Bagarreur"
                        else -> "Statut: ${user.role}"
                    }
                    binding?.tvStatus?.text = roleText
                } else {
                    binding?.tvFullName?.text = "-"
                    binding?.tvEmail?.text = "-"
                    binding?.tvStatus?.text = "Statut: -"
                }
            },
            onFailure = { e ->
                if (_binding == null) return@getCurrentUser
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.erreur_mise_a_jour_profil)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)

        dialogBinding.etUsername.setText(currentUsername)
        dialogBinding.etEmail.setText(currentEmail)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.enregistrer, null)
            .setNegativeButton(R.string.annuler, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val username = dialogBinding.etUsername.text.toString().trim()
                val email = dialogBinding.etEmail.text.toString().trim()

                if (username.isEmpty() || email.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.veuillez_remplir_tous_les_champs),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    dialogBinding.tilEmail.error = "Email invalide"
                    return@setOnClickListener
                }

                dialogBinding.tilEmail.error = null
                updateProfile(username, email, dialog)
            }
        }

        dialog.show()
    }

    private fun updateProfile(username: String, email: String, dialog: AlertDialog) {
        userRepository.updateUserProfile(
            username = username,
            email = email,
            onSuccess = {
                if (_binding == null) return@updateUserProfile
                currentUsername = username
                currentEmail = email
                binding?.tvFullName?.text = username
                binding?.tvEmail?.text = email
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profil_mis_a_jour),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            },
            onFailure = { e: Exception ->
                if (_binding == null) return@updateUserProfile
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.erreur_mise_a_jour_profil)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding?.tvFullName?.text = "-"
                binding?.tvEmail?.text = "-"
                binding?.tvStatus?.text = "Statut: -"
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
