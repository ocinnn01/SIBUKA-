package com.example.sibuka.models

data class AdminUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "admin",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "admin", 0)
}
