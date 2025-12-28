package com.example.sibuka.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sibuka.activities.LoginActivity
import com.example.sibuka.databinding.FragmentProfilBinding
import com.example.sibuka.databinding.DialogChangePasswordBinding
import com.example.sibuka.databinding.DialogEditProfileBinding
import com.example.sibuka.databinding.DialogExportDataBinding
import com.example.sibuka.models.AdminUser
import com.example.sibuka.models.Book
import com.example.sibuka.models.Borrowing
import com.example.sibuka.models.User
import com.example.sibuka.utils.FirebaseUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import android.net.Uri
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ProfilFragment : Fragment() {
    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private var adminUser: AdminUser? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadAdminData()
        loadStatistics()
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.cardEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.cardExportData.setOnClickListener {
            showExportDataDialog()
        }
    }

    private fun loadAdminData() {
        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser != null) {
            if (isAdded && _binding != null) {
                binding.textEmail.text = currentUser.email
            }

            FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_ADMIN_USERS)
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (isAdded && _binding != null) {
                        adminUser = document.toObject(AdminUser::class.java)
                        adminUser?.let {
                            binding.textName.text = it.name
                            binding.textRole.text = it.role.uppercase()
                        }
                    }
                }
                .addOnFailureListener { error ->
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loadStatistics() {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BOOKS)
            .get()
            .addOnSuccessListener { snapshot ->
                if (isAdded && _binding != null) {
                    val totalBooks = snapshot.documents.size
                    var totalStock = 0
                    snapshot.documents.forEach { document ->
                        val stock = document.getLong("stock")?.toInt() ?: 0
                        totalStock += stock
                    }
                    binding.textTotalBooks.text = totalBooks.toString()
                    binding.textTotalStock.text = totalStock.toString()
                }
            }

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                if (isAdded && _binding != null) {
                    binding.textActiveBorrowings.text = snapshot.documents.size.toString()
                }
            }

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .whereEqualTo("status", "overdue")
            .get()
            .addOnSuccessListener { snapshot ->
                if (isAdded && _binding != null) {
                    binding.textOverdueBorrowings.text = snapshot.documents.size.toString()
                }
            }

        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .get()
            .addOnSuccessListener { snapshot ->
                if (isAdded && _binding != null) {
                    val startOfMonth = getStartOfCurrentMonth()
                    var monthlyCount = 0

                    snapshot.documents.forEach { document ->
                        val borrowing = document.toObject(Borrowing::class.java)
                        borrowing?.let {
                            if (it.borrowDate >= startOfMonth) {
                                monthlyCount++
                            }
                        }
                    }

                    binding.textMonthlyBorrowings.text = monthlyCount.toString()
                }
            }
    }

    private fun getStartOfCurrentMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun showLogoutConfirmation() {
        if (!isAdded || context == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        if (!isAdded || context == null) return

        FirebaseUtils.auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showEditProfileDialog() {
        if (!isAdded || context == null) return

        val dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val roles = arrayOf("admin", "super_admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        dialogBinding.spinnerRole.setAdapter(adapter)

        adminUser?.let { user ->
            dialogBinding.etName.setText(user.name)
            dialogBinding.etEmail.setText(user.email)
            dialogBinding.spinnerRole.setText(user.role, false)
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etName.text.toString().trim()
            val role = dialogBinding.spinnerRole.text.toString().trim()

            if (name.isEmpty() || role.isEmpty()) {
                Toast.makeText(context, "Nama dan role tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProfile(name, role)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateProfile(name: String, role: String) {
        if (!isAdded || context == null) return

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser != null) {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "role" to role
            )

            FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_ADMIN_USERS)
                .document(currentUser.uid)
                .update(updates)
                .addOnSuccessListener {
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        loadAdminData()
                    }
                }
                .addOnFailureListener { error ->
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Gagal memperbarui profil: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showChangePasswordDialog() {
        if (!isAdded || context == null) return

        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnChangePassword.setOnClickListener {
            val currentPassword = dialogBinding.etCurrentPassword.text.toString().trim()
            val newPassword = dialogBinding.etNewPassword.text.toString().trim()
            val confirmPassword = dialogBinding.etNewPasswordConfirm.text.toString().trim()

            when {
                currentPassword.isEmpty() -> {
                    Toast.makeText(context, "Password saat ini harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(context, "Password baru harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newPassword.length < 6 -> {
                    Toast.makeText(context, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(context, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    changePassword(currentPassword, newPassword)
                    dialog.dismiss()
                }
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        if (!isAdded || context == null) return

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser?.email != null) {
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

            currentUser.reauthenticate(credential)
                .addOnSuccessListener {
                    currentUser.updatePassword(newPassword)
                        .addOnSuccessListener {
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { error ->
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Gagal mengubah password: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener { error ->
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Password saat ini salah: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showExportDataDialog() {
        val dialogBinding = DialogExportDataBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExport.setOnClickListener {
            val exportBooks = dialogBinding.chipBooks.isChecked
            val exportBorrowings = dialogBinding.chipBorrowings.isChecked
            val exportUsers = dialogBinding.chipUsers.isChecked
            val exportAsCSV = dialogBinding.chipCSV.isChecked

            if (!exportBooks && !exportBorrowings && !exportUsers) {
                Toast.makeText(context, "Please select at least one data type to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialogBinding.progressBar.visibility = View.VISIBLE
            dialogBinding.btnExport.isEnabled = false

            exportData(exportBooks, exportBorrowings, exportUsers, exportAsCSV, dialogBinding, dialog)
        }

        dialog.show()
    }

    private fun exportData(
        exportBooks: Boolean,
        exportBorrowings: Boolean,
        exportUsers: Boolean,
        exportAsCSV: Boolean,
        dialogBinding: DialogExportDataBinding,
        dialog: AlertDialog
    ) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val format = if (exportAsCSV) "csv" else "json"

        try {
            if (exportBooks) {
                exportBooksData(format, timestamp) { success ->
                    if (success) checkExportCompletion(exportBorrowings, exportUsers, format, timestamp, dialogBinding, dialog)
                    else showExportError(dialogBinding, dialog)
                }
            } else if (exportBorrowings) {
                exportBorrowingsData(format, timestamp) { success ->
                    if (success) checkExportCompletion(false, exportUsers, format, timestamp, dialogBinding, dialog)
                    else showExportError(dialogBinding, dialog)
                }
            } else if (exportUsers) {
                exportUsersData(format, timestamp) { success ->
                    if (success) showExportSuccess(dialogBinding, dialog)
                    else showExportError(dialogBinding, dialog)
                }
            }
        } catch (e: Exception) {
            showExportError(dialogBinding, dialog)
        }
    }

    private fun checkExportCompletion(
        exportBorrowings: Boolean,
        exportUsers: Boolean,
        format: String,
        timestamp: String,
        dialogBinding: DialogExportDataBinding,
        dialog: AlertDialog
    ) {
        if (exportBorrowings) {
            exportBorrowingsData(format, timestamp) { success ->
                if (success) {
                    if (exportUsers) {
                        exportUsersData(format, timestamp) { userSuccess ->
                            if (userSuccess) showExportSuccess(dialogBinding, dialog)
                            else showExportError(dialogBinding, dialog)
                        }
                    } else {
                        showExportSuccess(dialogBinding, dialog)
                    }
                } else {
                    showExportError(dialogBinding, dialog)
                }
            }
        } else if (exportUsers) {
            exportUsersData(format, timestamp) { success ->
                if (success) showExportSuccess(dialogBinding, dialog)
                else showExportError(dialogBinding, dialog)
            }
        } else {
            showExportSuccess(dialogBinding, dialog)
        }
    }

    private fun exportBooksData(format: String, timestamp: String, callback: (Boolean) -> Unit) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BOOKS)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val books = snapshot.documents.mapNotNull { it.toObject(Book::class.java) }
                    val fileName = "books_$timestamp.$format"
                    val success = if (format == "csv") {
                        createBooksCSV(books, fileName)
                    } else {
                        createBooksJSON(books, fileName)
                    }
                    callback(success)
                } catch (e: Exception) {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun exportBorrowingsData(format: String, timestamp: String, callback: (Boolean) -> Unit) {
        FirebaseUtils.firestore.collection(FirebaseUtils.COLLECTION_BORROWINGS)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val borrowings = snapshot.documents.mapNotNull { it.toObject(Borrowing::class.java) }
                    val fileName = "borrowings_$timestamp.$format"
                    val success = if (format == "csv") {
                        createBorrowingsCSV(borrowings, fileName)
                    } else {
                        createBorrowingsJSON(borrowings, fileName)
                    }
                    callback(success)
                } catch (e: Exception) {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun exportUsersData(format: String, timestamp: String, callback: (Boolean) -> Unit) {
        FirebaseUtils.firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val users = snapshot.documents.mapNotNull { doc ->
                        mapOf(
                            "id" to (doc.getString("id") ?: doc.id),
                            "name" to (doc.getString("name") ?: ""),
                            "email" to (doc.getString("email") ?: ""),
                            "phone" to (doc.getString("phone") ?: ""),
                            "registrationDate" to (doc.getLong("registrationDate") ?: 0L),
                            "status" to (doc.getString("status") ?: "active")
                        )
                    }
                    val fileName = "users_$timestamp.$format"
                    val success = if (format == "csv") {
                        createUsersCSV(users, fileName)
                    } else {
                        createUsersJSON(users, fileName)
                    }
                    callback(success)
                } catch (e: Exception) {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun createBooksCSV(books: List<Book>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            FileWriter(file).use { writer ->
                writer.append("ID,Title,Author,ISBN,Category,Stock,Available\n")
                books.forEach { book ->
                    writer.append("${book.id},\"${book.title}\",\"${book.author}\",\"${book.isbn}\",\"${book.category}\",${book.stock},${book.stock}\n")
                }
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createBooksJSON(books: List<Book>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            val gson = GsonBuilder().setPrettyPrinting().create()
            FileWriter(file).use { writer ->
                gson.toJson(books, writer)
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createBorrowingsCSV(borrowings: List<Borrowing>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            FileWriter(file).use { writer ->
                writer.append("ID,User Email,Book ID,Borrow Date,Due Date,Return Date,Status,Fine Amount\n")
                borrowings.forEach { borrowing ->
                    writer.append("${borrowing.id},${borrowing.userEmail},${borrowing.bookId},${borrowing.borrowDate},${borrowing.dueDate},${borrowing.returnDate ?: ""},${borrowing.status},${borrowing.fineAmount}\n")
                }
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createBorrowingsJSON(borrowings: List<Borrowing>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            val gson = GsonBuilder().setPrettyPrinting().create()
            FileWriter(file).use { writer ->
                gson.toJson(borrowings, writer)
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createUsersCSV(users: List<Map<String, Any>>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            FileWriter(file).use { writer ->
                writer.append("ID,Name,Email,Phone,Registration Date,Status\n")
                users.forEach { user ->
                    writer.append("${user["id"]},\"${user["name"]}\",\"${user["email"]}\",\"${user["phone"]}\",${user["registrationDate"]},${user["status"]}\n")
                }
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createUsersJSON(users: List<Map<String, Any>>, fileName: String): Boolean {
        return try {
            val file = createExportFile(fileName)
            val gson = GsonBuilder().setPrettyPrinting().create()
            FileWriter(file).use { writer ->
                gson.toJson(users, writer)
            }
            shareFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createExportFile(fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, "SIBUKA_$fileName")

        android.util.Log.d("ExportData", "File akan disimpan di: ${file.absolutePath}")

        return file
    }

    private fun shareFile(file: File) {
        try {
            android.util.Log.d("ExportData", "File berhasil dibuat: ${file.name}")
            android.util.Log.d("ExportData", "Ukuran file: ${file.length()} bytes")

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "SIBUKA Library Data Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share exported data"))
        } catch (e: Exception) {
            android.util.Log.e("ExportData", "Error sharing file: ${e.message}")
            Toast.makeText(context, "File tersimpan di Downloads: ${file.name}\nLokasi: ${file.parent}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showExportSuccess(dialogBinding: DialogExportDataBinding, dialog: AlertDialog) {
        dialogBinding.progressBar.visibility = View.GONE
        dialogBinding.btnExport.isEnabled = true
        Toast.makeText(context, "Data exported successfully to Downloads folder", Toast.LENGTH_LONG).show()
        dialog.dismiss()
    }

    private fun showExportError(dialogBinding: DialogExportDataBinding, dialog: AlertDialog) {
        dialogBinding.progressBar.visibility = View.GONE
        dialogBinding.btnExport.isEnabled = true
        Toast.makeText(context, "Error exporting data. Please try again.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
