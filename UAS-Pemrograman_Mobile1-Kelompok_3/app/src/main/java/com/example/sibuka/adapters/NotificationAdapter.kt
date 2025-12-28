package com.example.sibuka.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sibuka.R
import com.example.sibuka.databinding.ItemNotificationBinding
import com.example.sibuka.models.Borrowing
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<Borrowing>,
    private val onItemClick: (Borrowing) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(borrowing: Borrowing) {
            binding.apply {
                textBorrowerName.text = borrowing.borrowerName
                textBookTitle.text = borrowing.bookTitle
                textDueDate.text = "Jatuh Tempo: ${dateFormat.format(Date(borrowing.dueDate))}"
                
                val currentTime = System.currentTimeMillis()
                when {
                    borrowing.dueDate < currentTime -> {
                        val daysOverdue = ((currentTime - borrowing.dueDate) / (24 * 60 * 60 * 1000)).toInt()
                        textNotificationMessage.text = "Terlambat $daysOverdue hari"
                        textNotificationMessage.setTextColor(itemView.context.getColor(R.color.colorError))
                        iconNotification.setImageResource(R.drawable.ic_notifications)
                        iconNotification.setColorFilter(itemView.context.getColor(R.color.colorError))
                    }
                    borrowing.dueDate <= currentTime + (2 * 24 * 60 * 60 * 1000) -> {
                        val daysLeft = ((borrowing.dueDate - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                        val message = if (daysLeft == 0) "Jatuh tempo hari ini" else "Jatuh tempo dalam $daysLeft hari"
                        textNotificationMessage.text = message
                        textNotificationMessage.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                        iconNotification.setImageResource(R.drawable.ic_notifications)
                        iconNotification.setColorFilter(itemView.context.getColor(android.R.color.holo_orange_dark))
                    }
                    else -> {
                        val daysLeft = ((borrowing.dueDate - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                        textNotificationMessage.text = "Jatuh tempo dalam $daysLeft hari"
                        textNotificationMessage.setTextColor(itemView.context.getColor(R.color.colorSecondary))
                        iconNotification.setImageResource(R.drawable.ic_notifications)
                        iconNotification.setColorFilter(itemView.context.getColor(R.color.colorSecondary))
                    }
                }

                root.setOnClickListener {
                    onItemClick(borrowing)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}
