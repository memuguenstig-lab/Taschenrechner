package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.R
import com.example.data.Calculation
import com.example.data.ChatMessage
import com.example.data.GeneratedImage
import com.example.ui.*
import com.example.ui.games.FlappyBirdGame
import com.example.ui.games.MemoryGame
import com.example.ui.games.SlotMachineGame
import com.example.ui.games.SnakeGame
import com.example.ui.games.TetrisGame
import com.example.ui.games.TicTacToeGame
import com.example.ui.games.MemoryGame
import com.example.ui.games.DriftCarGame
import com.example.ui.games.CrowdRunnerGame
import com.example.ui.games.RhythmTapperGame
import com.example.ui.games.RetroSpaceShooterGame
import com.example.ui.games.LocalWifiFileServer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainAppScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    if (viewModel.showBrowserHistorySecretView) {
        BrowserHistoryScreen(viewModel, onBack = { viewModel.showBrowserHistorySecretView = false })
    } else if (viewModel.isSecretUnlocked) {
        key(viewModel.isSecureSecretUnlocked) {
            SecretArcadeDashboard(viewModel, modifier)
        }
    } else {
        when (viewModel.disguiseMode) {
            DisguiseMode.CONVERTER -> ConverterScreen(viewModel)
            DisguiseMode.NOTEPAD -> NotepadScreen(viewModel)
            else -> CalculatorScreen(viewModel, modifier)
        }
    }
}

