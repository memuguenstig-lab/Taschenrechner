package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val versionName: String, val versionCode: Int, val apkUrl: String, val changelog: String) : UpdateState
    object UpToDate : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    // URL to your version.json in Supabase (dynamic or set in config)
    private var supabaseUrl: String = ""
    private var supabaseKey: String = ""

    fun initialize(supabaseProjectRef: String, bucketName: String, key: String = "") {
        supabaseKey = key
        if (supabaseProjectRef.isNotEmpty() && bucketName.isNotEmpty()) {
            supabaseUrl = "https://$supabaseProjectRef.supabase.co/storage/v1/object/public/$bucketName"
        }
    }

    suspend fun checkForUpdates() {
        if (supabaseUrl.isEmpty()) {
            _updateState.value = UpdateState.Error("Supabase configuration is empty. Set credentials in Settings!")
            return
        }

        _updateState.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$supabaseUrl/version.json")
                
                if (supabaseKey.isNotEmpty()) {
                    requestBuilder.header("apikey", supabaseKey)
                    requestBuilder.header("Authorization", "Bearer $supabaseKey")
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error("Fehler beim Abrufen der Web-Version: ${response.code}")
                        return@withContext
                    }

                    val responseBody = response.body?.string() ?: ""
                    val json = JSONObject(responseBody)
                    val webVersionCode = json.optInt("versionCode", 1)
                    val webVersionName = json.optString("versionName", "1.0")
                    val apkUrl = json.optString("apkUrl", "")
                    val changelog = json.optString("changelog", "Keine Beschreibung.")

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    Log.d("UpdateManager", "Web: v$webVersionCode, Current: v$currentVersionCode")

                    if (webVersionCode > currentVersionCode) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            versionName = webVersionName,
                            versionCode = webVersionCode,
                            apkUrl = apkUrl,
                            changelog = changelog
                        )
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Check failed", e)
                _updateState.value = UpdateState.Error("Fehler beim Update-Check: ${e.localizedMessage}")
            }
        }
    }

    suspend fun downloadAndInstallApk(apkUrl: String) {
        if (apkUrl.isEmpty()) {
            _updateState.value = UpdateState.Error("Ungültige APK-URL")
            return
        }

        _updateState.value = UpdateState.Downloading(0f)
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(apkUrl)
                if (supabaseKey.isNotEmpty()) {
                    requestBuilder.header("apikey", supabaseKey)
                    requestBuilder.header("Authorization", "Bearer $supabaseKey")
                }
                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error("Download fehlgeschlagen: HTTP ${response.code}")
                        return@withContext
                    }

                    val body = response.body ?: throw Exception("Leerer Response Body")
                    val file = File(context.cacheDir, "app-update.apk")
                    if (file.exists()) file.delete()

                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                if (totalBytes > 0) {
                                    val progress = bytesDownloaded.toFloat() / totalBytes
                                    _updateState.value = UpdateState.Downloading(progress)
                                }
                            }
                        }
                    }

                    _updateState.value = UpdateState.ReadyToInstall(file)
                    installApk(file)
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Download failed", e)
                _updateState.value = UpdateState.Error("Zertifikatsfehler oder Download abgebrochen: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Installation failed", e)
            _updateState.value = UpdateState.Error("Installation konnte nicht gestartet werden: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
