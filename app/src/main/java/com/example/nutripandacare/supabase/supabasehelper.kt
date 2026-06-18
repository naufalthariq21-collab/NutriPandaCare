package com.example.nutripandacare.supabase

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseHelper {

    // ════════════════════════════════════════════════════════════
    // [FIX] SUPABASE_URL harus base URL project saja (tanpa /rest/v1/)
    // Sebelumnya: "https://xxx.supabase.co/rest/v1/" → endpoint storage
    // jadi "https://xxx.supabase.co/rest/v1//storage/v1/object/..." (SALAH)
    // Sekarang: "https://xxx.supabase.co" → endpoint storage jadi
    // "https://xxx.supabase.co/storage/v1/object/..." (BENAR)
    // ════════════════════════════════════════════════════════════
    private const val SUPABASE_URL      = "https://tguhtbsbzhgzznwkuzrt.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRndWh0YnNiemhnenpud2t1enJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE3MjIzOTcsImV4cCI6MjA5NzI5ODM5N30.hk3mG0KSTd1dGEUF9-QmArXkbcwOu7j47nZzJ4wvAc0"
    private const val BUCKET_NAME       = "foto-nutripanda"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun uploadImage(
        uri: Uri,
        context: Context,
        folder: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    postError(onError, "Tidak bisa membaca file foto")
                    return@launch
                }
                val bytes = inputStream.readBytes()
                inputStream.close()

                val url = uploadBytes(bytes, folder)
                if (url != null) {
                    postSuccess(onSuccess, url)
                } else {
                    postError(onError, "Upload gagal, coba lagi")
                }
            } catch (e: Exception) {
                postError(onError, e.message ?: "Error tidak terduga saat upload")
            }
        }
    }

    suspend fun uploadImageSuspend(bytes: ByteArray, folder: String = ""): String? {
        return withContext(Dispatchers.IO) { uploadBytes(bytes, folder) }
    }

    private fun uploadBytes(bytes: ByteArray, folder: String): String? {
        return try {
            val fileName   = "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val objectPath = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

            // [FIX] URL sekarang benar: {base}/storage/v1/object/{bucket}/{path}
            val uploadUrl = "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$objectPath"

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "image/jpeg")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                // [FIX] Public URL juga benar sekarang
                "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/$objectPath"
            } else {
                val errorBody = response.body?.string() ?: "no body"
                android.util.Log.e("SupabaseHelper", "Upload failed ${response.code}: $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseHelper", "Upload exception: ${e.message}", e)
            null
        }
    }

    private fun postSuccess(onSuccess: (String) -> Unit, url: String) {
        mainHandler.post { onSuccess(url) }
    }

    private fun postError(onError: (String) -> Unit, message: String) {
        mainHandler.post { onError(message) }
    }
}