# Tambahkan ke proguard-rules.pro untuk mengurangi log noise di production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep your application classes
-keep class com.example.sibuka.** { *; }
