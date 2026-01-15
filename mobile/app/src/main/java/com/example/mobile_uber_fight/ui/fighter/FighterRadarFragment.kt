package com.example.mobile_uber_fight.ui.fighter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mobile_uber_fight.databinding.FragmentFighterRadarBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository
import com.example.mobile_uber_fight.repositories.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class FighterRadarFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFighterRadarBinding? = null
    private val binding get() = _binding!!

    private val fightRepository = FightRepository()
    private val userRepository = UserRepository()
    private lateinit var googleMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var isFirstLocationUpdate = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startLocationUpdates()
        }
    }

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()

        val mapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupBottomSheet()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.missionBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        
        checkLocationPermission()
        setupMapListeners()
        listenForPendingFights()
    }

    private fun setupMapListeners() {
        googleMap.setOnMarkerClickListener { marker ->
            val fight = marker.tag as? Fight
            if (fight != null) {
                showMissionDetails(fight)
            }
            false
        }

        googleMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Mise à jour de la position du bagarreur dans Firestore
                    userRepository.updateUserLocation(location.latitude, location.longitude)

                    if (isFirstLocationUpdate) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                        isFirstLocationUpdate = false
                    }
                }
            }
        }
    }

    private fun showMissionDetails(fight: Fight) {
        binding.tvFightTitle.text = fight.fightType
        binding.tvFightAddress.text = fight.address
        
        binding.btnAcceptMission.setOnClickListener {
            acceptFightOffer(fight)
        }
        
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun listenForPendingFights() {
        setLoadingState(true)
        fightRepository.listenToPendingFights(
            onUpdate = { fights ->
                if (isAdded) {
                    setLoadingState(false)
                    updateMapMarkers(fights)
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

    private fun updateMapMarkers(fights: List<Fight>) {
        googleMap.clear()
        
        if (fights.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.tvEmpty.visibility = View.GONE
            
            fights.forEach { fight ->
                val loc = fight.location ?: return@forEach
                val latLng = LatLng(loc.latitude, loc.longitude)
                
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(fight.fightType)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                )
                marker?.tag = fight
            }
        }
    }

    private fun acceptFightOffer(fight: Fight) {
        setLoadingState(true)
        fightRepository.acceptFight(fight.id,
            onSuccess = {
                if (isAdded) {
                    setLoadingState(false)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    Toast.makeText(requireContext(), "Mission acceptée !", Toast.LENGTH_LONG).show()
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

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (::googleMap.isInitialized) {
            googleMap.isMyLocationEnabled = true
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
