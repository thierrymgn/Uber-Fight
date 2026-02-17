package com.example.mobile_uber_fight.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.ItemHistoryFightBinding
import com.example.mobile_uber_fight.models.Fight
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private var fights: List<Fight>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemHistoryFightBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryFightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val fight = fights[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvAddress.text = fight.address.ifEmpty { "Lieu non spécifié" }
            tvFightType.text = "Duel: ${fight.fightType}"

            val date = fight.createdAt
            if (date != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.getDefault())
                tvDate.text = sdf.format(date)
            } else {
                tvDate.text = "Date inconnue"
            }

            when (fight.status) {
                "COMPLETED" -> {
                    tvStatusBadge.text = "TERMINÉ"
                    tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
                }
                "CANCELLED" -> {
                    tvStatusBadge.text = "ANNULÉ"
                    tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.error_red))
                }
                else -> {
                    tvStatusBadge.text = fight.status
                    tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.text_hint))
                }
            }
        }
    }

    override fun getItemCount() = fights.size

    fun updateList(newFights: List<Fight>) {
        this.fights = newFights
        notifyDataSetChanged()
    }
}