package com.example.mobile_uber_fight.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
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
import com.example.mobile_uber_fight.adapter.PlacesAdapter
import com.example.mobile_uber_fight.databinding.FragmentClientHomeBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.models.User
import com.example.mobile_uber_fight.repositories.FightRepository
import com.example.mobile_uber_fight.repositories.UserRepository
import com.example.mobile_uber_fight.ui.shared.RatingBottomSheetFragment
import com.example.mobile_uber_fight.utils.DirectionsService
import com.example.mobile_uber_fight.logger.GrafanaLogger
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
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.graphics.createBitmap
import com.example.mobile_uber_fight.BuildConfig

class ClientHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding

    private val fightRepository = FightRepository()
    private val userRepository = UserRepository()
    private lateinit var googleMap: GoogleMap
    
    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
    
    private var currentUserLocation: Location? = null
    private var fightersList: List<User> = emptyList()
    private var currentFightId: String? = null
    private var currentFight: Fight? = null
    private var isSearchingAddress = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true

    private lateinit var placesClient: PlacesClient
    private lateinit var placesAdapter: PlacesAdapter

    private var fighterLocationListener: ListenerRegistration? = null
    private var fighterMarker: Marker? = null
    private var polyline: Polyline? = null

    private var chronoHandler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private val chronoRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            val mins = secondsElapsed / 60
            val secs = secondsElapsed % 60
            binding?.tvChrono?.text = String.format("%02d:%02d", mins, secs)
            chronoHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val TAG = "ClientHomeFragment"
        private const val RADIUS_IN_METERS = 4000.0
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            GrafanaLogger.logInfo("Location permission granted by client")
            startLocationUpdates()
        } else {
            GrafanaLogger.logWarn("Location permission denied by client")
            Toast.makeText(requireContext(), "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GrafanaLogger.logDebug("ClientHomeFragment: onViewCreated")
        initPlaces()
        
        val mapFragment = childFragmentManager.findFragmentById(binding!!.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()
        setupBottomSheet()
        setupListeners()
    }

    private fun initPlaces() {
        try {
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
            }
            placesClient = Places.createClient(requireContext())
            placesAdapter = PlacesAdapter(requireContext(), placesClient)
            binding?.etAddress?.setAdapter(placesAdapter)
        } catch (e: Exception) {
            GrafanaLogger.logError("Places initialization failed", e)
        }
    }

    private fun listenToCurrentRequest() {
        val uid = currentUserId ?: return
        GrafanaLogger.logDebug("Listening to current fight request", mapOf("userId" to uid))
        fightRepository.listenToCurrentRequest(uid) { fight ->
            if (_binding == null) return@listenToCurrentRequest
            
            if (currentFight?.status == "IN_PROGRESS" && fight == null) {
                GrafanaLogger.logInfo("Fight completed, opening rating", mapOf("fightId" to currentFight?.id.toString()))
                openRatingAndReset(currentFight!!)
            }

            if (currentFight?.status != fight?.status) {
                GrafanaLogger.logInfo("Fight status transition", mapOf(
                    "old" to currentFight?.status.toString(),
                    "new" to fight?.status.toString()
                ))
            }

            currentFight = fight
            currentFightId = fight?.id
            updateUIBasedOnFightStatus(fight)
            
            if (fight?.status == "ACCEPTED" || fight?.status == "IN_PROGRESS") {
                fight.fighterId?.let { startTrackingFighter(it, fight) }
            } else {
                stopTrackingFighter()
            }
        }
    }

    private fun startTrackingFighter(fighterId: String, fight: Fight) {
        if (!::googleMap.isInitialized) return
        if (fighterLocationListener != null) return

        GrafanaLogger.logInfo("Starting live tracking of fighter", mapOf("fighterId" to fighterId))
        googleMap.clear()

        fighterLocationListener = userRepository.listenToUserLocation(fighterId) { geoPoint ->
            if (_binding == null) return@listenToUserLocation
            geoPoint?.let { gp ->
                val fighterLatLng = LatLng(gp.latitude, gp.longitude)
                updateFighterMarker(fighterLatLng)
                
                fight.location?.let { fightGP ->
                    val fightLatLng = LatLng(fightGP.latitude, fightGP.longitude)
                    drawRoute(fighterLatLng, fightLatLng)
                }
            }
        }
    }

    private fun stopTrackingFighter() {
        if (fighterLocationListener != null) {
            GrafanaLogger.logDebug("Stopping fighter live tracking")
        }
        fighterLocationListener?.remove()
        fighterLocationListener = null
        fighterMarker?.remove()
        fighterMarker = null
        polyline?.remove()
        polyline = null
        binding?.cvEta?.visibility = View.GONE
        
        if (::googleMap.isInitialized) {
            displayFightersOnMap()
        }
    }

    private fun updateFighterMarker(latLng: LatLng) {
        if (_binding == null || !::googleMap.isInitialized) return
        val fighterIcon = bitmapDescriptorFromVector(requireContext(), R.drawable.ic_fighter_marker)
        if (fighterMarker == null) {
            fighterMarker = googleMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Bagarreur")
                .icon(fighterIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        } else {
            fighterMarker?.position = latLng
        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        lifecycleScope.launch {
            val route = DirectionsService.getDirections(origin, destination)
            if (route != null && isAdded && _binding != null && ::googleMap.isInitialized) {
                polyline?.remove()
                polyline = googleMap.addPolyline(PolylineOptions()
                    .addAll(route.polyline)
                    .color(Color.BLUE)
                    .width(10f))
                
                binding?.tvEta?.text = "Arrivée dans ${route.duration} (${route.distance})"
                binding?.cvEta?.visibility = View.VISIBLE

                val bounds = LatLngBounds.Builder()
                    .include(origin)
                    .include(destination)
                    .build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    private fun updateUIBasedOnFightStatus(fight: Fight?) {
        if (_binding == null) return
        
        when (fight?.status) {
            "PENDING" -> {
                binding?.formLayout?.visibility = View.GONE
                binding?.pendingLayout?.visibility = View.VISIBLE
                binding?.acceptedLayout?.visibility = View.GONE
                binding?.ivMapCenterPin?.visibility = View.GONE
                binding?.overlayInProgress?.visibility = View.GONE
                stopChrono()
            }
            "ACCEPTED" -> {
                binding?.formLayout?.visibility = View.GONE
                binding?.pendingLayout?.visibility = View.GONE
                binding?.acceptedLayout?.visibility = View.VISIBLE
                binding?.ivMapCenterPin?.visibility = View.GONE
                binding?.tvFighterName?.text = "Bagarreur en route !"
                binding?.overlayInProgress?.visibility = View.GONE
                stopChrono()
            }
            "IN_PROGRESS" -> {
                binding?.formLayout?.visibility = View.GONE
                binding?.pendingLayout?.visibility = View.GONE
                binding?.acceptedLayout?.visibility = View.GONE
                binding?.ivMapCenterPin?.visibility = View.GONE
                binding?.overlayInProgress?.visibility = View.VISIBLE
                startChrono()
            }
            else -> {
                binding?.formLayout?.visibility = View.VISIBLE
                binding?.pendingLayout?.visibility = View.GONE
                binding?.acceptedLayout?.visibility = View.GONE
                binding?.ivMapCenterPin?.visibility = View.VISIBLE
                binding?.overlayInProgress?.visibility = View.GONE
                stopChrono()
            }
        }
    }

    private fun startChrono() {
        if (secondsElapsed == 0) {
            GrafanaLogger.logDebug("Starting fight chronometer")
            chronoHandler.post(chronoRunnable)
        }
    }

    private fun stopChrono() {
        if (secondsElapsed > 0) {
            GrafanaLogger.logDebug("Stopping chronometer", mapOf("durationSeconds" to secondsElapsed))
        }
        chronoHandler.removeCallbacks(chronoRunnable)
        secondsElapsed = 0
        binding?.tvChrono?.text = "00:00"
    }

    private fun openRatingAndReset(fight: Fight) {
        fight.fighterId?.let { fighterId ->
            val ratingFragment = RatingBottomSheetFragment.newInstance(fighterId, fight.id) {
                GrafanaLogger.logDebug("Rating fragment closed")
            }
            ratingFragment.show(childFragmentManager, "Rating")
        }
    }

    private fun setupBottomSheet() {
        BottomSheetBehavior.from(binding!!.bottomSheet).apply {
            peekHeight = 250
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onMapReady(map: GoogleMap) {
        GrafanaLogger.logInfo("Google Map ready for client")
        googleMap = map
        
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        val paddingBottom = (300 * resources.displayMetrics.density).toInt()
        googleMap.setPadding(0, 0, 0, paddingBottom)

        googleMap.setOnCameraIdleListener {
            if (_binding != null && !isSearchingAddress) {
                val center = googleMap.cameraPosition.target
                getAddressFromCoordinates(center)
            }
        }

        checkLocationPermission()
        userRepository.listenToNearbyFighters { fighters ->
            if (_binding == null) return@listenToNearbyFighters
            GrafanaLogger.logDebug("Nearby fighters updated", mapOf("count" to fighters.size))
            this.fightersList = fighters
            displayFightersOnMap()
        }
        
        listenToCurrentRequest()
    }

    private fun displayFightersOnMap() {
        if (!::googleMap.isInitialized || _binding == null) return
        val myLocation = currentUserLocation ?: return
        
        if (fighterLocationListener != null) return 
        
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
        } catch (e: Exception) { 
            GrafanaLogger.logError("Marker icon creation failed", e)
            null 
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (_binding == null) return
                locationResult.lastLocation?.let { location ->
                    currentUserLocation = location
                    userRepository.updateUserLocation(location.latitude, location.longitude)
                    
                    if (fighterLocationListener == null && fightersList.isNotEmpty()) {
                        displayFightersOnMap()
                    }
                    
                    if (isFirstLocationUpdate && ::googleMap.isInitialized) {
                        GrafanaLogger.logInfo("First location fix received", mapOf("lat" to location.latitude, "lng" to location.longitude))
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
                if (_binding != null && !isSearchingAddress) {
                    val firstAddr = addresses?.firstOrNull()?.getAddressLine(0) ?: "Position sur la carte"
                    binding?.etAddress?.setText(firstAddr)
                }
            } catch (e: Exception) { 
                GrafanaLogger.logWarn("Reverse geocoding failed", mapOf("lat" to latLng.latitude, "lng" to latLng.longitude))
                if (_binding != null && !isSearchingAddress) {
                    binding?.etAddress?.setText("Position sur la carte") 
                }
            }
        }
    }

    private fun setupListeners() {
        binding?.btnOrderFight?.setOnClickListener { handleOrderFightClick() }
        binding?.btnCancelSearch?.setOnClickListener { handleCancelFightClick() }
        binding?.fabLocateMe?.setOnClickListener {
            isFirstLocationUpdate = true
            checkLocationPermission()
        }

        binding?.etAddress?.setOnItemClickListener { _, _, position, _ ->
            val placeId = placesAdapter.getPlaceId(position)
            GrafanaLogger.logInfo("Place selected from autocomplete", mapOf("placeId" to placeId))
            fetchPlaceDetails(placeId)
        }

        binding?.etAddress?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                GrafanaLogger.logInfo("Manual address search triggered", mapOf("query" to v.text.toString()))
                searchAddress(v.text.toString())
                hideKeyboard(v)
                true
            } else false
        }
    }

    private fun fetchPlaceDetails(placeId: String) {
        val fields = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, fields)

        isSearchingAddress = true
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            if (_binding == null) return@addOnSuccessListener
            val place = response.place
            place.location?.let { latLng ->
                GrafanaLogger.logInfo("Place coordinates fetched", mapOf("name" to place.displayName.toString(), "lat" to latLng.latitude))
                if (::googleMap.isInitialized) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
                hideKeyboard(binding!!.etAddress)
            }
            isSearchingAddress = false
        }.addOnFailureListener { e ->
            GrafanaLogger.logError("Fetch place details failed", e, mapOf("placeId" to placeId))
            isSearchingAddress = false
            Toast.makeText(requireContext(), "Erreur lors de la récupération du lieu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchAddress(addressText: String) {
        if (addressText.isEmpty()) return
        lifecycleScope.launch {
            try {
                isSearchingAddress = true
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocationName(addressText, 1) }
                if (!addresses.isNullOrEmpty() && _binding != null) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    GrafanaLogger.logInfo("Manual geocoding success", mapOf("address" to addressText))
                    if (::googleMap.isInitialized) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                } else if (_binding != null) {
                    GrafanaLogger.logWarn("Manual geocoding: Address not found", mapOf("address" to addressText))
                    Toast.makeText(requireContext(), "Adresse introuvable", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                GrafanaLogger.logError("Manual geocoding exception", e, mapOf("address" to addressText))
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Erreur de recherche", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSearchingAddress = false
            }
        }
    }

    private fun handleOrderFightClick() {
        if (!::googleMap.isInitialized) return
        
        val center = googleMap.cameraPosition.target
        val address = binding?.etAddress?.text.toString().trim()
        
        if (address.isEmpty()) {
            binding?.tilAddress?.error = "Veuillez saisir une adresse"
            return
        }
        
        setLoadingState(true)
        val selectedId = binding?.rgFightType?.checkedRadioButtonId ?: -1
        val type = view?.findViewById<RadioButton>(selectedId)?.text.toString()

        GrafanaLogger.logInfo("Client confirming order", mapOf("address" to address, "type" to type))

        fightRepository.createFightRequest(
            address = address, 
            lat = center.latitude, 
            lng = center.longitude, 
            fightType = type,
            onSuccess = { fightId ->
                if (_binding != null) {
                    setLoadingState(false)
                    currentFightId = fightId
                }
            },
            onFailure = { e ->
                if (_binding != null) {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun handleCancelFightClick() {
        val id = currentFightId ?: return
        setLoadingState(true)
        fightRepository.cancelFight(id,
            onSuccess = { 
                GrafanaLogger.logInfo("Fight cancelled by client button")
                if (_binding != null) setLoadingState(false) 
            },
            onFailure = { e ->
                if (_binding != null) {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), "Erreur d'annulation", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding?.btnOrderFight?.isEnabled = !isLoading
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
        if (!::googleMap.isInitialized) return
        GrafanaLogger.logDebug("Starting client location updates")
        googleMap.isMyLocationEnabled = true
        val request = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun onResume() {
        super.onResume()
        GrafanaLogger.logDebug("ClientHomeFragment: onResume")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        GrafanaLogger.logDebug("ClientHomeFragment: onPause")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopTrackingFighter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        GrafanaLogger.logDebug("ClientHomeFragment: onDestroyView")
        stopChrono()
        _binding = null
    }
}