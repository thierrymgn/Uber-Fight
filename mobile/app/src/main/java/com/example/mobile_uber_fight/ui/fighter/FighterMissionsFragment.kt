package com.example.mobile_uber_fight.ui.fighter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobile_uber_fight.databinding.FragmentFighterMissionsBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository

class FighterMissionsFragment : Fragment() {

    private var _binding: FragmentFighterMissionsBinding? = null
    private val binding get() = _binding!!

    private val fightRepository = FightRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFighterMissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenForActiveFight()
    }

    private fun listenForActiveFight() {
        fightRepository.listenToMyActiveFight(
            onFightFound = {
                if (isAdded) { // Vérifie que le fragment est toujours là
                    updateUi(it)
                }
            },
            onFailure = {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erreur: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun updateUi(activeFight: Fight?) {
        if (activeFight != null) {
            binding.layoutMissionDetails.visibility = View.VISIBLE
            binding.tvNoActiveMission.visibility = View.GONE

            binding.tvAddress.text = activeFight.address
            binding.tvFightType.text = activeFight.fightType

            binding.btnAction.setOnClickListener {
                // Logique à venir : changer le statut du combat, etc.
                Toast.makeText(requireContext(), "Action pour le combat ${activeFight.id}", Toast.LENGTH_SHORT).show()
            }

        } else {
            binding.layoutMissionDetails.visibility = View.GONE
            binding.tvNoActiveMission.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
