package com.example.mobile_uber_fight.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_uber_fight.databinding.ItemFightRequestBinding
import com.example.mobile_uber_fight.models.Fight

class FightAdapter(
    private var fights: List<Fight>,
    private val onAcceptClick: (Fight) -> Unit
) : RecyclerView.Adapter<FightAdapter.FightViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FightViewHolder {
        val binding = ItemFightRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FightViewHolder, position: Int) {
        val fight = fights[position]
        holder.bind(fight)
    }

    override fun getItemCount(): Int = fights.size

    fun updateFights(newFights: List<Fight>) {
        fights = newFights
        notifyDataSetChanged() // faut changer ça et utiliser DiffUtil si on met en prod ( normalement non )
    }

    inner class FightViewHolder(private val binding: ItemFightRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fight: Fight) {
            binding.tvFightType.text = fight.fightType
            binding.tvAddress.text = fight.address

            binding.btnAccept.setOnClickListener {
                onAcceptClick(fight)
            }
        }
    }
}
