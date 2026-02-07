package com.example.mobile_uber_fight.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mobile_uber_fight.databinding.FragmentRatingBottomSheetBinding
import com.example.mobile_uber_fight.repositories.UserRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RatingBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentRatingBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()

    private var targetUserId: String? = null
    private var fightId: String? = null
    private var onRatingSubmitted: (() -> Unit)? = null

    companion object {
        private const val ARG_TARGET_USER_ID = "target_user_id"
        private const val ARG_FIGHT_ID = "fight_id"

        fun newInstance(targetUserId: String, fightId: String, onRatingSubmitted: () -> Unit): RatingBottomSheetFragment {
            val fragment = RatingBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_TARGET_USER_ID, targetUserId)
                putString(ARG_FIGHT_ID, fightId)
            }
            fragment.arguments = args
            fragment.onRatingSubmitted = onRatingSubmitted
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        targetUserId = arguments?.getString(ARG_TARGET_USER_ID)
        fightId = arguments?.getString(ARG_FIGHT_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRatingBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            skipRating()
        }

        binding.btnSkipRating.setOnClickListener {
            skipRating()
        }

        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBar.rating
            val comment = binding.etComment.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(requireContext(), "Veuillez donner une note", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (targetUserId != null && fightId != null) {
                binding.btnSubmitRating.isEnabled = false
                userRepository.submitRating(
                    targetUserId!!,
                    fightId!!,
                    rating,
                    comment,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Merci pour votre avis !", Toast.LENGTH_SHORT).show()
                        onRatingSubmitted?.invoke()
                        dismiss()
                    },
                    onFailure = {
                        binding.btnSubmitRating.isEnabled = true
                        Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun skipRating() {
        onRatingSubmitted?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}