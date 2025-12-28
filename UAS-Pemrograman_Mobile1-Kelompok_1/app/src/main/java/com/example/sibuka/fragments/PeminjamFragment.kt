package com.example.sibuka.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sibuka.adapters.BorrowingAdapter
import com.example.sibuka.databinding.FragmentPeminjamBinding
import com.example.sibuka.models.Borrowing
import com.example.sibuka.utils.FirebaseUtils
import com.google.firebase.firestore.Query

class PeminjamFragment : Fragment() {
    private var _binding: FragmentPeminjamBinding? = null
    private val binding get() = _binding!!
    private lateinit var borrowingAdapter: BorrowingAdapter
    private val borrowings = mutableListOf<Borrowing>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeminjamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadBorrowings()
    }

    private fun setupRecyclerView() {
        borrowingAdapter = BorrowingAdapter(
            borrowings = borrowings,
            onItemClick = { borrowing ->
                showBorrowingDetails(borrowing)
            },
            onDeleteClick = { borrowing ->
                showDeleteConfirmation(borrowing)
            }
        )
        binding.recyclerViewBorrowings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = borrowingAdapter
        }
    }

    private fun setupClickListeners() {
        binding.chipAll.setOnClickListener {
            loadBorrowings()
        }

        binding.chipActive.setOnClickListener {
            loadBorrowings("active")
        }

        binding.chipReturned.setOnClickListener {
            loadBorrowings("returned")
        }

        binding.chipOverdue.setOnClickListener {
            loadBorrowings("overdue")
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchBorrowings(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadBorrowings()
                } else {
                    searchBorrowings(newText)
                }
                return true
            }
        })
    }

    private fun loadBorrowings(status: String? = null) {
        val query = if (status != null) {
            FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
                .whereEqualTo("status", status)
        } else {
            FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                return@addSnapshotListener
            }

            borrowings.clear()
            val currentTime = System.currentTimeMillis()

            snapshot?.documents?.forEach { document ->
                val borrowing = document.toObject(Borrowing::class.java)
                borrowing?.let {
                    if (it.status == "active" && currentTime > it.dueDate) {
                        updateBorrowingStatus(it.id, "overdue")
                        val updatedBorrowing = it.copy(status = "overdue")
                        borrowings.add(updatedBorrowing)
                    } else {
                        borrowings.add(it)
                    }
                }
            }

            borrowings.sortByDescending { it.borrowDate }

            if (isAdded && context != null) {
                borrowingAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun searchBorrowings(query: String?) {
        if (query.isNullOrEmpty()) {
            loadBorrowings()
            return
        }

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .get()
            .addOnSuccessListener { snapshot ->
                borrowings.clear()
                val searchQuery = query.lowercase()

                snapshot.documents.forEach { document ->
                    val borrowing = document.toObject(Borrowing::class.java)
                    borrowing?.let {
                        if (it.borrowerName.lowercase().contains(searchQuery) ||
                            it.borrowerNim.lowercase().contains(searchQuery) ||
                            it.bookTitle.lowercase().contains(searchQuery) ||
                            it.borrowerClass.lowercase().contains(searchQuery)) {
                            borrowings.add(it)
                        }
                    }
                }

                borrowings.sortByDescending { it.borrowDate }

                if (isAdded && context != null) {
                    borrowingAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
            .addOnFailureListener { error ->
                if (isAdded && context != null) {
                    Toast.makeText(context, "Error searching: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateBorrowingStatus(borrowingId: String, status: String) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .document(borrowingId)
            .update("status", status)
    }

    private fun showBorrowingDetails(borrowing: Borrowing) {
        // TODO: Implement borrowing details dialog
        Toast.makeText(context, "Details for: ${borrowing.borrowerName}", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(borrowing: Borrowing) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus Riwayat Peminjaman")
            .setMessage("Apakah Anda yakin ingin menghapus riwayat peminjaman buku \"${borrowing.bookTitle}\" oleh ${borrowing.borrowerName}?\n\nTindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBorrowingHistory(borrowing)
            }
            .setNegativeButton("Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteBorrowingHistory(borrowing: Borrowing) {
        if (!isAdded || context == null) return

        Toast.makeText(context, "Menghapus riwayat...", Toast.LENGTH_SHORT).show()

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .document(borrowing.id)
            .delete()
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Riwayat peminjaman berhasil dihapus", Toast.LENGTH_SHORT).show()
                    borrowingAdapter.removeBorrowing(borrowing)
                    updateEmptyState()
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded && context != null) {
                    Toast.makeText(context, "Gagal menghapus riwayat: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateEmptyState() {
        if (borrowings.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewBorrowings.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewBorrowings.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
