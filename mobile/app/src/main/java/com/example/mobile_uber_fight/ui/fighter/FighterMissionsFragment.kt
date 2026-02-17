package com.example.mobile_uber_fight.ui.fighter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobile_uber_fight.databinding.FragmentFighterMissionsBinding
import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.repositories.FightRepository
import com.example.mobile_uber_fight.ui.shared.RatingBottomSheetFragment
import com.example.mobile_uber_fight.utils.DirectionsService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.example.mobile_uber_fight.logger.GrafanaLogger
import com.example.mobile_uber_fight.logger.GrafanaMetrics
import kotlinx.coroutines.launch

class FighterMissionsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFighterMissionsBinding? = null
    private val binding get() = _binding

    private val fightRepository = FightRepository()
    private var googleMap: GoogleMap? = null
    private var currentFight: Fight? = null
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFighterMissionsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GrafanaMetrics.screenView("fighter_missions")
        val mapFragment = childFragmentManager.findFragmentById(binding!!.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)
        listenForActiveFight()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        }
        updateMissionUI()
    }

    private fun listenForActiveFight() {
        fightRepository.listenToMyActiveFight(
            onFightFound = { fight ->
                if (_binding == null) return@listenToMyActiveFight
                
                // Transition to COMPLETED -> Open Rating
                if (currentFight?.status == "IN_PROGRESS" && fight == null) {
                    openRatingAndReset(currentFight!!)
                }
                
                currentFight = fight
                updateMissionUI()
            },
            onFailure = { 
                Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateMissionUI() {
        val fight = currentFight
        if (fight == null) {
            binding?.tvNoActiveMission?.visibility = View.VISIBLE
            binding?.cardMission?.visibility = View.GONE
            binding?.overlayInProgress?.visibility = View.GONE
            binding?.btnCancelMission?.visibility = View.GONE
            stopChrono()
            googleMap?.clear()
            return
        }

        binding?.tvNoActiveMission?.visibility = View.GONE
        binding?.tvAddress?.text = fight.address
        binding?.tvFightType?.text = "Duel: ${fight.fightType}"

        when (fight.status) {
            "ACCEPTED" -> {
                binding?.cardMission?.visibility = View.VISIBLE
                binding?.overlayInProgress?.visibility = View.GONE
                binding?.tvStatus?.text = "Bagarreur en route"
                binding?.btnAction?.text = "J'AI ATTEINT LE LIEU"
                binding?.btnAction?.setBackgroundColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary))
                binding?.btnAction?.setOnClickListener {
                    GrafanaMetrics.fightAction("start")
                    fightRepository.updateFightStatus(fight.id, "IN_PROGRESS", {})
                }
                binding?.btnCancelMission?.visibility = View.VISIBLE
                binding?.btnCancelMission?.setOnClickListener { handleCancelMission() }
                stopChrono()
                drawRouteToClient(fight)
            }
            "IN_PROGRESS" -> {
                binding?.cardMission?.visibility = View.VISIBLE
                binding?.overlayInProgress?.visibility = View.VISIBLE
                binding?.btnCancelMission?.visibility = View.GONE
                binding?.tvStatus?.text = "Combat en cours"
                binding?.btnAction?.text = "TERMINER LE DUEL"
                binding?.btnAction?.setBackgroundColor(Color.RED)
                binding?.btnAction?.setOnClickListener {
                    GrafanaMetrics.fightAction("complete")
                    fightRepository.updateFightStatus(fight.id, "COMPLETED", {})
                }
                startChrono()
                googleMap?.clear()
            }
        }
    }

    private fun handleCancelMission() {
        val fight = currentFight ?: return
        binding?.btnCancelMission?.isEnabled = false
        GrafanaLogger.logInfo("Fighter cancelling mission", mapOf("fightId" to fight.id))

        fightRepository.releaseFight(fight.id,
            onSuccess = {
                if (isAdded && _binding != null) {
                    GrafanaMetrics.fightAction("fighter_cancel")
                    binding?.btnCancelMission?.isEnabled = true
                    Toast.makeText(requireContext(), "Mission annulée", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { e ->
                if (isAdded && _binding != null) {
                    GrafanaLogger.logError("Fighter cancel mission failed", e, mapOf("fightId" to fight.id))
                    binding?.btnCancelMission?.isEnabled = true
                    Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun drawRouteToClient(fight: Fight) {
        val fightLoc = fight.location ?: return
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val origin = LatLng(it.latitude, it.longitude)
                val destination = LatLng(fightLoc.latitude, fightLoc.longitude)
                
                lifecycleScope.launch {
                    val route = DirectionsService.getDirections(origin, destination)
                    if (route != null && _binding != null) {
                        polyline?.remove()
                        polyline = googleMap?.addPolyline(PolylineOptions()
                            .addAll(route.polyline)
                            .color(Color.BLUE)
                            .width(10f))
                        
                        val bounds = LatLngBounds.Builder().include(origin).include(destination).build()
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                    }
                }
            }
        }
    }

    private fun startChrono() {
        if (secondsElapsed == 0) {
            chronoHandler.post(chronoRunnable)
        }
    }

    private fun stopChrono() {
        chronoHandler.removeCallbacks(chronoRunnable)
        secondsElapsed = 0
        binding?.tvChrono?.text = "00:00"
    }

    private fun openRatingAndReset(fight: Fight) {
        val ratingFragment = RatingBottomSheetFragment.newInstance(fight.requesterId, fight.id) {
        }
        ratingFragment.show(childFragmentManager, "Rating")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopChrono()
        _binding = null
    }
}