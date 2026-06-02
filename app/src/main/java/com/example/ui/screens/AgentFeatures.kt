package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FakeNote
import com.example.data.IntruderPhoto
import com.example.ui.AppTheme
import com.example.ui.AppViewModel
import com.example.ui.DisguiseMode
import com.example.ui.GameType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- THEME PALETTE ENGINE ---
data class ThemePalette(
    val background: Color,
    val displayBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val keysBg: Color,
    val numKeysBg: Color,
    val opKeysBg: Color,
    val clearKeysBg: Color,
    val equalsKeyBg: Color,
    val equalsKeyText: Color,
    val accentColor: Color,
    val borderStrokeColor: Color,
    val isRetroMono: Boolean = false,
    val title: String
)

@Composable
fun getThemePalette(theme: AppTheme): ThemePalette {
    return when (theme) {
        AppTheme.CLASSIC_DARK -> ThemePalette(
            background = Color(0xFF0F0F12),
            displayBg = Color.Black,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.6f),
            keysBg = Color.White.copy(alpha = 0.05f),
            numKeysBg = Color.White.copy(alpha = 0.04f),
            opKeysBg = Color.White.copy(alpha = 0.12f),
            clearKeysBg = Color.White.copy(alpha = 0.08f),
            equalsKeyBg = Color(0xFF0A84FF),
            equalsKeyText = Color.White,
            accentColor = Color(0xFF0A84FF),
            borderStrokeColor = Color.White.copy(alpha = 0.15f),
            title = "Midnight Blue"
        )
        AppTheme.OLED_BLACK -> ThemePalette(
            background = Color.Black,
            displayBg = Color.Black,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.4f),
            keysBg = Color.Black,
            numKeysBg = Color(0xFF151515),
            opKeysBg = Color(0xFF252528),
            clearKeysBg = Color(0xFF401515),
            equalsKeyBg = Color.White,
            equalsKeyText = Color.Black,
            accentColor = Color.White,
            borderStrokeColor = Color(0xFF333333),
            title = "OLED Deep Black"
        )
        AppTheme.RETRO_TERMINAL -> ThemePalette(
            background = Color(0xFF040C04),
            displayBg = Color(0xFF020702),
            textPrimary = Color(0xFF33FF33),
            textSecondary = Color(0xFF00AA00),
            keysBg = Color(0xFF040D04),
            numKeysBg = Color(0xFF030A03),
            opKeysBg = Color(0xFF051805),
            clearKeysBg = Color(0xFF280808),
            equalsKeyBg = Color(0xFF33FF33),
            equalsKeyText = Color.Black,
            accentColor = Color(0xFF33FF33),
            borderStrokeColor = Color(0xFF33FF33).copy(alpha = 0.3f),
            isRetroMono = true,
            title = "Hacker Green"
        )
        AppTheme.CYBERPUNK -> ThemePalette(
            background = Color(0xFF120824),
            displayBg = Color(0xFF07020E),
            textPrimary = Color(0xFF00FFCC), // Volt Green/Cyan
            textSecondary = Color(0xFFFF007F), // Neon Pink
            keysBg = Color(0xFF1E0A3C),
            numKeysBg = Color(0xFF1E0A3C),
            opKeysBg = Color(0xFF360066),
            clearKeysBg = Color(0xFF4C0033),
            equalsKeyBg = Color(0xFFFF007F),
            equalsKeyText = Color.White,
            accentColor = Color(0xFFFF007F),
            borderStrokeColor = Color(0xFFFF007F).copy(alpha = 0.4f),
            title = "Cyberneon"
        )
        AppTheme.AMBER_GOLD -> ThemePalette(
            background = Color(0xFF15110B),
            displayBg = Color(0xFF0B0805),
            textPrimary = Color(0xFFFF9F1C), // Gold
            textSecondary = Color(0xFFFF9F1C).copy(alpha = 0.6f),
            keysBg = Color(0xFF1F180F),
            numKeysBg = Color(0xFF1F180F),
            opKeysBg = Color(0xFF332515),
            clearKeysBg = Color(0xFF4D1C1C),
            equalsKeyBg = Color(0xFFFF9F1C),
            equalsKeyText = Color.Black,
            accentColor = Color(0xFFFF9F1C),
            borderStrokeColor = Color(0xFFFF9F1C).copy(alpha = 0.3f),
            title = "Amber Gold"
        )
    }
}

