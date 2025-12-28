package com.example.sibuka.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val registrationDate: Long = 0L,
    val status: String = "active"
) {
    constructor() : this("", "", "", "", 0L, "active")
}
