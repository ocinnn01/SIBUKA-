package com.example.sibuka.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sibuka.adapters.NotificationAdapter
import com.example.sibuka.databinding.FragmentNotifikasiBinding
import com.example.sibuka.models.Borrowing
import com.example.sibuka.utils.FirebaseUtils
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotifikasiFragment : Fragment() {
    private var _binding: FragmentNotifikasiBinding? = null
    private val binding get() = _binding!!
    private lateinit var latenessAdapter: NotificationAdapter
    private val latenessHistory = mutableListOf<Borrowing>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotifikasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadLatenessHistory()
        loadCurrentBorrowingSummary()
    }

    private fun setupRecyclerView() {
        latenessAdapter = NotificationAdapter(latenessHistory) { borrowing ->
            showLatenessDetails(borrowing)
        }
        binding.recyclerViewLateness.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = latenessAdapter
        }
    }

    private fun setupClickListeners() {
        binding.chipOverdue.setOnClickListener {
            loadCurrentlyOverdue()
        }

        binding.chipReturned.setOnClickListener {
            loadReturnedLateBooks()
        }

        binding.chipAll.setOnClickListener {
            loadLatenessHistory()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadLatenessHistory()
            loadCurrentBorrowingSummary()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun loadCurrentBorrowingSummary() {
        // Load active borrowings count
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                val activeBorrowingsCount = snapshot.size()
                if (isAdded && context != null) {
                    binding.textCurrentBorrowing.text = activeBorrowingsCount.toString()
                }
            }
            .addOnFailureListener { error ->
                if (isAdded && context != null) {
                    binding.textCurrentBorrowing.text = "0"
                }
            }
    }

    private fun loadLatenessHistory() {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                latenessHistory.clear()
                val currentTime = System.currentTimeMillis()

                snapshot?.documents?.forEach { document ->
                    val borrowing = document.toObject(Borrowing::class.java)
                    borrowing?.let {
                        when {
                            it.status == "overdue" -> latenessHistory.add(it)
                            it.status == "returned_late" -> latenessHistory.add(it)
                            it.status == "active" && it.dueDate < currentTime -> {
                                updateBorrowingStatus(it.id, "overdue")
                                latenessHistory.add(it.copy(status = "overdue"))
                            }
                        }
                    }
                }

                latenessHistory.sortByDescending { it.dueDate }

                if (isAdded && context != null) {
                    latenessAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    updateLatenessSummary()
                }
            }
    }

    private fun updateBorrowingStatus(borrowingId: String, status: String) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .document(borrowingId)
            .update("status", status)
            .addOnFailureListener {
                // Handle error silently
            }
    }

    private fun loadCurrentlyOverdue() {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("status", "overdue")
            .get()
            .addOnSuccessListener { snapshot ->
                latenessHistory.clear()
                snapshot.documents.forEach { document ->
                    val borrowing = document.toObject(Borrowing::class.java)
                    borrowing?.let { latenessHistory.add(it) }
                }
                if (isAdded && context != null) {
                    latenessHistory.sortByDescending { it.dueDate }
                    latenessAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    updateLatenessSummary()
                }
            }
            .addOnFailureListener { error ->
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadReturnedLateBooks() {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("status", "returned_late")
            .get()
            .addOnSuccessListener { snapshot ->
                latenessHistory.clear()
                snapshot.documents.forEach { document ->
                    val borrowing = document.toObject(Borrowing::class.java)
                    borrowing?.let { latenessHistory.add(it) }
                }
                if (isAdded && context != null) {
                    latenessHistory.sortByDescending { it.returnDate ?: it.dueDate }
                    latenessAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    updateLatenessSummary()
                }
            }
            .addOnFailureListener { error ->
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateLatenessSummary() {
        val currentTime = System.currentTimeMillis()
        var currentlyOverdueCount = 0

        latenessHistory.forEach { borrowing ->
            if (borrowing.status == "overdue" || 
                (borrowing.status == "active" && borrowing.dueDate < currentTime)) {
                currentlyOverdueCount++
            }
        }

        // Update the current overdue count display (just the number)
        binding.textCurrentOverdue.text = currentlyOverdueCount.toString()
        
        // Update total count in the notification section
        binding.textTotalLate.text = "Total: ${latenessHistory.size}"
    }

    private fun showLatenessDetails(borrowing: Borrowing) {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val latenessInfo = when {
            borrowing.status == "overdue" -> {
                val daysLate = ((currentTime - borrowing.dueDate) / (24 * 60 * 60 * 1000)).toInt()
                "Terlambat $daysLate hari (sejak ${dateFormat.format(Date(borrowing.dueDate))})"
            }
            borrowing.status == "returned_late" && borrowing.returnDate != null && borrowing.returnDate > 0 -> {
                val daysLate = ((borrowing.returnDate - borrowing.dueDate) / (24 * 60 * 60 * 1000)).toInt()
                "Dikembalikan terlambat $daysLate hari (${dateFormat.format(Date(borrowing.returnDate))})"
            }
            else -> "Data keterlambatan tidak tersedia"
        }

        if (isAdded && context != null) {
            Toast.makeText(
                requireContext(),
                "${borrowing.borrowerName} - ${borrowing.bookTitle}\n$latenessInfo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateEmptyState() {
        if (latenessHistory.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewLateness.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewLateness.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