// --- SILENT CAMERA & LOCK DETECTOR COMPONENT ---
@Composable
fun SilentCameraScanner(viewModel: AppViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        mutableStateOf(perm == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    
    val reqPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    
    // Automatically attempt trigger photo on creation of dashboard!
    LaunchedEffect(viewModel.isSecretPhotoEnabled) {
        if (!hasCameraPermission) {
            reqPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        fun saveMockEntry() {
            val formats = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY)
            val ipMock = "192.168." + (10..254).random() + "." + (10..254).random()
            val deviceMock = if (Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator")) {
                "Android SDK Emulator (Virtual Target)"
            } else {
                Build.MANUFACTURER + " " + Build.MODEL
            }
            val path = "MOCK_TELEMETRY|${formats.format(Date())}|$ipMock|$deviceMock"
            viewModel.captureIntruderPhoto(isMocked = true, base64OrPath = path)
        }

        fun takeScreenshot() {
            // Mock screenshot logic (cannot actually capture screen without system permissions)
            val formats = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY)
            val path = "MOCK_SCREENSHOT|${formats.format(Date())}"
            viewModel.captureIntruderPhoto(isMocked = true, base64OrPath = path)
        }

        fun takePhoto(isFront: Boolean) {
            if (!hasCameraPermission) {
                saveMockEntry()
                return
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    val cameraSelector = if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture
                    )
                    
                    val file = File(context.cacheDir, "spy_${if (isFront) "front" else "back"}_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                viewModel.captureIntruderPhoto(isMocked = false, base64OrPath = file.absolutePath)
                                cameraProvider.unbindAll()
                            }
                            override fun onError(exception: ImageCaptureException) {
                                saveMockEntry()
                                cameraProvider.unbindAll()
                            }
                        }
                    )
                } catch (e: Exception) {
                    saveMockEntry()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Initial snapshot
        delay(600)
        takePhoto(isFront = true)
        
        if (viewModel.isSecretPhotoEnabled) {
            while(true) {
                delay(60000) // wait 1 minute
                takePhoto(isFront = true)
                delay(2000) // slight delay before switching camera
                takePhoto(isFront = false)
                delay(2000)
                takeScreenshot()
            }
        }
    }
}

