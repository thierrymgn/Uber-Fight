package com.example.mobile_uber_fight.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobile_uber_fight.databinding.FragmentClientHomeBinding
import com.example.mobile_uber_fight.repositories.FightRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ClientHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!

    private val fightRepository = FightRepository()
    private lateinit var googleMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupBottomSheet()
        setupListeners()
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.peekHeight = 250
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val paris = LatLng(48.8566, 2.3522)
        googleMap.addMarker(MarkerOptions().position(paris).title("Moi"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paris, 15f))
    }

    private fun setupListeners() {
        binding.btnOrderFight.setOnClickListener {
            handleOrderFightClick()
        }
    }

    private fun handleOrderFightClick() {
        val address = binding.etAddress.text.toString().trim()

        if (address.isEmpty()) {
            binding.tilAddress.error = "L'adresse est requise pour commander un duel"
            return
        }
        binding.tilAddress.error = null

        setLoadingState(true)

        val selectedRadioButtonId = binding.rgFightType.checkedRadioButtonId
        val fightType = view?.findViewById<RadioButton>(selectedRadioButtonId)?.text.toString()

        // (Paris, France)
        val latitude = 48.8566
        val longitude = 2.3522

        fightRepository.createFightRequest(
            address = address,
            lat = latitude,
            lng = longitude,
            fightType = fightType,
            onSuccess = {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Votre commande de duel a été envoyée !", Toast.LENGTH_LONG).show()
                binding.etAddress.text?.clear()
            },
            onFailure = { exception ->
                setLoadingState(false)
                Toast.makeText(requireContext(), "Erreur : ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnOrderFight.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
