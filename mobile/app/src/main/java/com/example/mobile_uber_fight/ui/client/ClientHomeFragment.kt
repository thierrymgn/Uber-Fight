package com.example.mobile_uber_fight.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobile_uber_fight.databinding.FragmentClientHomeBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale
import android.util.Log

class ClientHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!

    private val fightRepository = FightRepository()
    private lateinit var googleMap: GoogleMap
    private val userId = FirebaseAuth.getInstance().currentUser!!.uid
    private var selectedLocation: LatLng? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(requireContext(), "Permission de localisation refusée", Toast.LENGTH_SHORT).show()
        }
    }

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
        listenToCurrentRequest()
    }

    private fun listenToCurrentRequest() {
        fightRepository.listenToCurrentRequest(userId) { fight ->
            updateUIBasedOnFightStatus(fight)
        }
    }

    private fun updateUIBasedOnFightStatus(fight: Fight?) {
        when (fight?.status) {
            "PENDING" -> {
                binding.formLayout.visibility = View.GONE
                binding.pendingLayout.visibility = View.VISIBLE
                binding.acceptedLayout.visibility = View.GONE
            }
            "ACCEPTED" -> {
                binding.formLayout.visibility = View.GONE
                binding.pendingLayout.visibility = View.GONE
                binding.acceptedLayout.visibility = View.VISIBLE
            }
            else -> {
                binding.formLayout.visibility = View.VISIBLE
                binding.pendingLayout.visibility = View.GONE
                binding.acceptedLayout.visibility = View.GONE
            }
        }
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.peekHeight = 250
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            selectedLocation = center
            getAddressFromCoordinates(center)
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap.isMyLocationEnabled = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun getAddressFromCoordinates(latLng: LatLng) {
        lifecycleScope.launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0)
                    binding.etAddress.setText(addressText)
                } else {
                    binding.etAddress.setText("Position sur la carte")
                }
            } catch (e: Exception) {
                binding.etAddress.setText("Position sur la carte")
            }
        }
    }

    private fun setupListeners() {
        binding.btnOrderFight.setOnClickListener {
            handleOrderFightClick()
        }
        binding.fabLocateMe.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun handleOrderFightClick() {
        val address = binding.etAddress.text.toString().trim()

        if (address.isEmpty() || selectedLocation == null) {
            binding.tilAddress.error = "Veuillez sélectionner un lieu sur la carte"
            return
        }
        binding.tilAddress.error = null

        setLoadingState(true)

        val selectedRadioButtonId = binding.rgFightType.checkedRadioButtonId
        val fightType = view?.findViewById<RadioButton>(selectedRadioButtonId)?.text.toString()

        fightRepository.createFightRequest(
            address = address,
            lat = selectedLocation!!.latitude,
            lng = selectedLocation!!.longitude,
            fightType = fightType,
            onSuccess = {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Votre commande de duel a été envoyée !", Toast.LENGTH_LONG).show()
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
