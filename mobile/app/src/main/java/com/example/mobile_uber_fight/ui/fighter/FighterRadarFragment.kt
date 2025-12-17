package com.example.mobile_uber_fight.ui.fighter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobile_uber_fight.adapter.FightAdapter
import com.example.mobile_uber_fight.databinding.FragmentFighterRadarBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository

class FighterRadarFragment : Fragment() {

    private var _binding: FragmentFighterRadarBinding? = null
    private val binding get() = _binding!!

    private lateinit var fightAdapter: FightAdapter
    private val fightRepository = FightRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFighterRadarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForPendingFights()
    }

    private fun setupRecyclerView() {
        fightAdapter = FightAdapter(emptyList()) { fight ->
            acceptFightOffer(fight)
        }
        binding.rvFights.apply {
            adapter = fightAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun listenForPendingFights() {
        setLoadingState(true)
        fightRepository.listenToPendingFights(
            onUpdate = { fights ->
                if (isAdded) {
                    setLoadingState(false)
                    updateUi(fights)
                }
            },
            onFailure = { exception ->
                if (isAdded) {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), "Erreur: ${exception.message}", Toast.LENGTH_LONG).show()
                }
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
                Toast.makeText(requireContext(), "Mission acceptée ! Consultez l\'onglet 'Mes Courses'", Toast.LENGTH_LONG).show()
            },
            onFailure = { exception ->
                Toast.makeText(requireContext(), "Échec de l\'acceptation: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
