package com.example.sibuka.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sibuka.R
import com.example.sibuka.databinding.ItemBookBinding
import com.example.sibuka.models.Book

class BookAdapter(
    private val allBooks: MutableList<Book>,
    private val onBookClick: (Book) -> Unit,
    private val onFilterResult: (Boolean) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>(), Filterable {

    private var filteredBooks: MutableList<Book> = allBooks.toMutableList()

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.tvTitle.text = book.title
            binding.tvAuthor.text = book.author
            binding.tvCategory.text = book.category
            binding.tvStock.text = book.stock.toString()
            binding.tvPublisher.text = if (book.publicationYear > 0) {
                "${book.publisher} • ${book.publicationYear}"
            } else {
                book.publisher
            }
            binding.tvLocation.text = book.location.ifEmpty { "Tidak diketahui" }
            if (book.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(book.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.ivBookCover)
            } else {
                binding.ivBookCover.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(filteredBooks[position])
    }

    override fun getItemCount(): Int = filteredBooks.size

    fun getCurrentBooks(): List<Book> {
        return filteredBooks.toList()
    }

    fun updateBooks(newBooks: List<Book>) {
        allBooks.clear()
        allBooks.addAll(newBooks)
        filteredBooks.clear()
        filteredBooks.addAll(newBooks)
        notifyDataSetChanged()
    }

    fun clearFilter() {
        filteredBooks.clear()
        filteredBooks.addAll(allBooks)
        notifyDataSetChanged()
        onFilterResult(filteredBooks.isNotEmpty())
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchText = constraint?.toString()?.lowercase()?.trim()

                filteredBooks = if (searchText.isNullOrEmpty()) {
                    allBooks.toMutableList()
                } else {
                    allBooks.filter { book ->
                        book.title.lowercase().contains(searchText) ||
                        book.author.lowercase().contains(searchText) ||
                        book.category.lowercase().contains(searchText) ||
                        book.publisher.lowercase().contains(searchText) ||
                        book.location.lowercase().contains(searchText)
                    }.toMutableList()
                }

                return FilterResults().apply {
                    values = filteredBooks
                    count = filteredBooks.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                filteredBooks = (results?.values as? MutableList<Book>) ?: mutableListOf()
                notifyDataSetChanged()
                onFilterResult(filteredBooks.isNotEmpty())
            }
        }
    }
}
