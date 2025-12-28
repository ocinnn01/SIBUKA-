package com.example.sibuka.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sibuka.databinding.FragmentPinjamBinding
import com.example.sibuka.models.Book
import com.example.sibuka.models.Borrowing
import com.example.sibuka.utils.FirebaseUtils
import java.util.*

class PinjamFragment : Fragment() {
    private var _binding: FragmentPinjamBinding? = null
    private val binding get() = _binding!!
    private val availableBooks = mutableListOf<Book>()
    private var selectedBook: Book? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinjamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadAvailableBooks()
        setupBookSpinner()
    }

    private fun setupClickListeners() {
        binding.btnBorrow.setOnClickListener {
            processBorrow()
        }

        binding.btnReturn.setOnClickListener {
            processReturn()
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnToggleBorrow.id -> {
                        showBorrowSection()
                    }
                    binding.btnToggleReturn.id -> {
                        showReturnSection()
                    }
                }
            }
        }

        binding.btnToggleBorrow.isChecked = true
        showBorrowSection()
    }

    private fun showBorrowSection() {
        binding.layoutBorrow.visibility = View.VISIBLE
        binding.layoutReturn.visibility = View.GONE
    }

    private fun showReturnSection() {
        binding.layoutBorrow.visibility = View.GONE
        binding.layoutReturn.visibility = View.VISIBLE
    }

    private fun loadAvailableBooks() {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BOOKS)
            .whereGreaterThan("stock", 0)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded || context == null) return@addOnSuccessListener

                availableBooks.clear()
                snapshot.documents.forEach { document ->
                    val book = document.toObject(Book::class.java)
                    book?.let { availableBooks.add(it) }
                }
                setupBookSpinner()
            }
            .addOnFailureListener { error ->
                if (isAdded && context != null) {
                    Toast.makeText(context, "Error loading books: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupBookSpinner() {
        if (!isAdded || context == null) return

        val bookTitles = availableBooks.map { "${it.title} (Stok: ${it.stock})" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, bookTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBooks.adapter = adapter

        binding.spinnerBooks.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < availableBooks.size) {
                    selectedBook = availableBooks[position]
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedBook = null
            }
        })
    }

    private fun processBorrow() {
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val borrowerNim = binding.etBorrowerNim.text.toString().trim()
        val borrowerClass = binding.etBorrowerClass.text.toString().trim()

        if (borrowerName.isEmpty() || borrowerNim.isEmpty() || borrowerClass.isEmpty()) {
            Toast.makeText(context, "Mohon isi semua data peminjam", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedBook == null) {
            Toast.makeText(context, "Mohon pilih buku", Toast.LENGTH_SHORT).show()
            return
        }

        val book = selectedBook!!
        if (book.stock <= 0) {
            Toast.makeText(context, "Stok buku habis", Toast.LENGTH_SHORT).show()
            return
        }

        checkActiveBorrowing(borrowerNim) { hasActive ->
            if (hasActive) {
                Toast.makeText(context, "Peminjam masih memiliki buku yang belum dikembalikan", Toast.LENGTH_SHORT).show()
            } else {
                createBorrowing(borrowerName, borrowerNim, borrowerClass, book)
            }
        }
    }

    private fun checkActiveBorrowing(nim: String, callback: (Boolean) -> Unit) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("borrowerNim", nim)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { activeSnapshot ->
                if (activeSnapshot.documents.isNotEmpty()) {
                    callback(true)
                } else {
                    FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
                        .whereEqualTo("borrowerNim", nim)
                        .whereEqualTo("status", "overdue")
                        .get()
                        .addOnSuccessListener { overdueSnapshot ->
                            val hasOverdueBorrowing = overdueSnapshot.documents.isNotEmpty()
                            callback(hasOverdueBorrowing)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun createBorrowing(name: String, nim: String, kelas: String, book: Book) {
        val borrowingId = UUID.randomUUID().toString()
        val currentUser = FirebaseUtils.auth.currentUser

        val borrowing = Borrowing(
            id = borrowingId,
            borrowerName = name,
            borrowerNim = nim,
            borrowerClass = kelas,
            bookId = book.id,
            bookTitle = book.title,
            borrowDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
            status = "active",
            adminId = currentUser?.uid ?: ""
        )

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .document(borrowingId)
            .set(borrowing)
            .addOnSuccessListener {
                // Update book stock
                updateBookStock(book.id, book.stock - 1) {
                    Toast.makeText(context, "Peminjaman berhasil dicatat", Toast.LENGTH_SHORT).show()
                    clearBorrowForm()
                    loadAvailableBooks()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processReturn() {
        val nim = binding.etReturnNim.text.toString().trim()

        if (nim.isEmpty()) {
            Toast.makeText(context, "Mohon isi NIM peminjam", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("borrowerNim", nim)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isEmpty()) {
                    Toast.makeText(context, "Tidak ada peminjaman aktif untuk NIM tersebut", Toast.LENGTH_SHORT).show()
                } else {
                    val borrowing = snapshot.documents.first().toObject(Borrowing::class.java)
                    borrowing?.let { processBookReturn(it) }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processBookReturn(borrowing: Borrowing) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .document(borrowing.id)
            .update(
                mapOf(
                    "status" to "returned",
                    "returnDate" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BOOKS)
                    .document(borrowing.bookId)
                    .get()
                    .addOnSuccessListener { document ->
                        val book = document.toObject(Book::class.java)
                        book?.let {
                            updateBookStock(it.id, it.stock + 1) {
                                Toast.makeText(context, "Pengembalian berhasil dicatat", Toast.LENGTH_SHORT).show()
                                clearReturnForm()
                                loadAvailableBooks()
                            }
                        }
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBookStock(bookId: String, newStock: Int, onSuccess: () -> Unit) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BOOKS)
            .document(bookId)
            .update("stock", newStock)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error updating stock: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearBorrowForm() {
        binding.etBorrowerName.text?.clear()
        binding.etBorrowerNim.text?.clear()
        binding.etBorrowerClass.text?.clear()
        binding.spinnerBooks.setSelection(0)
    }

    private fun clearReturnForm() {
        binding.etReturnNim.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
