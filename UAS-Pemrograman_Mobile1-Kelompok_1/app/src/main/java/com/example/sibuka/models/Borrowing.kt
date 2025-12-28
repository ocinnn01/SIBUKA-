package com.example.sibuka.models

data class Borrowing(
    val id: String = "",
    val userEmail: String = "",
    val bookId: String = "",
    val borrowDate: Long = 0L,
    val dueDate: Long = 0L,
    val returnDate: Long? = null,
    val status: String = "active",
    val fineAmount: Double = 0.0,
    val borrowerName: String = "",
    val borrowerNim: String = "",
    val borrowerClass: String = "",
    val bookTitle: String = "",
    val adminId: String = ""
) {
    constructor() : this("", "", "", 0, 0, null, "active", 0.0, "", "", "", "", "")
}
