package com.example.mobile_uber_fight.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.FragmentClientHomeBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.models.User
import com.example.mobile_uber_fight.repositories.FightRepository
import com.example.mobile_uber_fight.repositories.UserRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.graphics.createBitmap

class ClientHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!

    private val fightRepository = FightRepository()
    private val userRepository = UserRepository()
    private lateinit var googleMap: GoogleMap
    private val userId = FirebaseAuth.getInstance().currentUser!!.uid
    private var selectedLocation: LatLng? = null
    private var currentUserLocation: Location? = null
    private var fightersList: List<User> = emptyList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true

    companion object {
        private const val RADIUS_IN_METERS = 4000.0
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startLocationUpdates()
        else Toast.makeText(requireContext(), "Permission refusée", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()
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
        binding.formLayout.visibility = if (fight == null) View.VISIBLE else View.GONE
        binding.pendingLayout.visibility = if (fight?.status == "PENDING") View.VISIBLE else View.GONE
        binding.acceptedLayout.visibility = if (fight?.status == "ACCEPTED") View.VISIBLE else View.GONE
    }

    private fun setupBottomSheet() {
        BottomSheetBehavior.from(binding.bottomSheet).apply {
            peekHeight = 250
            state = BottomSheetBehavior.STATE_EXPANDED
        }
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

        // Listen to fighters from Firestore
        userRepository.listenToNearbyFighters { fighters ->
            this.fightersList = fighters
            displayFightersOnMap()
        }
    }

    private fun displayFightersOnMap() {
        if (!::googleMap.isInitialized) return

        val myLocation = currentUserLocation ?: return

        googleMap.clear() // Remove old markers

        fightersList.forEach { fighter ->
            val fighterLoc = fighter.location ?: return@forEach

            val results = FloatArray(1)
            Location.distanceBetween(
                myLocation.latitude, myLocation.longitude,
                fighterLoc.latitude, fighterLoc.longitude,
                results
            )

            if (results[0] <= RADIUS_IN_METERS) {
                googleMap.addMarker(MarkerOptions()
                    .position(LatLng(fighterLoc.latitude, fighterLoc.longitude))
                    .title("${fighter.username} ⭐ ${fighter.rating}")
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_fighter_marker))
                )
            }
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return try {
            ContextCompat.getDrawable(context, vectorResId)?.run {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
                draw(Canvas(bitmap))
                BitmapDescriptorFactory.fromBitmap(bitmap)
            }
        } catch (e: Exception) { null }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentUserLocation = location
                    userRepository.updateUserLocation(location.latitude, location.longitude)

                    // Refresh markers with new location
                    displayFightersOnMap()

                    if (isFirstLocationUpdate) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                        isFirstLocationUpdate = false
                    }
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        googleMap.isMyLocationEnabled = true
        val request = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun getAddressFromCoordinates(latLng: LatLng) {
        lifecycleScope.launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                binding.etAddress.setText(addresses?.firstOrNull()?.getAddressLine(0) ?: "Position sur la carte")
            } catch (e: Exception) {
                binding.etAddress.setText("Position sur la carte")
            }
        }
    }

    private fun setupListeners() {
        binding.btnOrderFight.setOnClickListener { handleOrderFightClick() }
        binding.fabLocateMe.setOnClickListener {
            isFirstLocationUpdate = true
            checkLocationPermission()
        }
    }

    private fun handleOrderFightClick() {
        val address = binding.etAddress.text.toString().trim()
        val loc = selectedLocation
        if (address.isEmpty() || loc == null) {
            binding.tilAddress.error = "Sélectionnez un lieu"
            return
        }

        setLoadingState(true)
        val type = view?.findViewById<RadioButton>(binding.rgFightType.checkedRadioButtonId)?.text.toString()

        fightRepository.createFightRequest(address, loc.latitude, loc.longitude, type,
            onSuccess = {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Duel envoyé !", Toast.LENGTH_LONG).show()
            },
            onFailure = {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnOrderFight.isEnabled = !isLoading
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