package com.example.sibuka.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sibuka.R
import com.example.sibuka.databinding.ItemBorrowingBinding
import com.example.sibuka.models.Borrowing
import java.text.SimpleDateFormat
import java.util.*

class BorrowingAdapter(
    private val borrowings: MutableList<Borrowing>,
    private val onItemClick: (Borrowing) -> Unit,
    private val onDeleteClick: (Borrowing) -> Unit
) : RecyclerView.Adapter<BorrowingAdapter.BorrowingViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class BorrowingViewHolder(private val binding: ItemBorrowingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(borrowing: Borrowing) {
            binding.apply {
                textBorrowerName.text = borrowing.borrowerName
                textBorrowerNim.text = "NIM: ${borrowing.borrowerNim}"
                textBorrowerClass.text = borrowing.borrowerClass
                textBookTitle.text = borrowing.bookTitle
                textBorrowDate.text = "Pinjam: ${dateFormat.format(Date(borrowing.borrowDate))}"
                textDueDate.text = "Jatuh Tempo: ${dateFormat.format(Date(borrowing.dueDate))}"
                when (borrowing.status) {
                    "active" -> {
                        textStatus.text = "AKTIF"
                        textStatus.setBackgroundResource(R.color.colorSecondary)
                        textStatus.setTextColor(itemView.context.getColor(R.color.colorOnSecondary))
                        textReturnDate.visibility = android.view.View.GONE
                    }
                    "returned" -> {
                        textStatus.text = "DIKEMBALIKAN"
                        textStatus.setBackgroundResource(android.R.color.holo_green_dark)
                        textStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                        borrowing.returnDate?.let {
                            textReturnDate.text = "Dikembalikan: ${dateFormat.format(Date(it))}"
                            textReturnDate.visibility = android.view.View.VISIBLE
                        }
                    }
                    "overdue" -> {
                        textStatus.text = "TERLAMBAT"
                        textStatus.setBackgroundResource(R.color.colorError)
                        textStatus.setTextColor(itemView.context.getColor(R.color.colorOnError))

                        val daysOverdue = ((System.currentTimeMillis() - borrowing.dueDate) / (24 * 60 * 60 * 1000)).toInt()
                        textReturnDate.text = "Terlambat $daysOverdue hari"
                        textReturnDate.visibility = android.view.View.VISIBLE
                    }
                }
                btnDelete.visibility = if (borrowing.status == "returned") {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                root.setOnClickListener {
                    onItemClick(borrowing)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(borrowing)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowingViewHolder {
        val binding = ItemBorrowingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BorrowingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BorrowingViewHolder, position: Int) {
        holder.bind(borrowings[position])
    }

    override fun getItemCount(): Int = borrowings.size

    fun removeBorrowing(borrowing: Borrowing) {
        val position = borrowings.indexOf(borrowing)
        if (position != -1) {
            borrowings.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
