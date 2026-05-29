package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Call Gemini 3.5 Flash to generate a conversational response.
     * Incorporates previous conversation history for actual context.
     */
    suspend fun chat(
        prompt: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to offline guide.")
            return@withContext getLocalFallbackResponse(prompt)
        }

        try {
            val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Map history to the required json structure
            history.forEach { message ->
                val role = if (message.sender == "user") "user" else "model"
                val textParts = JSONArray().put(JSONObject().put("text", message.text))
                contentsArray.put(
                    JSONObject()
                        .put("role", role)
                        .put("parts", textParts)
                )
            }

            // Append the latest user prompt
            val newParts = JSONArray().put(JSONObject().put("text", prompt))
            contentsArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", newParts)
            )

            // Setup system instruction
            val systemInstruction = JSONObject().put(
                "parts", JSONArray().put(
                    JSONObject().put(
                        "text",
                        "Du bist ein intelligenter und freundlicher KI-Assistent in einer Taschenrechner- und Arcade-Games-App. " +
                        "Der Benutzer spricht Deutsch. Antworte charmant, hilfsbereit und prägnant. Du kannst über Mathe, die Spiele (Snake, Tetris, Flappy Bird) und vieles mehr philosophieren!"
                    )
                )
            )

            val requestBodyJson = JSONObject()
                .put("contents", contentsArray)
                .put("systemInstruction", systemInstruction)

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed code ${response.code}: $errBody")
                    throw Exception("API Error Code: ${response.code}")
                }

                val bodyStr = response.body?.string() ?: throw Exception("Empty response body")
                Log.d(TAG, "Response size: ${bodyStr.length}")

                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Keine Antwort.")
                        }
                    }
                }
                return@withContext "Keine Textantwort von der KI erhalten."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini Client", e)
            return@withContext "Fehler beim Laden von Gemini: ${e.localizedMessage}. Du kannst mich immer noch normal benutzen oder Bilder erstellen!"
        }
    }

    /**
     * Generate an image query URL via Pollinations AI.
     * This takes the prompt, clean or enhances it, and returns the direct image endpoint.
     */
    fun getGeneratedImageUrl(prompt: String): String {
        return try {
            val encoded = URLEncoder.encode(prompt, "UTF-8")
            // Use pollinations.ai for beautiful, robust, rapid image generation.
            "https://image.pollinations.ai/p/$encoded?width=1024&height=1024&nologo=true&seed=${(0..100000).random()}"
        } catch (e: Exception) {
            "https://image.pollinations.ai/p/beautiful_landscape?width=1024&height=1024&nologo=true"
        }
    }

    private fun getLocalFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hallo") || lower.contains("hi") -> 
                "Hallo! Ich bin dein lokaler Arcade-Assistent 🕹️. (Du hast den Gemini API Key noch nicht konfiguriert). Ich kann dir trotzdem helfen! Frage mich nach Spiele-Tipps, Rechenrätseln oder erstelle direkt tolle Bilder mit dem 'Bild Generieren'-Schalter!"
            lower.contains("hilfe") || lower.contains("rechner") ->
                "Das ist eine Taschenrechner-App mit Geheimfunktionen! Tippe '0000' und dann '=' auf dem Taschenrechner, um zu Snake, Tetris, Flappy Bird und diesem Chatbot zu gelangen."
            lower.contains("snake") ->
                "Bei Snake steuerst du eine Schlange, die wächst, wenn sie Futter 🍎 isst. Berühre nicht den Rand oder deinen eigenen Körper! Nutze die Tasten unter dem Spielfeld."
            lower.contains("tetris") ->
                "In Tetris sortierst du fallende Spielsteine (Tetrominos), um vollständige horizontale Zeilen zu bilden und zu löschen. Sobald die Steine das obere Limit überschreiten, ist das Spiel vorbei!"
            lower.contains("flappy") || lower.contains("bird") ->
                "Flappy Bird ist ein Geschicklichkeitsspiel! Tippe auf den Bildschirm, um das Vögelchen nach oben flattern zu lassen und weiche den grünen Hindernis-Röhren aus!"
            lower.contains("witz") || lower.contains("lustig") ->
                "Warum tragen Mathematiker keine Uhren? Weil sie wissen, wie spät es ist, wenn sie die Zeit integrieren! 😄 Wie gefällt dir das? Du kannst auch Bilder generieren lassen!"
            else ->
                "Interessante Frage! Da kein Gemini API Key konfiguriert ist, antworte ich dir offline. Aktiviere den 'Bild Generieren'-Schalter, um deine kreativen Ideen als Kunstwerke in die Galerie zu zaubern ✨!"
        }
    }
}