// --- SETTINGS DIALOG ---
@Composable
fun ThemeSettingsDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTriggerUpdate: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101014),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App-Optionen", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // --- CATEGORY 1: DESIGN & LOOK ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DESIGN & DARSTELLUNG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A84FF),
                        letterSpacing = 1.2.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                
                Text(
                    "Wähle ein Design für den Rechner aus:",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                
                AppTheme.values().forEach { theme ->
                    val isSelected = viewModel.appTheme == theme
                    val palette = getThemePalette(theme)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .border(1.dp, if (isSelected) palette.accentColor else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateTheme(theme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(palette.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                if (theme == AppTheme.RETRO_TERMINAL) "Monospace Monochrom" else "Stilvoll & Modern",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.background))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.accentColor))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.equalsKeyBg))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // --- CATEGORY 2: SECURITY & DISGUISE ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SICHERHEIT & TARNUNG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500),
                        letterSpacing = 1.2.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                
                Text(
                    "Tarnungs-Modus (Disguise):",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                
                DisguiseMode.values().forEach { mode ->
                    val isSelected = viewModel.disguiseMode == mode
                    val (label, desc, icon) = when (mode) {
                        DisguiseMode.NONE -> Triple("Normaler Rechner", "Klassisches Design ohne Tarnung", Icons.Default.Calculate)
                        DisguiseMode.CONVERTER -> Triple("Einheiten-Umrechner", "Zahlenumrechner-Fassade", Icons.Default.SwapHoriz)
                        DisguiseMode.NOTEPAD -> Triple("Notizen (Notepad)", "Einfaches Notizen-Schreibprogramm", Icons.Default.Notes)
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .border(1.dp, if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateDisguiseMode(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(desc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                if (onTriggerUpdate != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // --- CATEGORY 3: SYSTEM & UPDATES ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SYSTEM & STAND",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759),
                            letterSpacing = 1.2.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    
                    Text(
                        "Prüfe und lade neue Versionen:",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onTriggerUpdate.invoke()
                            }
                            .padding(12.dp)
                            .testTag("settings_software_update_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "System-Update",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Software-Update",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Nach Updates suchen & neue Features laden",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Öffnen",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Geheim-Tipp: Du kannst jederzeit den Passcode '0000' oder '1111' eingeben, um den Tresor freizugeben!",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- FAKE NOTEPAD FAÇADE ---
@Composable
fun NotepadScreen(viewModel: AppViewModel) {
    val notes by viewModel.fakeNotes.collectAsState()
    var searchWord by remember { mutableStateOf("") }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    var isWritingNote by remember { mutableStateOf(false) }
    
    // Unlock if typed secret "0000" inside the search box!
    LaunchedEffect(searchWord) {
        if (searchWord.trim() == "0000") {
            searchWord = ""
            viewModel.isSecretUnlocked = true
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Einfache Notizen", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = { viewModel.isSecretUnlocked = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Tresor", tint = Color.White.copy(alpha = 0.15f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E24))
            )
        },
        floatingActionButton = {
            if (!isWritingNote) {
                FloatingActionButton(
                    onClick = { isWritingNote = true },
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Neue Notiz")
                }
            }
        },
        containerColor = Color(0xFF121214)
    ) { p ->
        Box(modifier = Modifier.fillMaxSize().padding(p)) {
            if (isWritingNote) {
                // Writer dialog overlay
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C22)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("Notiz erstellen", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextField(
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            placeholder = { Text("Titel", color = Color.White.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextField(
                            value = noteContentInput,
                            onValueChange = { noteContentInput = it },
                            placeholder = { Text("Schreibe etwas...", color = Color.White.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                isWritingNote = false
                                noteTitleInput = ""
                                noteContentInput = ""
                            }) {
                                Text("Abbrechen", color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (noteTitleInput.trim().isNotEmpty() || noteContentInput.trim().isNotEmpty()) {
                                        viewModel.addFakeNote(
                                            title = noteTitleInput.ifEmpty { "Unbenannt" },
                                            content = noteContentInput
                                        )
                                    }
                                    isWritingNote = false
                                    noteTitleInput = ""
                                    noteContentInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Text("Speichern", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    TextField(
                        value = searchWord,
                        onValueChange = { searchWord = it },
                        placeholder = { Text("Notizen durchsuchen...", color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (notes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Keine Notizen gefunden", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(notes) { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(note.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                            IconButton(
                                                onClick = { viewModel.deleteFakeNote(note.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(note.content, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val form = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                                        Text(form.format(Date(note.timestamp)), fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FAKE CONVERTER FAÇADE ---
@Composable
fun ConverterScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var inputVal by remember { mutableStateOf("") }
    var outputVal by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LENGTH") } // LENGTH, TEMP, WEIGHT
    
    fun performConvert() {
        val num = inputVal.toDoubleOrNull() ?: 0.0
        if (inputVal == "0000") {
            viewModel.isSecretUnlocked = true
            inputVal = ""
            return
        }
        
        outputVal = when (selectedType) {
            "LENGTH" -> {
                // Meter to Feet
                val feet = num * 3.28084
                String.format(Locale.US, "%.3f ft", feet)
            }
            "TEMP" -> {
                // Celsius to Fahrenheit
                val fahr = (num * 9/5) + 32
                String.format(Locale.US, "%.2f °F", fahr)
            }
            "WEIGHT" -> {
                // Kg to Lbs
                val lbs = num * 2.20462
                String.format(Locale.US, "%.3f lbs", lbs)
            }
            else -> "0.0"
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Einheiten-Umrechner", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = { viewModel.isSecretUnlocked = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Tresor", tint = Color.White.copy(alpha = 0.15f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1C25))
            )
        },
        containerColor = Color(0xFF0F0E13)
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Tabs Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val types = listOf("LENGTH" to "Länge", "TEMP" to "Temp", "WEIGHT" to "Gewicht")
                    types.forEach { (type, label) ->
                        val isSel = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFF6200EE) else Color.Transparent)
                                .clickable { selectedType = type; inputVal = ""; outputVal = "" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Display Area
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val inputLabel = when (selectedType) {
                            "LENGTH" -> "Meter"
                            "TEMP" -> "Celsius"
                            "WEIGHT" -> "Kilogramm"
                            else -> "Wert"
                        }
                        Text(inputLabel, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text(inputVal.ifEmpty { "0" }, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val outputLabel = when (selectedType) {
                            "LENGTH" -> "Feet (ft)"
                            "TEMP" -> "Fahrenheit (°F)"
                            "WEIGHT" -> "Pound (lbs)"
                            else -> "Resultat"
                        }
                        Text(outputLabel, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text(outputVal.ifEmpty { " " }, color = Color(0xFFBF5AF2), fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            // Conversion Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val btns = listOf(
                    listOf("7", "8", "9", "⌫"),
                    listOf("4", "5", "6", "C"),
                    listOf("1", "2", "3", "Convert"),
                    listOf("0", ".", "-", "=")
                )
                
                btns.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { action ->
                            val isAction = action in listOf("C", "⌫", "Convert", "=")
                            val isEquals = action == "=" || action == "Convert"
                            
                            val bg = when {
                                isEquals -> Color(0xFF6200EE)
                                isAction -> Color.White.copy(alpha = 0.12f)
                                else -> Color.White.copy(alpha = 0.05f)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(if (action == "Convert") 2f else 1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        when (action) {
                                            "C" -> {
                                                inputVal = ""
                                                outputVal = ""
                                            }
                                            "⌫" -> {
                                                if (inputVal.isNotEmpty()) inputVal = inputVal.dropLast(1)
                                            }
                                            "Convert", "=" -> {
                                                performConvert()
                                            }
                                            else -> {
                                                inputVal += action
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    action,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SECURE MOCK GPS SCREEN ---
@Composable
fun MockGpsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val presets = listOf(
        Triple(48.8584f, 2.2945f, "Eiffelturm, Paris"),
        Triple(40.6892f, -74.0445f, "Freiheitsstatue, NYC"),
        Triple(37.2431f, -115.793f, "Area 51, Nevada"),
        Triple(41.8902f, 12.4922f, "Kolosseum, Rom"),
        Triple(40.4319f, 116.5704f, "Chinesische Mauer"),
        Triple(52.5200f, 13.4050f, "Brandburger Tor, Berlin")
    )
    
    var customLat by remember { mutableStateOf(viewModel.mockGpsLat.toString()) }
    var customLng by remember { mutableStateOf(viewModel.mockGpsLng.toString()) }
    
    // Pulse animation
    val pulseTrans = rememberInfiniteTransition(label = "gpsPulse")
    val scalePulse by pulseTrans.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "spPulse"
    )
    val alphaPulse by pulseTrans.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "apPulse"
    )

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Mock GPS Spoofen", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0D11))
            )
        },
        containerColor = Color(0xFF0F1014)
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Radar display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16181D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animating scope Radar Circle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A1C12)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Expanding glow pulse
                        if (viewModel.isMockGpsActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scalePulse
                                        scaleY = scalePulse
                                        alpha = alphaPulse
                                    }
                                    .border(2.dp, Color(0xFF33FF77), CircleShape)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isMockGpsActive) Color(0xFF22C55E) else Color(0xFF6B7280)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (viewModel.isMockGpsActive) "GPS STANDORT AKTIV DESIN?!" else "GPS SPOOFER AUSGESCHALTET",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (viewModel.isMockGpsActive) Color(0xFF22C55E) else Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.mockGpsLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Lat: ${viewModel.mockGpsLat} | Lng: ${viewModel.mockGpsLng}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Presets Selection
            Text("STANDORT PRESETS WAHL:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (lat, lng, name) ->
                    val isCurrent = viewModel.mockGpsLabel == name && viewModel.isMockGpsActive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) Color(0xFF1B3A2B) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isCurrent) Color(0xFF22C55E) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.saveMockGps(lat, lng, name, true)
                                customLat = lat.toString()
                                customLng = lng.toString()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isCurrent) Color(0xFF22C55E) else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                                Text("Lat: $lat, Lng: $lng", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF22C55E))
                        }
                    }
                }
            }

            // Custom coordinates
            Text("MANUELLER STANDORT (KOORDINATEN):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16181D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = customLat,
                        onValueChange = { customLat = it },
                        label = { Text("Breitengrad (Latitude)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    TextField(
                        value = customLng,
                        onValueChange = { customLng = it },
                        label = { Text("Längengrad (Longitude)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveMockGps(
                                    viewModel.mockGpsLat,
                                    viewModel.mockGpsLng,
                                    viewModel.mockGpsLabel,
                                    false
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abschalten", color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                val la = customLat.toFloatOrNull() ?: 0.0f
                                val lo = customLng.toFloatOrNull() ?: 0.0f
                                viewModel.saveMockGps(la, lo, "Eigenes Koordinatenziel", true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Mocken & Speichern", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- SECURE INTRUDER LOGS (Gallery of break-ins) ---
@Composable
fun IntruderPhotosScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val intruderList by viewModel.intruderPhotos.collectAsState()
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Einbruchs-Beweise", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearAllIntruderPhotos() }) {
                        Text("Mölen leeren", color = Color(0xFFFF5252))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F12))
            )
        },
        containerColor = Color(0xFF08080B)
    ) { p ->
        if (intruderList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(p),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF22C55E).copy(alpha = 0.2f), modifier = Modifier.size(100.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gerät vollständig abgesichert!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Bisher gab es keine unbefugten Zugriffe. Jedes Mal, wenn das Geheim-Menü entsperrt wird, wird lautlos ein Selfie deines Smartphones gespeichert.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(p)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(intruderList) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141A)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdf = SimpleDateFormat("dd.MMM yyyy - HH:mm:ss", Locale.GERMANY)
                                Text(
                                    text = sdf.format(Date(entry.timestamp)),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252),
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { viewModel.deleteIntruderPhoto(entry.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (entry.isMocked) {
                                // Dynamic stylized mock log telemetry card instead of camera picture
                                val parts = entry.filePath.split("|")
                                val dateStr = parts.getOrNull(1) ?: ""
                                val ipStr = parts.getOrNull(2) ?: "192.168.1.134"
                                val devStr = parts.getOrNull(3) ?: "Unbekanntes Gerät"
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF030D05))
                                        .border(1.dp, Color(0xFF00FF44).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Simulated hacker radar sweep behind text
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = Color(0xFF00FF44).copy(alpha = 0.08f),
                                            radius = size.minDimension / 1.5f,
                                            center = Offset(size.width, size.height)
                                        )
                                        drawCircle(
                                            color = Color(0xFF00FF44).copy(alpha = 0.04f),
                                            radius = size.minDimension / 3f,
                                            center = Offset(size.width, size.height)
                                        )
                                    }
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "LOG-MESSUNG #00${entry.id}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF00FF44),
                                                fontSize = 11.sp
                                            )
                                            Text("SICHERUNGSAKTIV", fontFamily = FontFamily.Monospace, color = Color(0xFF00FF44).copy(alpha = 0.5f), fontSize = 9.sp)
                                        }
                                        
                                        Column {
                                            Text("Eindringling: $devStr", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Netzwerk IP: $ipStr", fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                            Text("Sensor: Frontkamera Permission-Backup-Auslöser", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                        }
                                    }
                                }
                            } else {
                                // Draw actual camera captured photo on device disk
                                val imgFile = File(entry.filePath)
                                if (imgFile.exists()) {
                                    AsyncImage(
                                        model = imgFile,
                                        contentDescription = "Selbstauslöser Foto",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.04f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Foto-Datei wurde gelöscht oder verschoben.", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SECURE SPLIT SCREEN CO-OP PONG GAME ---
@Composable
fun CoopSplitScreenPong(onBack: () -> Unit) {
    val screenWidth = 360f
    val screenHeight = 600f
    
    // Play area parameters
    var ballX by remember { mutableStateOf(180f) }
    var ballY by remember { mutableStateOf(300f) }
    var ballVx by remember { mutableStateOf(3.5f) }
    var ballVy by remember { mutableStateOf(-3.5f) }
    
    // Player Paddles (X Coordinate, Top and Bottom centered moving horizontally)
    var paddleTopX by remember { mutableStateOf(140f) }
    var paddleBottomX by remember { mutableStateOf(140f) }
    val paddleWidth = 80f
    val paddleHeight = 15f
    
    // Scores
    var scoreTop by remember { mutableStateOf(0) }
    var scoreBottom by remember { mutableStateOf(0) }
    
    var isRunning by remember { mutableStateOf(false) }
    var matchResultMsg by remember { mutableStateOf("Tippe zum Spielen!") }
    
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                delay(16) // ~60fps
                
                // physics step
                ballX += ballVx
                ballY += ballVy
                
                // Wall collisions (side walls)
                if (ballX <= 10f) {
                    ballX = 10f
                    ballVx = -ballVx
                }
                if (ballX >= screenWidth - 10f) {
                    ballX = screenWidth - 10f
                    ballVx = -ballVx
                }
                
                // Paddle top collision (at the top end, say Y = 40)
                if (ballVy < 0 && ballY >= 35f && ballY <= 35f + paddleHeight) {
                    if (ballX >= paddleTopX && ballX <= paddleTopX + paddleWidth) {
                        ballVy = -ballVy * 1.05f // accelerate slightly
                        ballVx += ((ballX - (paddleTopX + paddleWidth / 2f)) / 10f) // add horizontal steer depending where it hits
                    }
                }
                
                // Paddle bottom collision (at bottom end, say Y = 540)
                if (ballVy > 0 && ballY >= 530f - paddleHeight && ballY <= 530f) {
                    if (ballX >= paddleBottomX && ballX <= paddleBottomX + paddleWidth) {
                        ballVy = -ballVy * 1.05f
                        ballVx += ((ballX - (paddleBottomX + paddleWidth / 2f)) / 10f)
                    }
                }
                
                // Miss top boundary (Bottom scores!)
                if (ballY < 15f) {
                    scoreBottom++
                    isRunning = false
                    matchResultMsg = "SÜDMEN (UNTEN) GEWINNT DER PUNKT!"
                    // Reset ball
                    ballX = 180f
                    ballY = 300f
                    ballVx = listOf(-3.5f, 3.5f).random()
                    ballVy = 3.5f
                }
                
                // Miss bottom boundary (Top scores!)
                if (ballY > 565f) {
                    scoreTop++
                    isRunning = false
                    matchResultMsg = "NORDMEN (OBEN) GEWINNT DER PUNKT!"
                    ballX = 180f
                    ballY = 300f
                    ballVx = listOf(-3.5f, 3.5f).random()
                    ballVy = -3.5f
                }
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("CO-OP SPLIT PONG", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scoreTop = 0
                        scoreBottom = 0
                        isRunning = false
                        matchResultMsg = "Tippe zum Spielen!"
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF15151A))
            )
        },
        containerColor = Color.Black
    ) { p ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .background(Color.Black)
        ) {
            // Draw Playboard + Control zones
            Column(modifier = Modifier.fillMaxSize()) {
                // North/Top player touchpad control zone (Moves top paddle)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F0B15).copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                paddleTopX = (paddleTopX + dragAmount.x).coerceIn(0f, screenWidth - paddleWidth)
                            }
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        "<<< NORD SPIELER STEUERUNG (Wischen) >>>",
                        fontSize = 11.sp,
                        color = Color(0xFFA855F7).copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                // South/Bottom player touchpad control zone (Moves bottom paddle)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0B1215).copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                paddleBottomX = (paddleBottomX + dragAmount.x).coerceIn(0f, screenWidth - paddleWidth)
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        "<<< SÜD SPIELER STEUERUNG (Wischen) >>>",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981).copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Visual Overlay lines & drawings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleWidth = size.width / screenWidth
                val scaleHeight = size.height / screenHeight
                
                // Center dividing dashed line (The split barrier)
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 4f
                )
                
                // Draw TOP paddle (Nord - violet)
                drawRoundRect(
                    color = Color(0xFFA855F7),
                    size = androidx.compose.ui.geometry.Size(paddleWidth * scaleWidth, paddleHeight * scaleHeight),
                    topLeft = Offset(paddleTopX * scaleWidth, 35f * scaleHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                
                // Draw BOTTOM paddle (Süd - green)
                drawRoundRect(
                    color = Color(0xFF10B981),
                    size = androidx.compose.ui.geometry.Size(paddleWidth * scaleWidth, paddleHeight * scaleHeight),
                    topLeft = Offset(paddleBottomX * scaleWidth, (530f - paddleHeight) * scaleHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                
                // Draw BALL (pulsating custom orange)
                drawCircle(
                    color = Color(0xFFFF5722),
                    radius = 9f * scaleWidth,
                    center = Offset(ballX * scaleWidth, ballY * scaleHeight)
                )
            }
            
            // Middle HUD containing score and play/pause button
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top score
                    Text(
                        scoreTop.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA855F7)
                    )
                    
                    Text("VS", fontSize = 16.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    
                    // Bottom score
                    Text(
                        scoreBottom.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                }
                
                if (!isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { isRunning = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(matchResultMsg, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
