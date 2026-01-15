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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var currentFightId: String? = null
    private var isSearchingAddress = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true

    companion object {
        private const val TAG = "ClientHomeFragment"
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
            currentFightId = fight?.id
            updateUIBasedOnFightStatus(fight)
        }
    }

    private fun updateUIBasedOnFightStatus(fight: Fight?) {
        when (fight?.status) {
            "PENDING" -> {
                binding.formLayout.visibility = View.GONE
                binding.pendingLayout.visibility = View.VISIBLE
                binding.acceptedLayout.visibility = View.GONE
                binding.ivMapCenterPin.visibility = View.GONE
            }
            "ACCEPTED" -> {
                binding.formLayout.visibility = View.GONE
                binding.pendingLayout.visibility = View.GONE
                binding.acceptedLayout.visibility = View.VISIBLE
                binding.ivMapCenterPin.visibility = View.GONE
                binding.tvFighterName.text = "Bagarreur en route !"
            }
            else -> {
                binding.formLayout.visibility = View.VISIBLE
                binding.pendingLayout.visibility = View.GONE
                binding.acceptedLayout.visibility = View.GONE
                binding.ivMapCenterPin.visibility = View.VISIBLE
            }
        }
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
            if (!isSearchingAddress) {
                val center = googleMap.cameraPosition.target
                selectedLocation = center
                getAddressFromCoordinates(center)
            }
        }

        checkLocationPermission()
        userRepository.listenToNearbyFighters { fighters ->
            this.fightersList = fighters
            displayFightersOnMap()
        }
    }

    private fun displayFightersOnMap() {
        if (!::googleMap.isInitialized) return
        val myLocation = currentUserLocation ?: return
        googleMap.clear()
        fightersList.forEach { fighter ->
            val fighterLoc = fighter.location ?: return@forEach
            val results = FloatArray(1)
            Location.distanceBetween(myLocation.latitude, myLocation.longitude, fighterLoc.latitude, fighterLoc.longitude, results)
            if (results[0] <= RADIUS_IN_METERS) {
                googleMap.addMarker(MarkerOptions()
                    .position(LatLng(fighterLoc.latitude, fighterLoc.longitude))
                    .title("${fighter.username} ⭐ ${fighter.rating}")
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_fighter_marker)))
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
                    if (fightersList.isNotEmpty()) displayFightersOnMap()
                    if (isFirstLocationUpdate) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                        isFirstLocationUpdate = false
                    }
                }
            }
        }
    }

    private fun getAddressFromCoordinates(latLng: LatLng) {
        lifecycleScope.launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) }
                binding.etAddress.setText(addresses?.firstOrNull()?.getAddressLine(0) ?: "Position sur la carte")
            } catch (e: Exception) { binding.etAddress.setText("Position sur la carte") }
        }
    }

    private fun setupListeners() {
        binding.btnOrderFight.setOnClickListener { handleOrderFightClick() }
        binding.btnCancelSearch.setOnClickListener { handleCancelFightClick() }
        binding.fabLocateMe.setOnClickListener {
            isFirstLocationUpdate = true
            checkLocationPermission()
        }

        binding.etAddress.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchAddress(v.text.toString())
                hideKeyboard(v)
                true
            } else false
        }
    }

    private fun searchAddress(addressText: String) {
        if (addressText.isEmpty()) return
        lifecycleScope.launch {
            try {
                isSearchingAddress = true
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocationName(addressText, 1) }
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    selectedLocation = latLng
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(requireContext(), "Adresse introuvable", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de recherche", Toast.LENGTH_SHORT).show()
            } finally {
                isSearchingAddress = false
            }
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
        val selectedId = binding.rgFightType.checkedRadioButtonId
        val type = view?.findViewById<RadioButton>(selectedId)?.text.toString()

        fightRepository.createFightRequest(address, loc.latitude, loc.longitude, type,
            onSuccess = { fightId ->
                setLoadingState(false)
                currentFightId = fightId
            },
            onFailure = { 
                setLoadingState(false)
                Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun handleCancelFightClick() {
        val id = currentFightId ?: return
        setLoadingState(true)
        fightRepository.cancelFight(id,
            onSuccess = { setLoadingState(false) },
            onFailure = {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Erreur d\'annulation", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnOrderFight.isEnabled = !isLoading
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startLocationUpdates()
        else locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}