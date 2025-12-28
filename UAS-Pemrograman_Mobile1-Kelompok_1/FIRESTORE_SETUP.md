# Firebase Firestore Rules untuk SIBUKA

## Setup Firestore Database Rules

Untuk menggunakan fitur tambah buku dengan Firestore, Anda perlu mengatur rules yang tepat.

### 1. Buka Firebase Console
- Kunjungi https://console.firebase.google.com
- Pilih project SIBUKA Anda

### 2. Setup Firestore Database
- Klik "Firestore Database" di menu kiri
- Jika belum ada, klik "Create database"
- Pilih "Start in test mode" untuk development

### 3. Update Firestore Rules
Klik tab "Rules" dan ganti dengan:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read and write access to books collection
    match /books/{bookId} {
      allow read, write: if true;
    }
    
    // Allow read and write access to borrowings collection
    match /borrowings/{borrowingId} {
      allow read, write: if true;
    }
    
    // For testing - delete this rule in production
    match /test/{testId} {
      allow read, write: if true;
    }
  }
}
```

### 4. Untuk Production (Lebih Aman)
Setelah testing berhasil, ganti dengan rules yang lebih aman:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /books/{bookId} {
      allow read: if true;  // Semua orang bisa baca
      allow write: if request.auth != null;  // Hanya user login yang bisa tulis
    }
    
    match /borrowings/{borrowingId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 5. Klik "Publish" untuk menerapkan rules

## Struktur Data Firestore

Data buku akan disimpan di collection "books" dengan struktur:
```
books/
  {bookId}/
    - id: string
    - title: string
    - author: string
    - category: string
    - publisher: string
    - publicationYear: number
    - stock: number
    - location: string
    - description: string
    - imageUrl: string
    - createdAt: timestamp
    - updatedAt: timestamp
```
