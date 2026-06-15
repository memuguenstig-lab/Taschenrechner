package com.example.ui.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalWifiFileServer(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isServerRunning by remember { mutableStateOf(false) }
    var serverHost by remember { mutableStateOf("") }
    val serverPort = 8080
    
    // Directory where shared files are stored
    val sharedDir = remember { File(context.filesDir, "wifi_shared_files").apply { mkdirs() } }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var serverLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var sharedNotes by remember { mutableStateOf<List<String>>(emptyList()) }

    fun updateFileList() {
        fileList = sharedDir.listFiles()?.toList() ?: emptyList()
    }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        serverLogs = (listOf("[$time] $msg") + serverLogs).take(50)
    }

    // Static Server holder
    val serverHolder = remember { mutableStateOf<HttpServer?>(null) }

    fun stopServer() {
        serverHolder.value?.let {
            it.stop(0)
            serverHolder.value = null
            isServerRunning = false
            addLog("Server gestoppt.")
        }
    }

    fun startServer() {
        if (isServerRunning) return
        val currentIp = getWiFiIPAddress()
        if (currentIp == null) {
            Toast.makeText(context, "Kein Wi-Fi Netzwerk erkannt!", Toast.LENGTH_LONG).show()
            addLog("Fehler: Kein Wi-Fi Netzwerk erkannt.")
            return
        }
        serverHost = currentIp
        
        try {
            val server = HttpServer.create(InetSocketAddress(serverPort), 0)
            server.createContext("/", HttpHandler { exchange ->
                val requestMethod = exchange.requestMethod
                val path = exchange.requestURI.path
                addLog("${requestMethod} ${path} von ${exchange.remoteAddress.hostName}")
                
                try {
                    when {
                        path == "/" && requestMethod == "GET" -> {
                            handleGetIndex(exchange, sharedDir, sharedNotes)
                        }
                        path == "/download" && requestMethod == "GET" -> {
                            handleFileDownload(exchange, sharedDir, ::addLog)
                        }
                        path == "/upload" && requestMethod == "POST" -> {
                            handleFileUpload(exchange, sharedDir, ::updateFileList, ::addLog)
                        }
                        path == "/addnote" && requestMethod == "POST" -> {
                            handleNoteUpload(exchange) { newNote ->
                                sharedNotes = sharedNotes + newNote
                                addLog("Neue Notiz empfangen.")
                            }
                        }
                        path == "/delete" && requestMethod == "GET" -> {
                            handleFileDelete(exchange, sharedDir, ::updateFileList, ::addLog)
                        }
                        else -> {
                            sendResponse(exchange, "Mit Spy-Server verbunden.", 200, "text/plain")
                        }
                    }
                } catch (e: Exception) {
                    addLog("Fehler bei Request: ${e.localizedMessage}")
                    sendResponse(exchange, "Interner Fehler: ${e.localizedMessage}", 500, "text/plain")
                }
            })
            
            server.executor = java.util.concurrent.Executors.newCachedThreadPool()
            server.start()
            serverHolder.value = server
            isServerRunning = true
            addLog("Server gestartet auf http://$serverHost:$serverPort")
        } catch (e: Exception) {
            addLog("Fehler beim Starten des Servers: ${e.localizedMessage}")
        }
    }

    LaunchedEffect(Unit) {
        updateFileList()
        // Auto-start server if wifi is good
        getWiFiIPAddress()?.let {
            serverHost = it
        }
    }

    // Stop server on screen dispose
    DisposableEffect(Unit) {
        onDispose {
            stopServer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Night blue / slate dark
    ) {
        TopAppBar(
            title = {
                Text(
                    "🛰️ SPY WI-FI HUB",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF020617))
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isServerRunning) Color(0xFF00FFCC) else Color(0xFF475569), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    "Server-Verbindung",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (isServerRunning) Color(0xFF00FFCC) else Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isServerRunning) "AKTIV (Online)" else "AUS (Offline)",
                                        color = if (isServerRunning) Color(0xFF00FFCC) else Color(0xFFEF4444),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Switch(
                                checked = isServerRunning,
                                onCheckedChange = { active ->
                                    if (active) startServer() else stopServer()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0F172A),
                                    checkedTrackColor = Color(0xFF00FFCC),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF334155)
                                )
                            )
                        }

                        if (isServerRunning) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "PC BROWSER ADRESSE:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                val urlStr = "http://$serverHost:$serverPort"
                                Text(
                                    urlStr,
                                    color = Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Spy FTP Server", urlStr))
                                        Toast.makeText(context, "Link kopiert!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren", tint = Color(0xFF00FFCC), modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Aktiviere den Schalter, um Dateien drahtlos über dein lokales WLAN mit deinem PC/Tablet auszutauschen.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Shared Folder Header / Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📁 GETEILTE DATEIEN (${fileList.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = { updateFileList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Dateien aktualisieren", tint = Color(0xFF00FFCC))
                    }
                }
            }

            if (fileList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Ordner leer",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Noch keine Dateien geteilt", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            Text("Dateien am PC hochladen, um sie hier zu sehen.", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(fileList) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = when (file.extension.lowercase()) {
                                    "txt", "pdf", "docx" -> Icons.Default.Description
                                    "png", "jpg", "jpeg", "webp" -> Icons.Default.Image
                                    "mp3", "wav" -> Icons.Default.Audiotrack
                                    "mp4", "mkv" -> Icons.Default.Movie
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = "Datei",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    "${formatFileSize(file.length())}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = {
                                file.delete()
                                updateFileList()
                                addLog("Datei gelöscht: ${file.name}")
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Aus Server löschen", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // Sync Notes Corner
            if (sharedNotes.isNotEmpty()) {
                item {
                    Text(
                        "📝 EMPFANGENE TEXTNOTIZEN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                items(sharedNotes) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note, color = Color.White, fontSize = 13.sp)
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Spy Note", note))
                                Toast.makeText(context, "In Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.CopyAll, contentDescription = "Kopieren", tint = Color(0xFF00FFCC))
                            }
                        }
                    }
                }
            }

            // Console Access Logs
            item {
                Text(
                    "⚡ LIVE PROTOKOLL (CLIENT ACTIONS)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    if (serverLogs.isEmpty()) {
                        Text(
                            "Warte auf Client-Aktivitäten...",
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(serverLogs) { log ->
                                Text(
                                    log,
                                    color = if (log.contains("Fehler")) Color(0xFFEF4444) else Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// WI-FI IP Helper
private fun getWiFiIPAddress(): String? {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (iface in interfaces) {
            val addrs = Collections.list(iface.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: ""
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (isIPv4) {
                        return sAddr
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 Bytes"
    val units = arrayOf("Bytes", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Simple response sender helper
private fun sendResponse(exchange: HttpExchange, htmlContent: String, statusCode: Int, contentType: String) {
    val bytes = htmlContent.toByteArray(Charsets.UTF_8)
    exchange.responseHeaders.set("Content-Type", "$contentType; charset=UTF-8")
    exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
    val os = exchange.responseBody
    os.write(bytes)
    os.close()
}

// Render index page for web browser clients
private fun handleGetIndex(exchange: HttpExchange, sharedDir: File, notes: List<String>) {
    val files = sharedDir.listFiles() ?: emptyArray()
    
    val filesTableRows = if (files.isEmpty()) {
        "<tr><td colspan='3' style='text-align:center; padding:20px; color:#64748B;'>Noch keine Spionageberichte hochgeladen.</td></tr>"
    } else {
        files.joinToString("") { file ->
            """
            <tr>
                <td><strong>${file.name}</strong></td>
                <td>${formatFileSize(file.length())}</td>
                <td style="text-align: right;">
                    <a href="/download?name=${java.net.URLEncoder.encode(file.name, "UTF-8")}" class="btn dl-btn">⬇ Download</a>
                    <a href="/delete?name=${java.net.URLEncoder.encode(file.name, "UTF-8")}" class="btn del-btn" style="background:#EF4444; margin-left:8px;">✖ Löschen</a>
                </td>
            </tr>
            """
        }
    }

    val noteItems = if (notes.isEmpty()) {
        "<p style='color:#64748B; font-style:italic;'>Keine Textnotizen vorhanden.</p>"
    } else {
        notes.joinToString("") { note ->
            "<div class='note-item'>$note</div>"
        }
    }

    val html = """
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>🛰️ Secret Agency WI-FI Share</title>
        <style>
            body {
                background: #0D131E;
                color: #E2E8F0;
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                padding: 40px 20px;
            }
            .container {
                max-width: 900px;
                margin: 0 auto;
            }
            header {
                display: flex;
                align-items: center;
                gap: 15px;
                border-bottom: 2px solid #00FFCC;
                padding-bottom: 20px;
                margin-bottom: 30px;
            }
            h1 {
                margin: 0;
                color: #00FFCC;
                font-family: Courier, monospace;
                letter-spacing: -1px;
            }
            .grid {
                display: grid;
                grid-template-columns: 2fr 1fr;
                gap: 24px;
            }
            @media (max-width: 768px) {
                .grid { grid-template-columns: 1fr; }
            }
            .card {
                background: #192231;
                border: 1px solid #2A3649;
                border-radius: 12px;
                padding: 24px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            }
            .card-title {
                color: #00FFCC;
                margin-top: 0;
                margin-bottom: 20px;
                font-size: 18px;
                font-family: Courier, monospace;
                text-transform: uppercase;
                border-bottom: 1px solid #2A3649;
                padding-bottom: 10px;
            }
            table {
                width: 100%;
                border-collapse: collapse;
            }
            th, td {
                padding: 12px;
                text-align: left;
                border-bottom: 1px solid #2A3649;
            }
            th { color: #8F9CAE; font-size: 13px; text-transform: uppercase; }
            .btn {
                display: inline-block;
                background: #00FFCC;
                color: #0D131E;
                padding: 8px 16px;
                border: none;
                border-radius: 6px;
                text-decoration: none;
                font-weight: bold;
                font-size: 13px;
                cursor: pointer;
                transition: transform 0.1s;
            }
            .btn:active { transform: scale(0.96); }
            .dl-btn { background: #00FFCC; color: #0D131E; }
            .del-btn { background: #EF4444 !important; color: white !important; }
            input[type="file"] {
                display: none;
            }
            .upload-label {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                border: 2px dashed #00FFCC;
                border-radius: 8px;
                padding: 30px;
                cursor: pointer;
                transition: background 0.2s;
            }
            .upload-label:hover { background: rgba(0,255,204,0.05); }
            .note-item {
                background: #0D131E;
                border-left: 3px solid #00FFCC;
                padding: 12.dp;
                margin-bottom: 10px;
                padding: 12px;
                border-radius: 0 8px 8px 0;
                font-size: 13px;
                word-break: break-all;
            }
            textarea {
                width: 100%;
                box-sizing: border-box;
                background: #0D131E;
                color: white;
                border: 1px solid #2A3649;
                border-radius: 6px;
                padding: 10px;
                resize: none;
                margin-bottom: 12px;
            }
            textarea:focus { border-color: #00FFCC; outline: none; }
        </style>
    </head>
    <body>
        <div class="container">
            <header>
                <span style="font-size: 40px;">🛰️</span>
                <div>
                    <h1>HERMES SPY WI-FI STATION</h1>
                    <p style="color: #64748B; margin: 5px 0 0 0;">Dateien und Notizen blitzschnell im lokalen Netzwerk transferieren.</p>
                </div>
            </header>
            
            <div class="grid">
                <div class="card">
                    <h2 class="card-title">🗄️ Geteilte Dokumente</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Größe</th>
                                <th style="text-align: right;">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            $filesTableRows
                        </tbody>
                    </table>
                </div>

                <div class="card" style="display: flex; flex-direction: column; gap: 24px;">
                    <div>
                        <h2 class="card-title">📤 Datei hochladen</h2>
                        <form id="uploadForm" action="/upload" method="post" enctype="multipart/form-data">
                            <label class="upload-label">
                                <span style="font-size: 32px; margin-bottom: 10px;">📂</span>
                                <strong style="color: #00FFCC;">Datei auswählen</strong>
                                <span style="color:#64748B; font-size:12px; margin-top:5px;">Oder hierher ziehen</span>
                                <input type="file" name="file" id="fileInput" onchange="submitForm()">
                            </label>
                        </form>
                    </div>

                    <div>
                        <h2 class="card-title">📝 Notiz zum Handy senden</h2>
                        <form action="/addnote" method="post">
                            <textarea name="note" rows="3" placeholder="Geben Sie Informationen ein, die Sie auf das Handy-Clipboard übertragen wollen..."></textarea>
                            <button type="submit" class="btn" style="width: 100%;">Absenden</button>
                        </form>
                        <hr style="border:0; border-top: 1px solid #2A3649; margin: 15px 0;">
                        <span style="color: #8F9CAE; font-size:12px; font-weight:bold; text-transform:uppercase;">Empfangene Berichte:</span>
                        <div style="margin-top: 10px;">
                            $noteItems
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <script>
            function submitForm() {
                var form = document.getElementById('uploadForm');
                var formData = new FormData(form);
                var xhr = new MyRequest();
                
                // Show uploading UI if desired, here we just submit directly:
                form.submit();
            }
        </script>
    </body>
    </html>
    """.trimIndent()
    sendResponse(exchange, html, 200, "text/html")
}

// Download action handler
private fun handleFileDownload(exchange: HttpExchange, sharedDir: File, logger: (String) -> Unit) {
    val query = exchange.requestURI.query ?: ""
    val filename = query.substringAfter("name=").let { java.net.URLDecoder.decode(it, "UTF-8") }
    val file = File(sharedDir, filename)
    
    if (file.exists() && file.parentFile == sharedDir) {
        val headers = exchange.responseHeaders
        headers.set("Content-Disposition", "attachment; filename=\"${file.name}\"")
        headers.set("Content-Type", "application/octet-stream")
        exchange.sendResponseHeaders(200, file.length())
        
        val fis = FileInputStream(file)
        val os = exchange.responseBody
        val buffer = ByteArray(4096)
        var count: Int
        while (fis.read(buffer).also { count = it } != -1) {
            os.write(buffer, 0, count)
        }
        fis.close()
        os.close()
        logger("Datei heruntergeladen: ${file.name}")
    } else {
        sendResponse(exchange, "Datei nicht gefunden.", 404, "text/plain")
    }
}

// Simple delete action
private fun handleFileDelete(exchange: HttpExchange, sharedDir: File, update: () -> Unit, logger: (String) -> Unit) {
    val query = exchange.requestURI.query ?: ""
    val filename = query.substringAfter("name=").let { java.net.URLDecoder.decode(it, "UTF-8") }
    val file = File(sharedDir, filename)
    
    if (file.exists() && file.parentFile == sharedDir) {
        file.delete()
        update()
        logger("Datei über Web UI gelöscht: $filename")
        // Redirect back home
        exchange.responseHeaders.set("Location", "/")
        exchange.sendResponseHeaders(303, -1)
    } else {
        sendResponse(exchange, "Konnte Datei nicht löschen.", 400, "text/plain")
    }
}

// Receive new Note
private fun handleNoteUpload(exchange: HttpExchange, addNote: (String) -> Unit) {
    val body = readBodyString(exchange.requestBody)
    val noteValue = body.substringAfter("note=").let { java.net.URLDecoder.decode(it, "UTF-8") }
    if (noteValue.isNotEmpty()) {
        addNote(noteValue)
    }
    // Redirect back home
    exchange.responseHeaders.set("Location", "/")
    exchange.sendResponseHeaders(303, -1)
}

// Simplified RFC-compliant multi-part body parser for binary files
private fun handleFileUpload(
    exchange: HttpExchange,
    sharedDir: File,
    onSuccess: () -> Unit,
    logger: (String) -> Unit
) {
    val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
    val boundary = contentType.substringAfter("boundary=").trim()
    val rawBytes = readBodyBytes(exchange.requestBody)
    
    if (boundary.isNotEmpty() && rawBytes.isNotEmpty()) {
        try {
            val boundaryBytes = "--$boundary".toByteArray(Charsets.UTF_8)
            val headerIndex = KmpSearch(rawBytes, boundaryBytes)
            if (headerIndex != -1) {
                // Find headers of part
                val subBytes = rawBytes.copyOfRange(headerIndex, rawBytes.size)
                val lineBreak = "\r\n\r\n".toByteArray(Charsets.UTF_8)
                val bodyStartIndex = KmpSearch(subBytes, lineBreak)
                
                if (bodyStartIndex != -1) {
                    val headerStr = String(subBytes.copyOfRange(0, bodyStartIndex), Charsets.UTF_8)
                    var fileName = "bericht_${System.currentTimeMillis()}.bin"
                    
                    if (headerStr.contains("filename=")) {
                        val fn = headerStr.substringAfter("filename=\"").substringBefore("\"")
                        if (fn.isNotEmpty()) {
                            fileName = fn
                        }
                    }
                    
                    // Body starts search
                    val fileDataStart = headerIndex + bodyStartIndex + 4
                    // End boundary
                    val nextBoundaryIndex = KmpSearchFrom(rawBytes, boundaryBytes, fileDataStart)
                    val fileDataEnd = if (nextBoundaryIndex != -1) nextBoundaryIndex - 2 else rawBytes.size - 2
                    
                    if (fileDataEnd > fileDataStart) {
                        val fileBytes = rawBytes.copyOfRange(fileDataStart, fileDataEnd)
                        val outFile = File(sharedDir, fileName)
                        FileOutputStream(outFile).use { fos ->
                            fos.write(fileBytes)
                        }
                        logger("Datei empfangen: $fileName (${formatFileSize(outFile.length())})")
                        onSuccess()
                    }
                }
            }
        } catch (e: Exception) {
            logger("Fehler beim Verarbeiten des Datei-Uploads: ${e.localizedMessage}")
        }
    }
    
    // Redirect back to home
    exchange.responseHeaders.set("Location", "/")
    exchange.sendResponseHeaders(303, -1)
}

// Simple body readers helpers
private fun readBodyString(stream: InputStream): String {
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private fun readBodyBytes(stream: InputStream): ByteArray {
    val streamOut = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var count: Int
    while (stream.read(buffer).also { count = it } != -1) {
        streamOut.write(buffer, 0, count)
    }
    stream.close()
    return streamOut.toByteArray()
}

// Simple Knuth-Morris-Pratt substring locator
private fun KmpSearch(bytes: ByteArray, pattern: ByteArray): Int {
    return KmpSearchFrom(bytes, pattern, 0)
}

private fun KmpSearchFrom(bytes: ByteArray, pattern: ByteArray, fromIndex: Int): Int {
    if (pattern.isEmpty() || bytes.size < pattern.size) return -1
    for (i in fromIndex..bytes.size - pattern.size) {
        var found = true
        for (j in pattern.indices) {
            if (bytes[i + j] != pattern[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}
