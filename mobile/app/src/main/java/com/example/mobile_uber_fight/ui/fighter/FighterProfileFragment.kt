package com.example.mobile_uber_fight.ui.fighter

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.DialogEditProfileBinding
import com.example.mobile_uber_fight.databinding.FragmentFighterProfileBinding
import com.example.mobile_uber_fight.adapter.ReviewAdapter
import com.example.mobile_uber_fight.logger.GrafanaMetrics
import com.example.mobile_uber_fight.repositories.UserRepository
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth

class FighterProfileFragment : Fragment() {

    private var _binding: FragmentFighterProfileBinding? = null
    private val binding get() = _binding

    private val userRepository = UserRepository()

    private var currentUsername: String = ""
    private var currentEmail: String = ""
    private val reviewAdapter = ReviewAdapter(emptyList())

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

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

        binding?.rvReviews?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvReviews?.adapter = reviewAdapter

        loadUserProfile()
        loadReviews()

        binding?.ivEditPhoto?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding?.ivProfile?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding?.btnEditProfile?.setOnClickListener {
            showEditProfileDialog()
        }

        binding?.btnSettings?.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        GrafanaMetrics.screenView("fighter_profil")
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

                    binding?.ratingBar?.rating = user.rating.toFloat()
                    if (user.ratingCount > 0) {
                        binding?.tvRating?.text = String.format("%.1f (%d avis)", user.rating, user.ratingCount)
                    } else {
                        binding?.tvRating?.text = "Aucun avis"
                    }

                    if (user.photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(user.photoUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding!!.ivProfile)
                    }
                }
            },
            onFailure = { e ->
                if (_binding == null) return@getCurrentUser
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadReviews() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userRepository.getReviewsForUser(uid,
            onSuccess = { reviews ->
                if (_binding == null) return@getReviewsForUser
                if (reviews.isEmpty()) {
                    binding?.tvNoReviews?.visibility = View.VISIBLE
                    binding?.rvReviews?.visibility = View.GONE
                } else {
                    binding?.tvNoReviews?.visibility = View.GONE
                    binding?.rvReviews?.visibility = View.VISIBLE
                    reviewAdapter.updateList(reviews)
                }
            },
            onFailure = {
                if (_binding == null) return@getReviewsForUser
                binding?.tvNoReviews?.visibility = View.VISIBLE
                binding?.rvReviews?.visibility = View.GONE
            }
        )
    }

    private fun uploadProfileImage(uri: Uri) {
        Toast.makeText(requireContext(), "Téléchargement en cours...", Toast.LENGTH_SHORT).show()
        
        userRepository.uploadProfilePicture(uri,
            onSuccess = { downloadUrl ->
                if (_binding == null) return@uploadProfilePicture
                Toast.makeText(requireContext(), "Photo mise à jour !", Toast.LENGTH_SHORT).show()
                
                Glide.with(this)
                    .load(downloadUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(binding!!.ivProfile)
            },
            onFailure = { e ->
                if (_binding == null) return@uploadProfilePicture
                Toast.makeText(requireContext(), "Échec: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            },
            onFailure = { e ->
                if (_binding == null) return@updateUserProfile
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
