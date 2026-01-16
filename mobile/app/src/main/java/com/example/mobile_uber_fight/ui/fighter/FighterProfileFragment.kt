package com.example.mobile_uber_fight.ui.fighter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.FragmentFighterProfileBinding

class FighterProfileFragment : Fragment() {

    private var _binding: FragmentFighterProfileBinding? = null
    private val binding get() = _binding!!

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

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_fighterProfileFragment_to_settingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
