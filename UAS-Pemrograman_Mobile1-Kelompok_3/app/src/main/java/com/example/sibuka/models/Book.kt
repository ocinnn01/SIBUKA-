package com.example.sibuka.models

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val category: String = "",
    val stock: Int = 0,
    val description: String = "",
    val imageUrl: String = "",
    val publisher: String = "",
    val publicationYear: Int = 0,
    val location: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    constructor() : this("", "", "", "", "", 0, "", "", "", 0, "", 0L, 0L)
}
