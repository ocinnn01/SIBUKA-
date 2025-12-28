package com.example.sibuka.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sibuka.R
import com.example.sibuka.adapters.BookAdapter
import com.example.sibuka.databinding.ActivityManageBooksBinding
import com.example.sibuka.dialogs.AddBookDialog
import com.example.sibuka.models.Book
import com.google.firebase.database.*

class ManageBooksActivity : AppCompatActivity(), AddBookDialog.OnBookAddedListener {

    private lateinit var binding: ActivityManageBooksBinding
    private lateinit var bookAdapter: BookAdapter
    private lateinit var database: DatabaseReference
    private val books = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupDatabase()
        setupFab()
        setupSearch()
        loadBooks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Kelola Buku"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            books,
            onBookClick = { book -> editBook(book) },
            onFilterResult = { hasResults -> handleSearchResults(hasResults) }
        )

        binding.recyclerViewBooks.apply {
            layoutManager = LinearLayoutManager(this@ManageBooksActivity)
            adapter = bookAdapter
        }
    }

    private fun setupDatabase() {
        database = FirebaseDatabase.getInstance().getReference("books")
    }

    private fun setupFab() {
        binding.fabAddBook.setOnClickListener {
            showAddBookDialog()
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }
        })

        binding.searchView.setOnCloseListener {
            clearSearch()
            false
        }
    }

    private fun performSearch(query: String?) {
        bookAdapter.filter.filter(query)
    }

    private fun clearSearch() {
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        bookAdapter.clearFilter()
    }

    private fun handleSearchResults(hasResults: Boolean) {
        if (hasResults) {
            // Ada hasil pencarian - tampilkan RecyclerView, sembunyikan empty state
            binding.recyclerViewBooks.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        } else {
            // Tidak ada hasil - tampilkan empty state, sembunyikan RecyclerView
            binding.recyclerViewBooks.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE

            // Update pesan berdasarkan apakah sedang mencari atau tidak
            val searchQuery = binding.searchView.query.toString()
            if (searchQuery.isNotEmpty()) {
                // Sedang mencari tapi tidak ada hasil
                binding.textViewEmpty.text = "Tidak ada hasil yang sesuai"
            } else {
                // Tidak sedang mencari dan tidak ada data
                binding.textViewEmpty.text = "Belum ada buku"
            }
        }
    }

    private fun showAddBookDialog() {
        val dialog = AddBookDialog()
        dialog.setOnBookAddedListener(this)
        dialog.show(supportFragmentManager, "AddBookDialog")
    }

    private fun loadBooks() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newBooks = mutableListOf<Book>()
                for (bookSnapshot in snapshot.children) {
                    val book = bookSnapshot.getValue(Book::class.java)
                    book?.let { newBooks.add(it) }
                }

                bookAdapter.updateBooks(newBooks)
                val currentQuery = binding.searchView.query.toString()
                if (currentQuery.isNotEmpty()) {
                    performSearch(currentQuery)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.recyclerViewBooks.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.textViewEmpty.text = "Gagal memuat data buku"
            }
        })
    }

    private fun editBook(book: Book) {
        val dialog = AddBookDialog.newInstance(book)
        dialog.setOnBookAddedListener(this)
        dialog.show(supportFragmentManager, "EditBookDialog")
    }

    override fun onBookAdded(book: Book) {
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_manage_books, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_search -> {
                binding.searchView.requestFocus()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (!binding.searchView.isIconified) {
            clearSearch()
        } else {
            super.onBackPressed()
        }
    }
}
