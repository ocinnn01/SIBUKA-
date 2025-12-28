package com.example.sibuka.dialogs

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sibuka.R
import com.example.sibuka.databinding.DialogAddBookBinding
import com.example.sibuka.models.Book
import com.google.firebase.firestore.FirebaseFirestore

class AddBookDialog : DialogFragment() {

    private var _binding: DialogAddBookBinding? = null
    private val binding get() = _binding!!

    private var listener: OnBookAddedListener? = null
    private var editBook: Book? = null
    private val firestore = FirebaseFirestore.getInstance()

    interface OnBookAddedListener {
        fun onBookAdded(book: Book)
    }

    fun setOnBookAddedListener(listener: OnBookAddedListener) {
        this.listener = listener
    }

    fun setEditMode(book: Book) {
        this.editBook = book
    }

    companion object {
        private const val TAG = "AddBookDialog"
        private const val COLLECTION_BOOKS = "books"

        fun newInstance(book: Book? = null): AddBookDialog {
            val dialog = AddBookDialog()
            dialog.editBook = book
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupImageUrlWatcher()

        if (editBook != null) {
            binding.tvDialogTitle.text = "Edit Buku"
            binding.btnSave.text = "Update Buku"
            populateFields(editBook!!)
        } else {
            binding.tvDialogTitle.text = "Tambah Buku Baru"
            binding.btnSave.text = "Simpan Buku"
        }

        testFirestoreConnection()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.95).toInt()
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.setLayout(width, height)
        }
    }

    private fun testFirestoreConnection() {
        Log.d(TAG, "Testing Firestore connection...")

        val testData = hashMapOf(
            "test" to "connection_test_${System.currentTimeMillis()}"
        )

        firestore.collection("test").add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Firestore connection successful. Test document: ${documentReference.id}")
                documentReference.delete()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firestore connection failed: ${exception.message}")
            }
    }

    private fun populateFields(book: Book) {
        binding.etTitle.setText(book.title)
        binding.etAuthor.setText(book.author)
        binding.etCategory.setText(book.category)
        binding.etPublisher.setText(book.publisher)
        binding.etPublicationYear.setText(if (book.publicationYear > 0) book.publicationYear.toString() else "")
        binding.etStock.setText(if (book.stock > 0) book.stock.toString() else "")
        binding.etLocation.setText(book.location)
        binding.etDescription.setText(book.description)

        if (book.imageUrl.isNotEmpty()) {
            binding.etImageUrl.setText(book.imageUrl)
            loadImageFromUrl(book.imageUrl)
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveBook()
        }

        binding.btnPreviewImage.setOnClickListener {
            val imageUrl = binding.etImageUrl.text.toString().trim()
            if (imageUrl.isNotEmpty()) {
                previewImageFromUrl(imageUrl)
            } else {
                Toast.makeText(context, "Masukkan link gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupImageUrlWatcher() {
        binding.etImageUrl.doOnTextChanged { text, _, _, _ ->
            val url = text.toString().trim()
            if (url.isNotEmpty() && isValidImageUrl(url)) {
                binding.etImageUrl.postDelayed({
                    if (binding.etImageUrl.text.toString().trim() == url) {
                        loadImageFromUrl(url)
                    }
                }, 1000)
            }
        }
    }

    private fun previewImageFromUrl(url: String) {
        if (!isValidImageUrl(url)) {
            Toast.makeText(context, "Link gambar tidak valid. Gunakan format JPG, PNG, atau WebP", Toast.LENGTH_SHORT).show()
            return
        }

        loadImageFromUrl(url)
        Toast.makeText(context, "Memuat preview gambar...", Toast.LENGTH_SHORT).show()
    }

    private fun loadImageFromUrl(url: String) {
        if (!isAdded) return

        try {
            Glide.with(this)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivBookCover)

            Log.d(TAG, "Loading image from URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from URL: ${e.message}")
            binding.ivBookCover.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun isValidImageUrl(url: String): Boolean {
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return false
        }

        val lowerUrl = url.lowercase()
        return lowerUrl.contains(".jpg") ||
               lowerUrl.contains(".jpeg") ||
               lowerUrl.contains(".png") ||
               lowerUrl.contains(".webp") ||
               lowerUrl.contains("image") ||
               lowerUrl.contains("photo")
    }

    private fun saveBook() {
        Log.d(TAG, "saveBook() called")

        // Get and validate input fields
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val publisher = binding.etPublisher.text.toString().trim()
        val publicationYearStr = binding.etPublicationYear.text.toString().trim()
        val stockStr = binding.etStock.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()

        Log.d(TAG, "Input data - Title: '$title', Author: '$author', Category: '$category'")

        // Clear previous errors
        binding.etTitle.error = null
        binding.etAuthor.error = null
        binding.etCategory.error = null
        binding.etStock.error = null
        binding.etImageUrl.error = null

        var hasError = false

        if (title.isEmpty()) {
            binding.etTitle.error = "Judul buku tidak boleh kosong"
            binding.etTitle.requestFocus()
            hasError = true
        }

        if (author.isEmpty()) {
            binding.etAuthor.error = "Penulis tidak boleh kosong"
            if (!hasError) binding.etAuthor.requestFocus()
            hasError = true
        }

        if (category.isEmpty()) {
            binding.etCategory.error = "Kategori tidak boleh kosong"
            if (!hasError) binding.etCategory.requestFocus()
            hasError = true
        }

        val publicationYear = publicationYearStr.toIntOrNull() ?: 0
        val stock = stockStr.toIntOrNull() ?: 0

        if (stock <= 0) {
            binding.etStock.error = "Stok harus lebih dari 0"
            if (!hasError) binding.etStock.requestFocus()
            hasError = true
        }

        if (imageUrl.isNotEmpty() && !isValidImageUrl(imageUrl)) {
            binding.etImageUrl.error = "Link gambar tidak valid"
            if (!hasError) binding.etImageUrl.requestFocus()
            hasError = true
        }

        if (hasError) {
            Log.w(TAG, "Validation failed")
            Toast.makeText(context, "Mohon periksa kembali data yang diisi", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "All validations passed")

        setLoadingState(true)

        try {
            // Generate book ID for edit mode or create new document reference
            val bookId = editBook?.id ?: firestore.collection(COLLECTION_BOOKS).document().id

            Log.d(TAG, "Book ID: $bookId")

            val book = Book(
                id = bookId,
                title = title,
                author = author,
                isbn = "",
                category = category,
                publisher = publisher,
                publicationYear = publicationYear,
                description = description,
                stock = stock,
                location = location,
                imageUrl = imageUrl,
                createdAt = editBook?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Created book object: $book")
            saveBookToFirestore(book)

        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveBook: ${e.message}", e)
            setLoadingState(false)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveBookToFirestore(book: Book) {
        Log.d(TAG, "Saving book to Firestore: ${book.title}")
        val bookData = hashMapOf(
            "id" to book.id,
            "title" to book.title,
            "author" to book.author,
            "isbn" to book.isbn,
            "category" to book.category,
            "publisher" to book.publisher,
            "publicationYear" to book.publicationYear,
            "description" to book.description,
            "stock" to book.stock,
            "location" to book.location,
            "imageUrl" to book.imageUrl,
            "createdAt" to book.createdAt,
            "updatedAt" to book.updatedAt
        )

        firestore.collection(COLLECTION_BOOKS)
            .document(book.id)
            .set(bookData)
            .addOnSuccessListener {
                Log.d(TAG, "Book saved successfully to Firestore")

                if (isAdded && context != null) {
                    val message = if (editBook != null) "Buku berhasil diperbarui" else "Buku berhasil ditambahkan"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                    listener?.onBookAdded(book)
                    dismiss()
                } else {
                    Log.w(TAG, "Fragment not added or context is null")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save book to Firestore: ${exception.message}", exception)

                setLoadingState(false)

                if (isAdded && context != null) {
                    val errorMsg = when {
                        exception.message?.contains("Permission denied") == true ->
                            "Tidak ada izin untuk menyimpan data. Periksa aturan Firestore."
                        exception.message?.contains("network") == true ->
                            "Masalah koneksi internet. Periksa koneksi dan coba lagi."
                        exception.message?.contains("PERMISSION_DENIED") == true ->
                            "Akses ditolak. Periksa aturan keamanan Firestore."
                        else -> "Gagal menyimpan buku: ${exception.message}"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (!isAdded || _binding == null) return

        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.btnSave.text = if (isLoading) "Menyimpan..." else "Simpan"
        binding.etTitle.isEnabled = !isLoading
        binding.etAuthor.isEnabled = !isLoading
        binding.etCategory.isEnabled = !isLoading
        binding.etPublisher.isEnabled = !isLoading
        binding.etPublicationYear.isEnabled = !isLoading
        binding.etStock.isEnabled = !isLoading
        binding.etLocation.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
        binding.etImageUrl.isEnabled = !isLoading
        binding.btnPreviewImage.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
