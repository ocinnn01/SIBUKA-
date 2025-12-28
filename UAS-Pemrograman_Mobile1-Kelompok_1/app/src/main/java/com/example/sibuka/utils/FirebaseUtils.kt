package com.example.sibuka.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseUtils {
    private const val TAG = "FirebaseUtils"

    val auth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Auth: ${e.message}")
            FirebaseAuth.getInstance()
        }
    }

    val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Storage: ${e.message}")
            FirebaseStorage.getInstance()
        }
    }

    const val COLLECTION_ADMIN_USERS = "admin_users"
    const val COLLECTION_BOOKS = "books"
    const val COLLECTION_BORROWINGS = "borrowings"
    const val STORAGE_BOOK_COVERS = "book_covers"
}
