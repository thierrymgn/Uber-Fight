package com.example.mobile_uber_fight.ui.fighter

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.DialogEditProfileBinding
import com.example.mobile_uber_fight.databinding.FragmentFighterProfileBinding
import com.example.mobile_uber_fight.repositories.UserRepository

class FighterProfileFragment : Fragment() {

    private var _binding: FragmentFighterProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()

    private var currentUsername: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFighterProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_fighterProfileFragment_to_settingsFragment)
        }
    }

    private fun loadUserProfile() {
        userRepository.getCurrentUser(
            onSuccess = { user ->
                if (user != null) {
                    currentUsername = user.username
                    binding.tvFullName.text = currentUsername.ifEmpty {
                        "-"
                    }
                    
                    binding.tvEmail.text = user.email.ifEmpty {
                        "-"
                    }

                    val roleText = when (user.role.uppercase()) {
                        "CLIENT" -> "Statut: Client"
                        "FIGHTER" -> "Statut: Bagarreur"
                        else -> "Statut: ${user.role}"
                    }
                    binding.tvStatus.text = roleText
                } else {
                    binding.tvFullName.text = "-"
                    binding.tvEmail.text = "-"
                    binding.tvStatus.text = "Statut: -"
                }
            },
            onFailure = { e ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.erreur_mise_a_jour_profil),
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvFullName.text = "-"
                binding.tvEmail.text = "-"
                binding.tvStatus.text = "Statut: -"
            }
        )
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)

        dialogBinding.etUsername.setText(currentUsername)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.enregistrer, null)
            .setNegativeButton(R.string.annuler, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val username = dialogBinding.etUsername.text.toString().trim()

                if (username.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.veuillez_remplir_tous_les_champs),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                updateProfile(username, dialog)
            }
        }

        dialog.show()
    }

    private fun updateProfile(username: String, dialog: AlertDialog) {
        userRepository.updateUserProfile(
            username = username,
            onSuccess = {
                currentUsername = username
                binding.tvFullName.text = username
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profil_mis_a_jour),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            },
            onFailure = { e: Exception ->
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.erreur_mise_a_jour_profil)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