@Composable
fun CalculatorScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val calculations by viewModel.calculations.collectAsStateWithLifecycle()
    var showCalcSettings by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val palette = getThemePalette(viewModel.appTheme)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = palette.opKeysBg,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCalcSettings = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Einstellungen",
                            tint = palette.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background
                )
            )
        },
        containerColor = palette.background
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Panel: Display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // Typed Math expression formula
                    AnimatedCalculatorText(
                        text = viewModel.calculatorInput.ifEmpty { "0" },
                        fontSize = 32.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        testTag = "calculator_input_display",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Computed Result
                    AnimatedCalculatorText(
                        text = viewModel.calculatorOutput.ifEmpty { "" },
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.02).sp,
                        testTag = "calculator_result_display",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }

                // Right Panel: Keypad Buttons
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val buttons = listOf(
                        listOf("C", "⌫", "%", "/"),
                        listOf("7", "8", "9", "*"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", "00", ".", "=")
                    )
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            row.forEach { char ->
                                key(char) {
                                    val isOperator = char in listOf("/", "*", "-", "+")
                                    val isClear = char in listOf("C", "⌫", "%")
                                    val isEquals = char == "="

                                    val btnBg = when {
                                        isEquals -> palette.equalsKeyBg
                                        isOperator -> palette.opKeysBg
                                        isClear -> palette.clearKeysBg
                                        else -> palette.numKeysBg
                                    }
                                    val btnTextColor = when {
                                        isEquals -> palette.equalsKeyText
                                        isOperator -> palette.accentColor
                                        else -> palette.textPrimary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(btnBg)
                                            .clickable {
                                                if (isEquals) viewModel.onCalculatorEvaluate()
                                                else viewModel.onCalculatorChar(char)
                                            }
                                            .testTag("calc_btn_$char"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val displayChar = when(char) {
                                            "/" -> "÷"
                                            "*" -> "×"
                                            "-" -> "−"
                                            else -> char
                                        }
                                        Text(
                                            text = displayChar,
                                            fontSize = 26.sp,
                                            fontWeight = if (isOperator || isEquals) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isOperator && !isEquals && !isClear) Color.White else btnTextColor,
                                            fontFamily = if (palette.isRetroMono) FontFamily.Monospace else FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f) // Fixed display weight
                        .padding(horizontal = 24.dp)
                        .padding(top = 40.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // History Line (Ghost expression)
                    AnimatedCalculatorText(
                        text = viewModel.calculatorInput.ifEmpty { "0" },
                        fontSize = 40.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        testTag = "calculator_input_display",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Computed Result
                    AnimatedCalculatorText(
                        text = viewModel.calculatorOutput.ifEmpty { "" },
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.02).sp,
                        testTag = "calculator_result_display",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }

                // Keypad layout
                val buttons = listOf(
                    listOf("C", "⌫", "%", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", "00", ".", "=")
                )

                // Standard Keypad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                        .weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            row.forEach { char ->
                                key(char) {
                                    val isOperator = char in listOf("/", "*", "-", "+")
                                    val isClear = char in listOf("C", "⌫", "%")
                                    val isEquals = char == "="

                                    val btnBg = when {
                                        isEquals -> palette.equalsKeyBg
                                        isOperator -> palette.opKeysBg
                                        isClear -> palette.clearKeysBg
                                        else -> palette.numKeysBg
                                    }

                                    val btnTextColor = when {
                                        isEquals -> palette.equalsKeyText
                                        isOperator -> palette.accentColor
                                        else -> palette.textPrimary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f) // Account for spacing in weight
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(btnBg)
                                            .clickable {
                                                if (isEquals) viewModel.onCalculatorEvaluate()
                                                else viewModel.onCalculatorChar(char)
                                            }
                                            .testTag("calc_btn_$char"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val displayChar = when(char) {
                                            "/" -> "÷"
                                            "*" -> "×"
                                            "-" -> "−"
                                            else -> char
                                        }
                                        Text(
                                            text = displayChar,
                                            fontSize = 32.sp,
                                            fontWeight = if (isOperator || isEquals) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isOperator && !isEquals && !isClear) Color.White else btnTextColor,
                                            fontFamily = if (palette.isRetroMono) FontFamily.Monospace else FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showCalcSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showCalcSettings = false },
                contentAlignment = Alignment.Center
            ) {
                // We intercept click to prevent dismissing when clicking inside the dialog
                Box(modifier = Modifier.clickable(enabled = false) {}) {
                    val containerColor = Color(0xFF18181b)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF3f3f46))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                IconButton(onClick = { showCalcSettings = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("MATH CONFIGURATION", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Scientific Mode", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("Advanced functions", color = palette.textSecondary, fontSize = 14.sp)
                                }
                                Switch(checked = false, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = palette.background, checkedTrackColor = palette.opKeysBg, uncheckedTrackColor = Color(0xFF3f3f46), uncheckedThumbColor = Color(0xFFA1A1AA)))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("High Precision", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("128-bit floating point", color = palette.textSecondary, fontSize = 14.sp)
                                }
                                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = palette.background, checkedTrackColor = palette.opKeysBg))
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0xFF3f3f46))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("UPDATE CENTER", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF27272a), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3f3f46).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text("Firmware Version", color = Color.White, fontWeight = FontWeight.Medium)
                                        Text("v2.4.1-stable", color = palette.textSecondary, fontSize = 12.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(palette.opKeysBg.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .border(1.dp, palette.opKeysBg.copy(alpha=0.2f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Up to date", color = palette.opKeysBg, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3f3f46)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("CHECK FOR UPDATES", fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0xFF3f3f46))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("PRIVACY & SECURITY", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Vault Protocol", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("Enable sequence triggers", color = palette.textSecondary, fontSize = 14.sp)
                                }
                                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = palette.background, checkedTrackColor = palette.opKeysBg))
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { showCalcSettings = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.textPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done", color = palette.background, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SECRET ARCADE DASHBOARD ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecretArcadeDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (showSettingsDialog) {
        ThemeSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false }, onTriggerUpdate = { showUpdateDialog = true })
    }

    if (showUpdateDialog) {
        Dialog(onDismissRequest = { showUpdateDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.9f else 0.95f)
                    .fillMaxHeight(if (isLandscape) 0.95f else 0.85f)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF151518),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Software-Update",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showUpdateDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Schließen",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AppUpdateCenter(viewModel)
                }
            }
        }
    }

    SilentCameraScanner(viewModel)

    BackHandler {
        if (viewModel.currentSecretSection == SecretSection.GAMES && viewModel.activeGame != GameType.HOME) {
            viewModel.activeGame = GameType.HOME
        } else if (viewModel.currentSecretSection == SecretSection.GALLERY) {
            viewModel.currentSecretSection = SecretSection.CHAT
        } else {
            viewModel.lockSecretMode()
        }
    }

    val pages = if (viewModel.isSecureSecretUnlocked) {
        listOf(
            SecretSection.SETTINGS
        )
    } else {
        listOf(
            SecretSection.STATS,
            SecretSection.GAMES,
            SecretSection.CHAT,
            SecretSection.BROWSER,
            SecretSection.WATCH
        )
    }
    
    val initialPage = pages.indexOf(viewModel.currentSecretSection).coerceAtLeast(0)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )

    LaunchedEffect(viewModel.currentSecretSection) {
        val targetIdx = pages.indexOf(viewModel.currentSecretSection)
        if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIdx)
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val currentPageEnum = pages[pagerState.currentPage]
            if (viewModel.currentSecretSection != currentPageEnum && viewModel.currentSecretSection != SecretSection.GALLERY) {
                viewModel.currentSecretSection = currentPageEnum
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (viewModel.currentSecretSection != SecretSection.GALLERY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF0F172A)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00FFCC),
                        edgePadding = 8.dp,
                        modifier = Modifier.weight(1f),
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = Color(0xFF00FFCC)
                                )
                            }
                        }
                    ) {
                        pages.forEachIndexed { index, section ->
                            val icon = when (section) {
                                SecretSection.STATS -> Icons.Default.BarChart
                                SecretSection.GAMES -> Icons.Default.SportsEsports
                                SecretSection.CHAT -> Icons.Default.SmartToy
                                SecretSection.BROWSER -> Icons.Default.Public
                                SecretSection.WATCH -> Icons.Default.PlayCircle
                                SecretSection.SETTINGS -> Icons.Default.Settings
                                else -> Icons.Default.Circle
                            }
                            val label = when (section) {
                                SecretSection.STATS -> "Stats"
                                SecretSection.GAMES -> "Spiele"
                                SecretSection.CHAT -> "KI"
                                SecretSection.BROWSER -> "Web"
                                SecretSection.WATCH -> "Watch"
                                SecretSection.SETTINGS -> "Settings"
                                else -> ""
                            }
                            androidx.compose.material3.Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { viewModel.currentSecretSection = section },
                                icon = { Icon(icon, contentDescription = label) },
                                text = { Text(label, fontSize = 10.sp) },
                                selectedContentColor = Color(0xFF00FFCC),
                                unselectedContentColor = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    if (!viewModel.isSecureSecretUnlocked) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Einstellungen",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if ((viewModel.currentSecretSection == SecretSection.GAMES && viewModel.activeGame != GameType.HOME) || 
                viewModel.currentSecretSection == SecretSection.BROWSER ||
                viewModel.currentSecretSection == SecretSection.WATCH) {
                FloatingActionButton(
                    onClick = { viewModel.isFullScreen = !viewModel.isFullScreen },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Vollbildschirm"
                    )
                }
            }
        },
        containerColor = Color(0xFF15171C)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (viewModel.currentSecretSection == SecretSection.GALLERY) {
                GalleryTabScreen(viewModel)
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    beyondViewportPageCount = pages.size
                ) { page ->
                    when (pages[page]) {
                        SecretSection.STATS -> StatsTabScreen(viewModel)
                        SecretSection.GAMES -> GamesTabScreen(viewModel)
                        SecretSection.CHAT -> ChatBotTabScreen(viewModel)
                        SecretSection.BROWSER -> BrowserTabScreen(viewModel)
                        SecretSection.WATCH -> WatchTabScreen()
                        SecretSection.SETTINGS -> SecretSettingsTabScreen(viewModel)
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}


// --- TAB SUB-SCREENS ---

@Composable
fun StatsTabScreen(viewModel: AppViewModel) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SpionageDashboardView(viewModel = viewModel)
        }
    }
}

