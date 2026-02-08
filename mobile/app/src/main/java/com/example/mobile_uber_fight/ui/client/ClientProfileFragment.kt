package com.example.mobile_uber_fight.ui.client

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
import com.example.mobile_uber_fight.databinding.FragmentClientProfileBinding
import com.example.mobile_uber_fight.repositories.UserRepository

class ClientProfileFragment : Fragment() {

    private var _binding: FragmentClientProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()

    private var currentFirstName: String = ""
    private var currentLastName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadUserProfile()

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_clientProfileFragment_to_settingsFragment)
        }
    }

    private fun loadUserProfile() {
        userRepository.getCurrentUser(
            onSuccess = { user ->
                if (user != null) {
                    currentFirstName = user.firstName
                    currentLastName = user.lastName
                    updateUI()
                }
            },
            onFailure = { e ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.erreur_mise_a_jour_profil),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun updateUI() {
        val fullName = if (currentFirstName.isNotEmpty() || currentLastName.isNotEmpty()) {
            "$currentFirstName $currentLastName".trim()
        } else {
            "-"
        }
        binding.tvFullName.text = fullName
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)

        // Pré-remplir avec les valeurs actuelles
        dialogBinding.etFirstName.setText(currentFirstName)
        dialogBinding.etLastName.setText(currentLastName)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.enregistrer, null)
            .setNegativeButton(R.string.annuler, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val firstName = dialogBinding.etFirstName.text.toString().trim()
                val lastName = dialogBinding.etLastName.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.veuillez_remplir_tous_les_champs),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                updateProfile(firstName, lastName, dialog)
            }
        }

        dialog.show()
    }

    private fun updateProfile(firstName: String, lastName: String, dialog: AlertDialog) {
        userRepository.updateUserProfile(
            firstName = firstName,
            lastName = lastName,
            onSuccess = {
                currentFirstName = firstName
                currentLastName = lastName
                updateUI()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profil_mis_a_jour),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            },
            onFailure = { e ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.erreur_mise_a_jour_profil),
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
