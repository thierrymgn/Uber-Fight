package com.example.mobile_uber_fight.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_uber_fight.databinding.ItemReviewBinding
import com.example.mobile_uber_fight.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(private var reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        with(holder.binding) {
            ratingBar.rating = review.rating.toFloat()

            if (review.comment.isNotEmpty()) {
                tvComment.text = review.comment
                tvComment.visibility = View.VISIBLE
            } else {
                tvComment.visibility = View.GONE
            }

            val date = review.createdAt
            if (date != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDate.text = sdf.format(date)
            } else {
                tvDate.text = ""
            }

            tvAuthorName.text = "Utilisateur"
            FirebaseFirestore.getInstance().collection("users").document(review.fromUserId).get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("username")
                    if (!username.isNullOrEmpty()) {
                        tvAuthorName.text = username
                    }
                }
        }
    }

    override fun getItemCount() = reviews.size

    fun updateList(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }
}