@Composable
fun SecretSettingsTabScreen(viewModel: AppViewModel) {
    if (viewModel.activeGame == GameType.INTRUDER_PHOTOS) {
        IntruderPhotosScreen(
            viewModel = viewModel,
            onBack = { viewModel.activeGame = GameType.HOME }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Geheime Einstellungen & Beweise", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Silent Photo Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Stiller Fotomodus & Screenshot", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Macht jede Minute heimlich Foto (V/H) + Screenshot, wenn App offen.", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            androidx.compose.material3.Switch(
                checked = viewModel.isSecretPhotoEnabled,
                onCheckedChange = { viewModel.updateSecretPhotoEnabled(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF3366),
                    checkedTrackColor = Color(0xFFFF3366).copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Panic Lock Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Panik-Verriegelung (Face-Down/Schütteln)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Schließt die geheime Ansicht sofort, sobald das Handy geschüttelt oder mit dem Bildschirm nach unten hingesetzt wird.", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            androidx.compose.material3.Switch(
                checked = viewModel.isPanicLockEnabled,
                onCheckedChange = { viewModel.updatePanicLockEnabled(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF10B981),
                    checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("panic_lock_switch")
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Intruder Gallery Button
        Button(
            onClick = { viewModel.activeGame = GameType.INTRUDER_PHOTOS },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF00FFCC))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Beweisfotos anzeigen", fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
fun GamesTabScreen(viewModel: AppViewModel) {
    var showUpdateDialogSecret by remember { mutableStateOf(false) }
    var showNamePromptForGame by remember { mutableStateOf<GameType?>(null) }
    var temporaryPlayerName by remember { mutableStateOf("") }

    fun isActualPlayableGame(gameType: GameType): Boolean {
        return when (gameType) {
            GameType.HOME, GameType.MOCK_GPS, GameType.INTRUDER_PHOTOS, GameType.DISGUISE_SETTINGS, GameType.WIFI_SERVER -> false
            else -> true
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (showNamePromptForGame != null) {
        val targetGame = showNamePromptForGame!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNamePromptForGame = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Identifizierung",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "IDENTIFIKATION ERFORDERLICH",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Gib vor Spielstart deinen Spionage-Decknamen ein, um deinen Score und Ränge im Geheimbereich zu speichern.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = temporaryPlayerName,
                        onValueChange = { temporaryPlayerName = it },
                        label = { Text("Agenten-Deckname", fontSize = 11.sp) },
                        placeholder = { Text("Agent James") },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00FFCC),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("agent_name_input")
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (temporaryPlayerName.isNotBlank()) {
                            viewModel.playerAgentName = temporaryPlayerName.trim()
                            viewModel.activeGame = targetGame
                            showNamePromptForGame = null
                        }
                    },
                    enabled = temporaryPlayerName.isNotBlank(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("start_simulation_button")
                ) {
                    Text("BEITRETEN 🚀", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNamePromptForGame = null }) {
                    Text("Abbrechen", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUpdateDialogSecret) {
        Dialog(onDismissRequest = { showUpdateDialogSecret = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.9f else 0.95f)
                    .fillMaxHeight(if (isLandscape) 0.95f else 0.85f)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF151518),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Software-Update",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showUpdateDialogSecret = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Schließen",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AppUpdateCenter(viewModel)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewModel.activeGame,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "games_navigation",
            modifier = Modifier.fillMaxSize()
        ) { game ->
            when (game) {
            GameType.HOME -> GamesCatalogView(
                snakeHighScore = viewModel.snakeHighScore,
                tetrisHighScore = viewModel.tetrisHighScore,
                flappyBirdHighScore = viewModel.flappyBirdHighScore,
                dinoHighScore = viewModel.dinoHighScore,
                ticTacToeWins = viewModel.ticTacToeWins,
                memoryHighScore = viewModel.memoryHighScore,
                viewModel = viewModel,
                onSelect = { selectedGame ->
                    if (isActualPlayableGame(selectedGame)) {
                        showNamePromptForGame = selectedGame
                        temporaryPlayerName = viewModel.playerAgentName
                    } else {
                        viewModel.activeGame = selectedGame
                    }
                }
            )
            GameType.SNAKE -> SnakeGame(
                highScore = viewModel.snakeHighScore,
                onHighScoreUpdate = { viewModel.snakeHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.TETRIS -> TetrisGame(
                highScore = viewModel.tetrisHighScore,
                onHighScoreUpdate = { viewModel.tetrisHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.FLAPPYBIRD -> FlappyBirdGame(
                highScore = viewModel.flappyBirdHighScore,
                onHighScoreUpdate = { viewModel.flappyBirdHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.TICTACTOE -> TicTacToeGame(
                wins = viewModel.ticTacToeWins,
                onWinUpdate = { viewModel.ticTacToeWins = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.MEMORY -> MemoryGame(
                highScore = viewModel.memoryHighScore,
                onHighScoreUpdate = { viewModel.memoryHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.SLOTS -> SlotMachineGame(
                coins = viewModel.coins,
                onCoinsUpdate = { viewModel.coins = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.BLACKJACK -> com.example.ui.games.BlackjackGame(
                coins = viewModel.coins,
                onCoinsUpdate = { viewModel.coins = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.MINES -> com.example.ui.games.MinesGame(
                coins = viewModel.coins,
                onCoinsUpdate = { viewModel.coins = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.DINO -> com.example.ui.games.DinoGame(
                highScore = viewModel.dinoHighScore,
                onHighScoreUpdate = { viewModel.dinoHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.TWO_THOUSAND_FORTY_EIGHT -> com.example.ui.games.TwoThousandFortyEightGame(
                highScore = viewModel.twoThousandFortyEightHighScore,
                onHighScoreUpdate = { viewModel.twoThousandFortyEightHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.PONG -> com.example.ui.games.PongGame(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.DOTS_AND_BOXES -> com.example.ui.games.DotsAndBoxesGame(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.MOCK_GPS -> MockGpsScreen(
                viewModel = viewModel,
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.INTRUDER_PHOTOS -> IntruderPhotosScreen(
                viewModel = viewModel,
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.DISGUISE_SETTINGS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    ThemeSettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.activeGame = GameType.HOME },
                        onTriggerUpdate = { showUpdateDialogSecret = true }
                    )
                }
            }
            GameType.COOP_SPLIT_SCREEN -> CoopSplitScreenPong(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.COOP_SPLIT_TUG_OF_WAR -> CoopSplitScreenTugOfWar(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.COOP_SPLIT_REACTION -> CoopSplitScreenReaction(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.DRIFT_CAR -> DriftCarGame(
                highScore = viewModel.driftHighScore,
                onHighScoreUpdate = { viewModel.driftHighScore = it },
                coins = viewModel.coins,
                onCoinsUpdate = { viewModel.coins = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.CROWD_RUNNER -> CrowdRunnerGame(
                highScore = viewModel.crowdHighScore,
                onHighScoreUpdate = { viewModel.crowdHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.RHYTHM_TAPPER -> RhythmTapperGame(
                highScore = viewModel.rhythmHighScore,
                onHighScoreUpdate = { viewModel.rhythmHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.SPACE_SHOOTER -> RetroSpaceShooterGame(
                highScore = viewModel.spaceHighScore,
                onHighScoreUpdate = { viewModel.spaceHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.WIFI_SERVER -> LocalWifiFileServer(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
        }
    }

    if (viewModel.activeGame != GameType.HOME) {
        androidx.compose.material3.IconButton(
            onClick = { viewModel.lockSecretMode() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Schließen & Sperren",
                tint = Color.White
            )
        }
    }
}
}

@Composable
fun GamesCatalogView(
    snakeHighScore: Int,
    tetrisHighScore: Int,
    flappyBirdHighScore: Int,
    dinoHighScore: Int,
    ticTacToeWins: Int,
    memoryHighScore: Int,
    viewModel: AppViewModel,
    onSelect: (GameType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: SPIELESAMMLUNG (Games Collection) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Spiele",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🕹️ SPIELESAMMLUNG",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        item {
            Column {
                val gamesList = listOf(
                    Triple("BEAT SLAM & CO.", "Triff fallende Beams im Takt!", "Tapper" to GameType.RHYTHM_TAPPER),
                    Triple("SPACE SHOOTER", "Verteidige das All im Retro-Style!", "Retro" to GameType.SPACE_SHOOTER),
                    Triple("WI-FI SHARE", "Dateien per Webbrowser übertragen!", "Utility" to GameType.WIFI_SERVER),
                    Triple("CROWD RUNNER", "Sammle so viele Männer wie möglich!", "Crowd" to GameType.CROWD_RUNNER),
                    Triple("DRIFT CAR", "Drifte ohne zu crashen!", "Drift" to GameType.DRIFT_CAR),
                    Triple("CO-OP PONG", "1v1 Split Screen Tischtennis!", "Split" to GameType.COOP_SPLIT_SCREEN),
                    Triple("CO-OP TAUZIEHEN", "Tap War: Drücke so schnell du kannst!", "Split" to GameType.COOP_SPLIT_TUG_OF_WAR),
                    Triple("CO-OP REAKTION", "Wer reagiert schneller bei Grün?", "Split" to GameType.COOP_SPLIT_REACTION),
                    Triple("SLOT MACHINE", "Drehe die Rollen für den Jackpot!", "Slots" to GameType.SLOTS),
                    Triple("BLACKJACK", "Zieh Karten und schlag den Dealer!", "21" to GameType.BLACKJACK),
                    Triple("MINES", "Sammle Edelsteine, meide Minen!", "Mines" to GameType.MINES),
                    Triple("DINO JUMP", "Springe über Kakteen!", "🏆 Rekord: $dinoHighScore" to GameType.DINO),
                    Triple("2048", "Kombiniere gleiche Zahlen!", "Zahlen" to GameType.TWO_THOUSAND_FORTY_EIGHT),
                    Triple("PONG", "Der absolute Retro-Klassiker!", "Pong" to GameType.PONG),
                    Triple("KÄSTCHENSPIEL", "Verbinde Punkte, sichere Felder!", "Pass & Play" to GameType.DOTS_AND_BOXES),
                    Triple("TIC TAC TOE", "Drei-In-Einer-Reihe Match!", "🏆 Siege: $ticTacToeWins" to GameType.TICTACTOE),
                    Triple("MEMORY PAIRS", "Finde alle passenden Paare!", "🏆 Rekord: ${if (memoryHighScore > 0) memoryHighScore else "-"}" to GameType.MEMORY),
                    Triple("SNAKE CLASSIC", "Sammle Äpfel und wachse!", "🏆 Rekord: $snakeHighScore" to GameType.SNAKE),
                    Triple("TETRIS BLOCKS", "Rotiere herabfallende Blöcke!", "🏆 Rekord: $tetrisHighScore" to GameType.TETRIS),
                    Triple("FLAPPY BIRD", "Durchqueren der Rohre!", "🏆 Rekord: $flappyBirdHighScore" to GameType.FLAPPYBIRD)
                )

                val icons = listOf(
                    Icons.Default.MusicNote,
                    Icons.Default.Navigation,
                    Icons.Default.Wifi,
                    Icons.Default.DirectionsRun,
                    Icons.Default.DirectionsCar,
                    Icons.Default.SportsTennis,
                    Icons.Default.PanTool,
                    Icons.Default.Timer,
                    Icons.Default.MonetizationOn,
                    Icons.Default.Style,
                    Icons.Default.Warning,
                    Icons.Default.DirectionsRun,
                    Icons.Default.GridOn,
                    Icons.Default.SportsTennis,
                    Icons.Default.BorderAll,
                    Icons.Default.Grid3x3,
                    Icons.Default.ViewModule,
                    Icons.Default.TrendingFlat,
                    Icons.Default.GridOn,
                    Icons.Default.Air
                )

                val colors = listOf(
                    Color(0xFFEC4899), Color(0xFF00FFCC), Color(0xFF3B82F6),
                    Color(0xFF3B82F6), Color(0xFFEF4444),
                    Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF10B981),
                    Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFFF59E0B),
                    Color(0xFF10B981), Color(0xFFEAB308), Color(0xFF3B82F6),
                    Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFFEC4899),
                    Color(0xFF10B981), Color(0xFFA855F7), Color(0xFFFACC15)
                )

                for (i in gamesList.indices step 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val data = gamesList[i]
                            GameCard(
                                title = data.first,
                                description = data.second,
                                highScoreText = data.third.first,
                                icon = icons[i],
                                accentColor = colors[i],
                                onClick = { onSelect(data.third.second) }
                            )
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (i + 1 < gamesList.size) {
                                val data = gamesList[i + 1]
                                GameCard(
                                    title = data.first,
                                    description = data.second,
                                    highScoreText = data.third.first,
                                    icon = icons[i + 1],
                                    accentColor = colors[i + 1],
                                    onClick = { onSelect(data.third.second) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: SPY/AGENT UTILITIES ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Spymaster",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🛡️ SPY & SECURITY CORNER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        item {
            Column {
                val toolsList = listOf(
                    Triple("MOCK GPS", "Aktiven GPS-Standort virtuell vortäuschen", "Standort" to GameType.MOCK_GPS),
                    Triple("TARNUNG", "Rechner-Layout und Faux-Verhalten config", "Theme/Tarn" to GameType.DISGUISE_SETTINGS),
                    Triple("SPLIT PONG", "Zwei-Spieler Split-Screen Pong Duell", "Split Duo" to GameType.COOP_SPLIT_SCREEN),
                    Triple("SPLIT TAUZIEHEN", "Zwei-Spieler Split-Screen Klick Duell", "Split Duo" to GameType.COOP_SPLIT_TUG_OF_WAR)
                )

                val toolIcons = listOf(
                    Icons.Default.LocationOn,
                    Icons.Default.Masks,
                    Icons.Default.PlayArrow,
                    Icons.Default.TouchApp
                )

                val toolColors = listOf(
                    Color(0xFF00FFCC),
                    Color(0xFFEAB308),
                    Color(0xFFFF5722),
                    Color(0xFF8B5CF6)
                )

                for (i in toolsList.indices step 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val data = toolsList[i]
                            GameCard(
                                title = data.first,
                                description = data.second,
                                highScoreText = data.third.first,
                                icon = toolIcons[i],
                                accentColor = toolColors[i],
                                onClick = { onSelect(data.third.second) }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (i + 1 < toolsList.size) {
                                val data = toolsList[i + 1]
                                GameCard(
                                    title = data.first,
                                    description = data.second,
                                    highScoreText = data.third.first,
                                    icon = toolIcons[i + 1],
                                    accentColor = toolColors[i + 1],
                                    onClick = { onSelect(data.third.second) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(
    title: String,
    description: String,
    highScoreText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(title) {
        isVisible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f, 
        animationSpec = tween(durationMillis = 500), label = "alpha"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable { onClick() }
            .testTag("game_card_$title"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f, fill = false).padding(top = 4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = description,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }
            
            Text(
                text = highScoreText,
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ChatBotTabScreen(viewModel: AppViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Scroll to end automatically on message count additions
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { _ -> focusManager.clearFocus() })
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(12.dp)
            ) {
                // Settings row inside bot bar: Mode toggle + Clear history buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern design button switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.isImageGenerationMode = !viewModel.isImageGenerationMode }
                    ) {
                        Switch(
                            checked = viewModel.isImageGenerationMode,
                            onCheckedChange = { viewModel.isImageGenerationMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isImageGenerationMode) "🎨 Bilderstellung Aktiv" else "💬 Chat-Modus Aktiv",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Clear Chat icon
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Chat leeren",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }

                // Chat Input box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = viewModel.chatInputText,
                        onValueChange = { viewModel.chatInputText = it },
                        placeholder = {
                            Text(
                                if (viewModel.isImageGenerationMode) "Was möchtest du zeichnen lassen?..." else "Schreibe dem KI-Bot...",
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111827),
                            unfocusedContainerColor = Color(0xFF111827),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                viewModel.sendChatMessage()
                                focusManager.clearFocus()
                            }
                        )
                    )

                    IconButton(
                        onClick = { 
                            viewModel.sendChatMessage()
                            focusManager.clearFocus()
                        },
                        enabled = viewModel.chatInputText.isNotBlank() && !viewModel.isBotResponding,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (viewModel.chatInputText.isNotBlank() && !viewModel.isBotResponding) Color(0xFF10B981) else Color(0xFF3F3F46),
                                shape = CircleShape
                            )
                            .testTag("chat_send_button")
                    ) {
                        if (viewModel.isBotResponding) {
                            CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Senden",
                                tint = if (viewModel.chatInputText.isNotBlank() && !viewModel.isBotResponding) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingVals ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingVals),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Bot",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Dein KI Chatbot ist bereit! \uD83E\uDD16",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tippe eine Nachricht oder wechsle unten auf \"Bilderstellung\", um fantastische digitale Kunst zu generieren und sogleich in deiner Galerie zu archivieren!",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingVals)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
            
            FloatingActionButton(
                onClick = { viewModel.currentSecretSection = SecretSection.GALLERY },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                containerColor = Color(0xFF10B981).copy(alpha = 0.8f),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galerie")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Color(0xFF10B981) else Color(0xFF1F2937)
    val textColor = Color.White
    val cornerRadius = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = cornerRadius,
            border = if (isUser) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Text Message
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp
                )

                // If this is a generative Image response
                if (!message.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Generated artwork",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTabScreen(viewModel: AppViewModel) {
    val images by viewModel.generatedImages.collectAsStateWithLifecycle()
    var selectedImageForZoom by remember { mutableStateOf<GeneratedImage?>(null) }

    if (images.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Keine Bilder",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Deine Galerie ist leer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Schreibe dem KI-Chatbot im \"Bilderstellung\"-Zweig und erzeuge fantastische Bilder, welche hier automatisch aufgereiht werden!",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "MEINE GENERIERTEN KUNSTWERKE \uD83C\uDFA8",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { image ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = { selectedImageForZoom = image },
                                onLongClick = { viewModel.deleteImageFromGallery(image.id) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = image.imageUrl,
                                contentDescription = image.prompt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Prompt overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = image.prompt,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Zoom Image interactive dialog popup modal representation
    selectedImageForZoom?.let { zoomImg ->
        Dialog(onDismissRequest = { selectedImageForZoom = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { selectedImageForZoom = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
                    }

                    AsyncImage(
                        model = zoomImg.imageUrl,
                        contentDescription = zoomImg.prompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Thema / Prompt:",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = zoomImg.prompt,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.deleteImageFromGallery(zoomImg.id)
                            selectedImageForZoom = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Bild löschen", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aus Galerie löschen", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WatchTabScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("TikTok", "YouTube", "Instagram")
    val urls = listOf("https://www.tiktok.com/explore", "https://www.youtube.com", "https://www.instagram.com")

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF0F0F11),
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty() && selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF10B981)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, color = if (selectedTabIndex == index) Color(0xFF10B981) else Color.White) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            SimpleWebScreen(url = urls[selectedTabIndex])
        }
    }
}

@Composable
fun AppUpdateCenter(viewModel: AppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val updateState by viewModel.updateManager.updateState.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Updates",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App-Update-Center",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            when (val state = updateState) {
                is com.example.utils.UpdateState.Idle -> {
                    Text(
                        text = "Aktuelle Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.updateManager.checkForUpdates()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auf Updates prüfen", fontWeight = FontWeight.Bold)
                    }
                }
                is com.example.utils.UpdateState.Checking -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Suche nach neueren Versionen...", color = Color.White)
                    }
                }
                is com.example.utils.UpdateState.UpdateAvailable -> {
                    Text(
                        text = "Update Verfügbar: v${state.versionName} (Build ${state.versionCode})",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Changelog:\n${state.changelog}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.updateManager.resetState() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abbrechen")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.updateManager.downloadAndInstallApk(state.apkUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is com.example.utils.UpdateState.Downloading -> {
                    Text(
                        text = "Lade neue Version herunter...",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(state.progress * 100).toInt()}% abgeschlossen",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                is com.example.utils.UpdateState.ReadyToInstall -> {
                    Text(
                        text = "Heruntergeladen und bereit!",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.updateManager.resetState() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abbrechen")
                        }
                        Button(
                            onClick = { viewModel.updateManager.installApk(state.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Installieren")
                        }
                    }
                }
                is com.example.utils.UpdateState.UpToDate -> {
                    Text(
                        text = "✓ Deine App ist aktuell!",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.updateManager.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Schließen", color = Color.White)
                    }
                }
                is com.example.utils.UpdateState.Error -> {
                    Text(
                        text = "Fehler: ${state.message}",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.updateManager.resetState() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Schließen")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.updateManager.checkForUpdates()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCalculatorText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    fontFamily: FontFamily = FontFamily.Monospace,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    testTag: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (targetState.length > initialState.length) {
                (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) { it / 2 } 
                        + fadeIn(tween(120)) 
                        + scaleIn(initialScale = 0.88f))
                    .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.96f))
            } else {
                (fadeIn(tween(120)) + scaleIn(initialScale = 1.05f))
                    .togetherWith(slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it / 2 } 
                        + fadeOut(tween(120)) 
                        + scaleOut(targetScale = 0.88f))
            }
        },
        label = testTag,
        modifier = modifier
    ) { displayedText ->
        Text(
            text = displayedText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SpionageDashboardView(viewModel: AppViewModel) {
    // Calculative Spy score
    val snakeScorePts = viewModel.snakeHighScore * 10
    val tetrisScorePts = viewModel.tetrisHighScore / 2
    val flappyScorePts = viewModel.flappyBirdHighScore * 50
    val dinoScorePts = viewModel.dinoHighScore * 2
    val memoryHighScore = viewModel.memoryHighScore
    val memoryScorePts = if (memoryHighScore > 0) (600 - memoryHighScore * 10).coerceAtLeast(0) else 0
    val ticTacToeScorePts = viewModel.ticTacToeWins * 100
    val driftScorePts = viewModel.driftHighScore * 2
    val crowdScorePts = viewModel.crowdHighScore
    val rhythmScorePts = viewModel.rhythmHighScore / 5
    val spaceScorePts = viewModel.spaceHighScore / 5
    val twoThousandFortyEightScorePts = viewModel.twoThousandFortyEightHighScore / 10

    val totalPoints = snakeScorePts + tetrisScorePts + flappyScorePts + dinoScorePts +
            memoryScorePts + ticTacToeScorePts + driftScorePts + crowdScorePts +
            rhythmScorePts + spaceScorePts + twoThousandFortyEightScorePts

    val (rank, rankDesc, nextRankThreshold, nextRankName) = when {
        totalPoints < 100 -> Quadruple("Rekrut", "Spielfeld absichern, Ausrüstung testen und erste Einsätze fliegen.", 100, "Techniker")
        totalPoints < 500 -> Quadruple("Techniker", "Errichten von abhörsicheren Datenleitungen und Spionage-Infrastruktur.", 500, "Schatten-Agent")
        totalPoints < 1500 -> Quadruple("Schatten-Agent", "Leise Infiltration feindlicher Rechenleistungen unter dem Radar.", 1500, "Geheimdienst-Experte")
        totalPoints < 3500 -> Quadruple("Geheimdienst-Experte", "Dechiffrierung hochkomplexer Datenströme und Steuerung verdeckter Ops.", 3500, "Doppelagent")
        else -> Quadruple("Doppelagent", "Legende des Agency-Netzwerks. Meister der Täuschung, Verschlüsselung und des globalen Zugriffs.", totalPoints, "Maximaler Rang")
    }

    val progress = if (nextRankThreshold == totalPoints || nextRankThreshold == 0) 1.0f else (totalPoints.toFloat() / nextRankThreshold.toFloat()).coerceIn(0f, 1f)

    var showAchievements by remember { mutableStateOf(false) }

    // Achievements definitions
    val achievements = listOf(
        AchievementItem("🛡️ Erster Einsatz", "Eingeloggt als physischer Agent", viewModel.playerAgentName.isNotBlank()),
        AchievementItem("🐍 Schlangenbeschwörer", "Erreiche 50+ Punkte in Snake", viewModel.snakeHighScore >= 50),
        AchievementItem("🧱 Stapel-Meister", "Erreiche 1000+ Punkte in Tetris", viewModel.tetrisHighScore >= 1000),
        AchievementItem("🐦 Flug-Akrobat", "Erreiche 20+ Punkte in Flappy Bird", viewModel.flappyBirdHighScore >= 20),
        AchievementItem("🦖 Urzeit-Läufer", "Erreiche 300+ Punkte in Dino Jump", viewModel.dinoHighScore >= 300),
        AchievementItem("🧠 Super-Gehirn", "Löse Memory Pairs in unter 25 Zügen", viewModel.memoryHighScore in 1..25),
        AchievementItem("❌ TTT-Bezwinger", "Erreiche 10+ Siege in TicTacToe", viewModel.ticTacToeWins >= 10),
        AchievementItem("🏎️ Drift-König", "Erreiche 200+ Punkte in Drift Car", viewModel.driftHighScore >= 200),
        AchievementItem("👥 Crowd-Kommandant", "Erreiche 300+ Punkte in Crowd Runner", viewModel.crowdHighScore >= 300),
        AchievementItem("🎵 Rhythmus-Gott", "Erreiche 3000+ Punkte in Beat Slam", viewModel.rhythmHighScore >= 3000),
        AchievementItem("🚀 Galaxiewächter", "Erreiche 1000+ Punkte in Space Shooter", viewModel.spaceHighScore >= 1000),
        AchievementItem("🔒 Sicherheits-Fanatiker", "Panik-Verriegelung dauerhaft aktiviert", viewModel.isPanicLockEnabled)
    )

    val unlockedCount = achievements.count { it.unlocked }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("agency_dashboard_card"),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🛰️ AGENCY AGENTEN-DASHBOARD",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (viewModel.playerAgentName.isNotBlank()) "AGENT: ${viewModel.playerAgentName.uppercase()}" else "PROFIL: ANONYM",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$totalPoints OP-PTS",
                        color = Color(0xFFFACC15),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(12.dp))

            // Rank Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Emblem/Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rank emblem",
                        tint = when (rank) {
                            "Rekrut" -> Color(0xFF94A3B8)
                            "Techniker" -> Color(0xFF38BDF8)
                            "Schatten-Agent" -> Color(0xFFA78BFA)
                            "Geheimdienst-Experte" -> Color(0xFFF43F5E)
                            else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SPIONAGE-RANG",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "$unlockedCount/12 TROPHÄEN",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        rank.uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                rankDesc,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress towards next rank
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "FORTSCHRITT: " + if (rank != "Doppelagent") "${(progress*100).toInt()}%" else "MAXIMALE RECHTE",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (rank != "Doppelagent") {
                        Text(
                            "NÄCHSTER RANG: $nextRankName ($nextRankThreshold PTS)",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle achievements button
            androidx.compose.material3.Button(
                onClick = { showAchievements = !showAchievements },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().testTag("toggle_achievements_button"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (showAchievements) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle achievements icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showAchievements) "TROPHÄENSCHRANK SCHLIESSEN" else "TROPHÄEN & LEISTUNGEN EINSEHEN",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Achievements details list
            if (showAchievements) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🏅 DEINE GEHEIMDIENST-LEISTUNGEN",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    achievements.forEach { ach ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        if (ach.unlocked) Color(0xFF065F46) else Color(0xFF1F2937),
                                        androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ach.unlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Unlocked",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ach.title,
                                    color = if (ach.unlocked) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    ach.description,
                                    color = if (ach.unlocked) Color(0xFF94A3B8) else Color(0xFF475569),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Quadruple data class
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

data class AchievementItem(
    val title: String,
    val description: String,
    val unlocked: Boolean
)
