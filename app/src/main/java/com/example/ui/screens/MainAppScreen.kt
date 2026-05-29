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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainAppScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    if (viewModel.isSecretUnlocked) {
        SecretArcadeDashboard(viewModel, modifier)
    } else {
        CalculatorScreen(viewModel, modifier)
    }
}

@Composable
fun CalculatorScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val calculations by viewModel.calculations.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (showUpdateDialog) {
        Dialog(onDismissRequest = { showUpdateDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.85f else 0.95f)
                    .fillMaxHeight(if (isLandscape) 0.9f else 0.75f)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showUpdateDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("update_center_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Update Center",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("history_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showHistory) Icons.Default.Close else Icons.Default.History,
                            contentDescription = "Historie",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Panel: Display & History
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // History Panel (shown vertically inside the left half)
                    AnimatedVisibility(
                        visible = showHistory,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                        modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Rechenverlauf",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Leeren", color = Color(0xFFEF9A9A))
                                    }
                                }

                                if (calculations.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Keine Einträge.",
                                            color = Color.White.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(calculations) { calc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.useHistoryItem(calc) }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(calc.expression, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                                    Text("= ${calc.result}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Take", tint = Color.White.copy(alpha = 0.5f))
                                            }
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Typed Math expression formula
                    Text(
                        text = viewModel.calculatorInput.ifEmpty { "0" },
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("calculator_input_display")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Computed Result
                    Text(
                        text = viewModel.calculatorOutput.ifEmpty { "" },
                        fontSize = 42.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Thin,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("calculator_result_display")
                    )
                }

                // Right Panel: Keypad Buttons with Glassmorphism
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buttons = listOf(
                        listOf("C", "⌫", " ", "/"),
                        listOf("7", "8", "9", "*"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("00", "0", ".", "=")
                    )
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { char ->
                                if (char.isEmpty() || char == " ") {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val isOperator = char in listOf("/", "*", "-", "+")
                                    val isClear = char in listOf("C", "⌫")
                                    val isEquals = char == "="

                                    val btnBg = when {
                                        isEquals -> Color(0xFF0A84FF).copy(alpha = 0.9f)
                                        isOperator -> Color.White.copy(alpha = 0.15f)
                                        isClear -> Color.White.copy(alpha = 0.1f)
                                        else -> Color.White.copy(alpha = 0.05f)
                                    }

                                    Button(
                                        onClick = {
                                            if (isEquals) viewModel.onCalculatorEvaluate()
                                            else viewModel.onCalculatorChar(char)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                            .testTag("calc_btn_$char"),
                                        colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = char,
                                            fontSize = 20.sp,
                                            fontWeight = if (isOperator || isEquals) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
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
                    .background(Color.Black)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Display & History Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (showHistory) 0.55f else 0.35f)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // Typed Math expression formula
                    Text(
                        text = viewModel.calculatorInput.ifEmpty { "0" },
                        fontSize = 28.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("calculator_input_display")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Computed Result
                    Text(
                        text = viewModel.calculatorOutput.ifEmpty { "" },
                        fontSize = 52.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Thin,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("calculator_result_display")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // History Panel
                    AnimatedVisibility(
                        visible = showHistory,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Rechenverlauf",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Leeren", color = Color(0xFFEF9A9A))
                                    }
                                }

                                if (calculations.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Keine Einträge.",
                                            color = Color.White.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(calculations) { calc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.useHistoryItem(calc) }
                                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(calc.expression, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                                                    Text("= ${calc.result}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Take", tint = Color.White.copy(alpha = 0.5f))
                                            }
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Keypad layout
                val buttons = listOf(
                    listOf("C", "⌫", " ", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("00", "0", ".", "=")
                )

                // Glassmorphism Keypad Background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                        .weight(if (showHistory) 0.45f else 0.65f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { char ->
                                if (char.isEmpty() || char == " ") {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val isOperator = char in listOf("/", "*", "-", "+")
                                    val isClear = char in listOf("C", "⌫")
                                    val isEquals = char == "="

                                    val btnBg = when {
                                        isEquals -> Color(0xFF0A84FF).copy(alpha = 0.9f) // Vibrant blue
                                        isOperator -> Color.White.copy(alpha = 0.15f)
                                        isClear -> Color.White.copy(alpha = 0.1f)
                                        else -> Color.White.copy(alpha = 0.05f)
                                    }

                                    val btnTextColor = Color.White

                                    Button(
                                        onClick = {
                                            if (isEquals) viewModel.onCalculatorEvaluate()
                                            else viewModel.onCalculatorChar(char)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                            .testTag("calc_btn_$char"),
                                        colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                                        shape = RoundedCornerShape(20.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = char,
                                            fontSize = 24.sp,
                                            fontWeight = if (isOperator || isEquals) FontWeight.Bold else FontWeight.Medium,
                                            color = btnTextColor
                                        )
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

// --- SECRET ARCADE DASHBOARD ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecretArcadeDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (viewModel.currentSecretSection == SecretSection.GAMES && viewModel.activeGame != GameType.HOME) {
            viewModel.activeGame = GameType.HOME
        } else if (viewModel.currentSecretSection == SecretSection.GALLERY) {
            viewModel.currentSecretSection = SecretSection.CHAT
        } else {
            viewModel.lockSecretMode()
        }
    }

    val pages = listOf(
        SecretSection.GAMES,
        SecretSection.CHAT,
        SecretSection.BROWSER,
        SecretSection.WATCH
    )
    
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
            if (viewModel.currentSecretSection != SecretSection.GAMES || viewModel.activeGame == GameType.HOME) {
                if (viewModel.currentSecretSection != SecretSection.GALLERY) {
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color(0xFF0F0F11),
                        contentColor = Color.White,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = Color.White
                                )
                            }
                        }
                    ) {
                        pages.forEachIndexed { index, section ->
                            val icon = when (section) {
                                SecretSection.GAMES -> Icons.Default.SportsEsports
                                SecretSection.CHAT -> Icons.Default.SmartToy
                                SecretSection.BROWSER -> Icons.Default.Public
                                SecretSection.WATCH -> Icons.Default.PlayCircle
                                else -> Icons.Default.Circle
                            }
                            val label = when (section) {
                                SecretSection.GAMES -> "Spiele"
                                SecretSection.CHAT -> "KI"
                                SecretSection.BROWSER -> "Web"
                                SecretSection.WATCH -> "Watch"
                                else -> ""
                            }
                            androidx.compose.material3.Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { viewModel.currentSecretSection = section },
                                icon = { Icon(icon, contentDescription = label) },
                                text = { Text(label, fontSize = 10.sp) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.White.copy(alpha = 0.4f)
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
                    containerColor = Color.White.copy(alpha = 0.15f),
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
        containerColor = Color.Black
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
                    userScrollEnabled = (viewModel.currentSecretSection != SecretSection.GAMES || viewModel.activeGame == GameType.HOME)
                ) { page ->
                    when (pages[page]) {
                        SecretSection.GAMES -> GamesTabScreen(viewModel)
                        SecretSection.CHAT -> ChatBotTabScreen(viewModel)
                        SecretSection.BROWSER -> BrowserTabScreen()
                        SecretSection.WATCH -> WatchTabScreen()
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}


// --- TAB SUB-SCREENS ---

@Composable
fun GamesTabScreen(viewModel: AppViewModel) {
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
                onSelect = { viewModel.activeGame = it }
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
                highScore = viewModel.memoryHighScore, // we can reuse memory or add new. Let's reuse memory for now to avoid adding new score, or just ignore highscore. Wait, wait, let's just pass memoryHighScore.
                onHighScoreUpdate = { viewModel.memoryHighScore = it },
                onBack = { viewModel.activeGame = GameType.HOME }
            )
            GameType.PONG -> com.example.ui.games.PongGame(
                onBack = { viewModel.activeGame = GameType.HOME }
            )
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
    val coins = 500 // Not fully passing down just for display if needed
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SPIELESAMMLUNG",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            GameCard(
                title = "SLOT MACHINE",
                description = "Drehe die Rollen und hoffe auf den Jackpot!",
                highScoreText = "Spielen",
                icon = Icons.Default.MonetizationOn,
                accentColor = Color(0xFF8B5CF6),
                onClick = { onSelect(GameType.SLOTS) }
            )
        }

        item {
            GameCard(
                title = "BLACKJACK",
                description = "Ziehe Karten und schlag den Dealer (21).",
                highScoreText = "Spielen",
                icon = Icons.Default.Style,
                accentColor = Color(0xFFEF4444),
                onClick = { onSelect(GameType.BLACKJACK) }
            )
        }

        item {
            GameCard(
                title = "MINES",
                description = "Sammle Edelsteine, aber pass auf die Minen auf!",
                highScoreText = "Spielen",
                icon = Icons.Default.Warning,
                accentColor = Color(0xFFF59E0B),
                onClick = { onSelect(GameType.MINES) }
            )
        }

        item {
            GameCard(
                title = "DINO JUMP",
                description = "Springe über Hindernisse wie im Browser ohne Internet!",
                highScoreText = "🏆 Highscore: $dinoHighScore",
                icon = Icons.Default.DirectionsRun,
                accentColor = Color(0xFF10B981),
                onClick = { onSelect(GameType.DINO) }
            )
        }

        item {
            GameCard(
                title = "2048",
                description = "Kombiniere Zahlen, um die 2048-Kachel zu erreichen!",
                highScoreText = "Spielen",
                icon = Icons.Default.GridOn,
                accentColor = Color(0xFFEAB308),
                onClick = { onSelect(GameType.TWO_THOUSAND_FORTY_EIGHT) }
            )
        }

        item {
            GameCard(
                title = "PONG",
                description = "Der Retro-Klassiker! Schlage den Ball am Gegner vorbei.",
                highScoreText = "Spielen",
                icon = Icons.Default.SportsTennis,
                accentColor = Color(0xFF3B82F6),
                onClick = { onSelect(GameType.PONG) }
            )
        }

        item {
            // TicTacToe Row Card
            GameCard(
                title = "TIC TAC TOE",
                description = "Klassisches Drei-in-einer-Reihe gegen dich selbst oder einen Freund.",
                highScoreText = "👑 Siege: $ticTacToeWins",
                icon = Icons.Default.Grid3x3,
                accentColor = Color(0xFF3B82F6),
                onClick = { onSelect(GameType.TICTACTOE) }
            )
        }
        
        item {
            // Memory Row Card
            GameCard(
                title = "MEMORY PAIRS",
                description = "Finde die passenden Paare! Trainiere dein Gedächtnis.",
                highScoreText = "🏆 Rekord: ${if (memoryHighScore > 0) memoryHighScore else "-"}",
                icon = Icons.Default.ViewModule,
                accentColor = Color(0xFFEC4899),
                onClick = { onSelect(GameType.MEMORY) }
            )
        }

        item {
            // Snake Row Card
            GameCard(
                title = "SNAKE CLASSIC",
                description = "Sammle Äpfel und wachse, ohne deine eigene Schwanzflosse oder die Ränder zu beißen!",
                highScoreText = "🏆 Rekord: $snakeHighScore",
                icon = Icons.Default.TrendingFlat,
                accentColor = Color(0xFF10B981),
                onClick = { onSelect(GameType.SNAKE) }
            )
        }

        item {
            // Tetris Card
            GameCard(
                title = "TETRIS BLOCKS",
                description = "Schiebe und rotiere herunterfallende Blöcke. Bilde vollständige Linien, um Punkte zu sahnen!",
                highScoreText = "🏆 Rekord: $tetrisHighScore",
                icon = Icons.Default.GridOn,
                accentColor = Color(0xFFA855F7),
                onClick = { onSelect(GameType.TETRIS) }
            )
        }

        item {
            // Flappy Bird Card
            GameCard(
                title = "FLAPPY BIRD",
                description = "Weiche den Röhren aus! Tippe den Screen an, um deine Flügel schlagen zu lassen.",
                highScoreText = "🏆 Rekord: $flappyBirdHighScore",
                icon = Icons.Default.Air,
                accentColor = Color(0xFFFACC15),
                onClick = { onSelect(GameType.FLAPPYBIRD) }
            )
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
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    highScoreText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Start game",
                tint = accentColor,
                modifier = Modifier.size(32.dp)
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
                    .background(Color(0xFF0F0F11))
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
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
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
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
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
                                color = if (viewModel.chatInputText.isNotBlank() && !viewModel.isBotResponding) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .testTag("chat_send_button")
                    ) {
                        if (viewModel.isBotResponding) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Senden",
                                tint = if (viewModel.chatInputText.isNotBlank() && !viewModel.isBotResponding) Color.White else Color.White.copy(alpha = 0.4f)
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
                            color = Color.White.copy(alpha = 0.6f),
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
    val bubbleColor = if (isUser) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)
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
            border = if (isUser) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
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
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.4f),
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
    val urls = listOf("https://www.tiktok.com", "https://www.youtube.com", "https://www.instagram.com")

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