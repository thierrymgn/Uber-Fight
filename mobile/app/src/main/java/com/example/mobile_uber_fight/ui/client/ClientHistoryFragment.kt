package com.example.mobile_uber_fight.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobile_uber_fight.adapter.HistoryAdapter
import com.example.mobile_uber_fight.databinding.FragmentClientHistoryBinding
import com.example.mobile_uber_fight.logger.GrafanaMetrics
import com.example.mobile_uber_fight.repositories.FightRepository

class ClientHistoryFragment : Fragment() {

    private var _binding: FragmentClientHistoryBinding? = null
    private val binding get() = _binding!!

    private val repository = FightRepository()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadHistory()
        GrafanaMetrics.screenView("client_history")
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList())
        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadHistory() {
        setLoading(true)
        repository.getClientHistory(
            onSuccess = { fights ->
                if (_binding != null) {
                    setLoading(false)
                    if (fights.isEmpty()) {
                        binding.emptyHistoryLayout.visibility = View.VISIBLE
                        binding.rvHistory.visibility = View.GONE
                    } else {
                        binding.emptyHistoryLayout.visibility = View.GONE
                        binding.rvHistory.visibility = View.VISIBLE
                        historyAdapter.updateList(fights)
                    }
                }
            },
            onFailure = { e ->
                if (_binding != null) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
