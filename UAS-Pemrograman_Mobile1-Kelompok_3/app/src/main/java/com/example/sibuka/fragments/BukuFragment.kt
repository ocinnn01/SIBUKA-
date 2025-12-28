package com.example.sibuka.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sibuka.adapters.BookAdapter
import com.example.sibuka.databinding.FragmentBukuBinding
import com.example.sibuka.dialogs.AddBookDialog
import com.example.sibuka.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class BukuFragment : Fragment() {
    private var _binding: FragmentBukuBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookAdapter: BookAdapter
    private val books = mutableListOf<Book>()
    private val allBooksOriginal = mutableListOf<Book>()
    private val firestore = FirebaseFirestore.getInstance()
    private var booksListener: ListenerRegistration? = null
    private var currentSelectedCategory: String? = null

    companion object {
        private const val TAG = "BukuFragment"
        private const val COLLECTION_BOOKS = "books"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBukuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            books,
            onBookClick = { book -> showBookOptionsDialog(book) },
            onFilterResult = { hasResults -> handleSearchResults(hasResults) }
        )
        binding.rvBooks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookAdapter
        }
    }

    private fun loadBooks() {
        Log.d(TAG, "Loading books from Firestore...")

        booksListener?.remove()

        booksListener = firestore.collection(COLLECTION_BOOKS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading books: ${error.message}", error)
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Error loading books: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                Log.d(TAG, "Snapshot received. Documents count: ${snapshot?.documents?.size ?: 0}")

                val newBooks = mutableListOf<Book>()
                snapshot?.documents?.forEach { document ->
                    try {
                        Log.d(TAG, "Processing document: ${document.id}")
                        Log.d(TAG, "Document data: ${document.data}")

                        val book = document.toObject(Book::class.java)
                        if (book != null) {
                            Log.d(TAG, "Successfully parsed book: ${book.title}")
                            newBooks.add(book)
                        } else {
                            Log.w(TAG, "Failed to parse document ${document.id} to Book object")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing book document ${document.id}: ${e.message}", e)
                    }
                }

                newBooks.sortBy { it.title }

                Log.d(TAG, "Total books loaded: ${newBooks.size}")

                if (isAdded && context != null) {
                    books.clear()
                    books.addAll(newBooks)
                    allBooksOriginal.clear()
                    allBooksOriginal.addAll(newBooks)
                    if (currentSelectedCategory != null) {
                        filterBooksByCategory(currentSelectedCategory)
                    } else {
                        bookAdapter.updateBooks(newBooks)
                        updateBookCount(newBooks.size)
                    }

                    Log.d(TAG, "BookAdapter updated with ${newBooks.size} books")
                    updateEmptyState()
                } else {
                    Log.w(TAG, "Fragment not added or context is null, skipping UI update")
                }
            }
    }

    private fun updateBookCount(count: Int) {
        try {
            val countText = if (count == 0) {
                "Belum ada buku"
            } else {
                "Total: $count buku ${if (count > 0) "aktif" else ""}"
            }
            binding.tvBookCount.text = countText
            Log.d(TAG, "Updated book count display: $count")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating book count display: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        binding.btnAddBook.setOnClickListener {
            showAddBookDialog()
        }

        binding.btnAddBookEmpty.setOnClickListener {
            showAddBookDialog()
        }

        binding.btnFilterCategory.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.btnSortBy.setOnClickListener {
            showSortDialog()
        }

        binding.btnFilterCategory.setOnLongClickListener {
            resetCategoryFilter()
            true
        }

        binding.btnSortBy.setOnLongClickListener {
            resetSort()
            true
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.trim()
                performSearch(query)
            }
        })
    }

    private fun performSearch(query: String?) {
        bookAdapter.filter.filter(query)
    }

    private fun handleSearchResults(hasResults: Boolean) {
        val searchQuery = binding.etSearch.text.toString().trim()

        if (hasResults) {
            binding.rvBooks.visibility = View.VISIBLE

            try {
                val emptyView = binding.root.findViewById<View>(android.R.id.empty)
                emptyView?.visibility = View.GONE
            } catch (e: Exception) {

            }
        } else {
            if (searchQuery.isNotEmpty()) {
                binding.rvBooks.visibility = View.GONE
                showEmptySearchResult()
            } else {
                binding.rvBooks.visibility = View.VISIBLE
            }
        }
    }

    private fun showEmptySearchResult() {
        if (isAdded && context != null) {
            Toast.makeText(context, "Tidak ada hasil yang sesuai", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddBookDialog() {
        if (!isAdded || context == null) return

        val addBookDialog = AddBookDialog()
        addBookDialog.setOnBookAddedListener(object : AddBookDialog.OnBookAddedListener {
            override fun onBookAdded(book: Book) {
                Log.d(TAG, "Book added callback received: ${book.title}")
            }
        })
        addBookDialog.show(parentFragmentManager, "AddBookDialog")
    }

    private fun showCategoryFilterDialog() {
        if (!isAdded || context == null) return

        val categories = allBooksOriginal.map { it.category }.distinct().sorted().toMutableList()
        categories.add(0, "Semua Kategori")

        val categoryArray = categories.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Berdasarkan Kategori")
            .setItems(categoryArray) { _, which ->
                val selectedCategory = if (which == 0) null else categoryArray[which]
                filterBooksByCategory(selectedCategory)

                binding.btnFilterCategory.text = selectedCategory ?: "Kategori"
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showSortDialog() {
        if (!isAdded || context == null) return

        val sortOptions = arrayOf(
            "Judul A-Z",
            "Judul Z-A",
            "Penulis A-Z",
            "Penulis Z-A",
            "Tahun Terbaru",
            "Tahun Terlama",
            "Stok Terbanyak",
            "Stok Tersedikit"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Urutkan Berdasarkan")
            .setItems(sortOptions) { _, which ->
                sortBooks(which)

                binding.btnSortBy.text = sortOptions[which]
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun filterBooksByCategory(category: String?) {
        currentSelectedCategory = category

        val filteredBooks = if (category == null) {
            allBooksOriginal.toList()
        } else {
            allBooksOriginal.filter { it.category.equals(category, ignoreCase = true) }
        }

        bookAdapter.updateBooks(filteredBooks)
        updateBookCount(filteredBooks.size)

        binding.etSearch.text?.clear()
    }

    private fun sortBooks(sortType: Int) {
        val currentBooks = bookAdapter.getCurrentBooks().toMutableList()

        when (sortType) {
            0 -> currentBooks.sortBy { it.title.lowercase() }
            1 -> currentBooks.sortByDescending { it.title.lowercase() }
            2 -> currentBooks.sortBy { it.author.lowercase() }
            3 -> currentBooks.sortByDescending { it.author.lowercase() }
            4 -> currentBooks.sortByDescending { it.publicationYear }
            5 -> currentBooks.sortBy { it.publicationYear }
            6 -> currentBooks.sortByDescending { it.stock }
            7 -> currentBooks.sortBy { it.stock }
        }

        bookAdapter.updateBooks(currentBooks)
    }

    private fun showBookOptionsDialog(book: Book) {
        if (!isAdded || context == null) return

        val options = arrayOf("Edit Buku", "Hapus Buku", "Lihat Detail")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Aksi untuk: ${book.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditBookDialog(book)
                    1 -> showDeleteConfirmation(book)
                    2 -> showBookDetails(book)
                }
            }
            .show()
    }

    private fun showEditBookDialog(book: Book) {
        if (!isAdded || context == null) return

        val editBookDialog = AddBookDialog()
        editBookDialog.setEditMode(book)
        editBookDialog.setOnBookAddedListener(object : AddBookDialog.OnBookAddedListener {
            override fun onBookAdded(updatedBook: Book) {
                Log.d(TAG, "Book edited callback received: ${updatedBook.title}")
                Toast.makeText(context, "Buku berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
        })
        editBookDialog.show(parentFragmentManager, "EditBookDialog")
    }

    private fun showDeleteConfirmation(book: Book) {
        if (!isAdded || context == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Buku")
            .setMessage("Apakah Anda yakin ingin menghapus buku '${book.title}'?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBook(book)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteBook(book: Book) {
        Log.d(TAG, "Deleting book: ${book.title}")

        firestore.collection(COLLECTION_BOOKS)
            .document(book.id)
            .delete()
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Buku '${book.title}' berhasil dihapus", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Book deleted successfully")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting book: ${exception.message}", exception)
                if (isAdded && context != null) {
                    Toast.makeText(context, "Gagal menghapus buku: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showBookDetails(book: Book) {
        if (!isAdded || context == null) return

        val message = """
            Judul: ${book.title}
            Penulis: ${book.author}
            Penerbit: ${book.publisher}
            Tahun: ${book.publicationYear}
            Kategori: ${book.category}
            Stok: ${book.stock}
            Lokasi: ${book.location}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Buku")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateEmptyState() {
        if (books.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvBooks.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvBooks.visibility = View.VISIBLE
        }
    }

    private fun resetCategoryFilter() {
        currentSelectedCategory = null
        filterBooksByCategory(null)
        binding.btnFilterCategory.text = "Kategori"
        Toast.makeText(context, "Filter kategori direset", Toast.LENGTH_SHORT).show()
    }

    private fun resetSort() {
        sortBooks(0)
        binding.btnSortBy.text = "Urutkan"
        Toast.makeText(context, "Pengurutan direset", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        booksListener?.remove()
        _binding = null
    }
}
