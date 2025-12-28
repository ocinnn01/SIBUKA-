package com.example.sibuka.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

object FirebaseStorageHelper {

    private const val TAG = "FirebaseStorageHelper"
    private const val BOOK_COVERS_FOLDER = "book_covers"

    suspend fun uploadBookImage(uri: Uri, bookId: String): Result<String> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val timestamp = System.currentTimeMillis()
            val fileName = "book_${bookId}_$timestamp.jpg"

            ensureFolderExists(storage, BOOK_COVERS_FOLDER)

            val imageRef = storage.reference.child("$BOOK_COVERS_FOLDER/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploaded_by", "sibuka_app")
                .setCustomMetadata("upload_time", timestamp.toString())
                .build()

            Log.d(TAG, "Uploading image to: $BOOK_COVERS_FOLDER/$fileName")

            val uploadTask = imageRef.putFile(uri, metadata).await()
            Log.d(TAG, "Upload successful: ${uploadTask.metadata}")

            val downloadUrl = imageRef.downloadUrl.await()
            Log.d(TAG, "Download URL obtained: $downloadUrl")

            Result.success(downloadUrl.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("User does not have permission") == true ||
                e.message?.contains("storage.unauthorized") == true ->
                    "Tidak ada izin untuk mengupload. Pastikan Anda sudah login dan Firebase Storage Rules sudah benar."

                e.message?.contains("object-not-found") == true ||
                e.message?.contains("does not exist") == true ->
                    "Folder storage tidak ditemukan. Folder akan dibuat otomatis."

                e.message?.contains("network") == true ->
                    "Masalah koneksi internet. Periksa koneksi Anda."

                e.message?.contains("storage quota exceeded") == true ->
                    "Kuota storage Firebase habis."

                e.message?.contains("invalid") == true ->
                    "Format gambar tidak valid. Gunakan JPG atau PNG."

                else -> "Gagal mengupload gambar: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    private suspend fun ensureFolderExists(storage: FirebaseStorage, folderName: String) {
        try {
            val folderRef = storage.reference.child(folderName)

            folderRef.listAll().await()
            Log.d(TAG, "Folder $folderName already exists")

        } catch (e: Exception) {
            Log.d(TAG, "Folder $folderName not found, creating...")

            val placeholderRef = storage.reference.child("$folderName/.placeholder")
            val placeholderData = "folder_init".toByteArray()

            try {
                placeholderRef.putBytes(placeholderData).await()
                Log.d(TAG, "Folder $folderName created")

                placeholderRef.delete().await()
                Log.d(TAG, "Placeholder removed")

            } catch (createError: Exception) {
                Log.w(TAG, "Failed to create folder: ${createError.message}")
            }
        }
    }

    suspend fun deleteBookImage(imageUrl: String): Result<Unit> {
        return try {
            if (imageUrl.isEmpty()) {
                return Result.success(Unit)
            }

            val storage = FirebaseStorage.getInstance()
            val imageRef = storage.getReferenceFromUrl(imageUrl)

            imageRef.delete().await()
            Log.d(TAG, "Image deleted successfully: $imageUrl")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun checkStorageConfiguration(): Boolean {
        return try {
            val storage = FirebaseStorage.getInstance()
            val testRef = storage.reference.child("test_connection")

            val testData = "test".toByteArray()
            testRef.putBytes(testData).await()

            testRef.delete().await()

            Log.d(TAG, "Storage configuration OK")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Storage configuration error: ${e.message}")
            false
        }
    }
}
