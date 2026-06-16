package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Simple class representation for particles and skidmarks
data class DriftSmoke(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var age: Float,
    val maxAge: Float
)

data class TyreSkid(
    val pos: Offset,
    var alpha: Float = 1.0f
)

data class CarSkin(
    val id: String,
    val name: String,
    val bodyColor: Color,
    val accentColor: Color,
    val underglowColor: Color,
    val price: Int,
    val isUnlockByScore: Boolean = false,
    val requiredScore: Int = 0,
    val description: String = ""
)

val DriftCarSkins = listOf(
    CarSkin("classic", "Neon Shadow", Color(0xFF1E293B), Color(0xFF00FFCC), Color(0xFF00FFCC), 0, description = "Der klassische Agency-Einsatzwagen."),
    CarSkin("red_baron", "Rot-Baron", Color(0xFF991B1B), Color(0xFFFBBF24), Color(0xFFEF4444), 100, description = "Aggressives Rot mit goldenen Carbonlinien."),
    CarSkin("tokyo_midnight", "Tokyo Midnight", Color(0xFF2E1065), Color(0xFFF472B6), Color(0xFFFF00CC), 200, description = "Perfekt für illegale nächtliche Straßenrennen."),
    CarSkin("golden_phoenix", "Golden Phoenix", Color(0xFF0F172A), Color(0xFFFACC15), Color(0xFFEAB308), 350, description = "Reiner Luxus für Meisterspione."),
    CarSkin("toxic_venom", "Toxic Venom", Color(0xFF064E3B), Color(0xFFA3E635), Color(0xFF22C55E), 150, description = "Giftgrünes Schleier-Design."),
    CarSkin("arctic_blizzard", "Arctic Blizzard", Color(0xFFF8FAFC), Color(0xFF38BDF8), Color(0xFF0EA5E9), 150, description = "Eisweißes Winter-Tarnmuster."),
    CarSkin("reaper", "Phantom Reaper", Color(0xFF020617), Color(0xFF94A3B8), Color(0xFF64748B), 0, isUnlockByScore = true, requiredScore = 300, description = "Freigeschaltet ab einem Rekord von 300 Punkten!")
)

fun isSkinUnlocked(skin: CarSkin, hs: Int, unlockedSet: Set<String>): Boolean {
    if (skin.price == 0 && !skin.isUnlockByScore) return true
    if (skin.isUnlockByScore) return hs >= skin.requiredScore
    return unlockedSet.contains(skin.id)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriftCarGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_stats", Context.MODE_PRIVATE) }
    
    var selectedSkinId by remember { mutableStateOf(prefs.getString("selected_car_skin", "classic") ?: "classic") }
    var unlockedSkinsString by remember { mutableStateOf(prefs.getString("unlocked_car_skins", "classic") ?: "classic") }
    val unlockedSkins = remember(unlockedSkinsString) { unlockedSkinsString.split(",").toSet() }

    val currentSkin = remember(selectedSkinId) {
        DriftCarSkins.find { it.id == selectedSkinId } ?: DriftCarSkins[0]
    }

    fun selectSkin(skin: CarSkin) {
        selectedSkinId = skin.id
        prefs.edit().putString("selected_car_skin", skin.id).apply()
    }

    fun unlockSkin(skin: CarSkin) {
        if (coins >= skin.price) {
            onCoinsUpdate(coins - skin.price)
            val newUnlocked = (unlockedSkins + skin.id).joinToString(",")
            unlockedSkinsString = newUnlocked
            prefs.edit().putString("unlocked_car_skins", newUnlocked).apply()
            selectSkin(skin)
        }
    }

    var isRunning by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var driftCombo by remember { mutableStateOf(0f) }
    var highscore by remember(highScore) { mutableStateOf(highScore) }
    var isCarInitialized by remember { mutableStateOf(false) }
    var prevCw by remember { mutableStateOf(0f) }
    var coinsAddedForThisRun by remember { mutableStateOf(false) }

    // Physical Car variables (computed in pixels during canvas loop)
    var carX by remember { mutableStateOf(0f) } // Initialized in first loop
    var carY by remember { mutableStateOf(0f) }
    var carAngle by remember { mutableStateOf(0f) } // Radians (0 is pointing up)
    
    // Direct speed vectors
    var vx by remember { mutableStateOf(0f) }
    var vy by remember { mutableStateOf(0f) }
    
    // Input holds
    var steerLeftPressed by remember { mutableStateOf(false) }
    var steerRightPressed by remember { mutableStateOf(false) }

    // Distance/Time scrolled
    var distanceTraveled by remember { mutableStateOf(0f) }
    var speedMultiplier by remember { mutableStateOf(1.0f) }
    var isDrifting by remember { mutableStateOf(false) }

    // Particle / Skid collections
    val smokeParticles = remember { mutableStateListOf<DriftSmoke>() }
    val skidMarks = remember { mutableStateListOf<TyreSkid>() }

    // Helper to clear and reset game parameters
    fun resetGame(canvasW: Float, canvasH: Float) {
        carX = canvasW / 2f
        carY = canvasH * 0.72f
        carAngle = 0f
        vx = 0f
        vy = 0f
        score = 0
        driftCombo = 0f
        distanceTraveled = 0f
        speedMultiplier = 1.0f
        smokeParticles.clear()
        skidMarks.clear()
        coinsAddedForThisRun = false
    }

    // Curving road center generator based on world Y coordinate
    fun getRoadCenterX(worldY: Float, canvasWidth: Float): Float {
        val mid = canvasWidth / 2f
        val amp1 = canvasWidth * 0.22f
        val amp2 = canvasWidth * 0.08f
        
        // Start straight for travel distance up to 2400f, then transition smoothly over the next 2000f
        val straightLength = 2400f
        val transitionLength = 2000f
        val blend = when {
            worldY <= straightLength -> 0f
            worldY >= straightLength + transitionLength -> 1f
            else -> {
                val t = (worldY - straightLength) / transitionLength
                t * t * (3f - 2f * t) // Smoothstep
            }
        }
        
        val curveOffset = amp1 * sin(worldY * 0.0016f) + amp2 * cos(worldY * 0.0041f)
        return mid + curveOffset * blend
    }

    // Core Game Tick Thread
    LaunchedEffect(isRunning, gameOver) {
        if (isRunning && !gameOver) {
            while (true) {
                delay(16) // ~60 FPS

                // Increase game difficulty/speed slightly over time
                speedMultiplier = (1.0f + score * 0.0003f).coerceAtMost(2.0f)

                // 1. Steering Force Input Calculation
                val rotationSpeed = 0.048f
                if (steerLeftPressed) {
                    carAngle -= rotationSpeed
                }
                if (steerRightPressed) {
                    carAngle += rotationSpeed
                }

                // Keep angle in range [-PI, PI]
                if (carAngle < -Math.PI.toFloat()) carAngle += (2 * Math.PI.toFloat())
                if (carAngle > Math.PI.toFloat()) carAngle -= (2 * Math.PI.toFloat())

                // 2. Physics & Real Sideways Slipping Vectors (Drifting Feeling!)
                val forwardSpeed = 5.2f * speedMultiplier
                
                // Target velocities if wheels have full grip
                val targetVx = sin(carAngle) * forwardSpeed
                val targetVy = -cos(carAngle) * forwardSpeed

                // Interpolate velocities (creates drifting momentum/slippage!)
                // Forward movement is snappy; sideways drift has slip.
                val gripCoefficient = 0.12f // Lower values = more slide/slippage/drift!
                vx = vx * (1f - gripCoefficient) + targetVx * gripCoefficient
                vy = vy * (1f - gripCoefficient) + targetVy * gripCoefficient

                // Speed actually traveled (scroll speed/forward component is based on the general speed)
                distanceTraveled += forwardSpeed * 1.5f

                // Move car horizontal relative coordinate
                carX += vx

                // 3. Friction & Skid detection
                // Angle of actual movement vector
                val travelAngle = kotlin.math.atan2(vx.toDouble(), -vy.toDouble()).toFloat()
                var angleDiff = kotlin.math.abs(carAngle - travelAngle)
                if (angleDiff > Math.PI.toFloat()) {
                    angleDiff = (2 * Math.PI.toFloat()) - angleDiff
                }

                // If angle difference is high, the car is DRIFTING!
                isDrifting = angleDiff > 0.22f && (steerLeftPressed || steerRightPressed)

                if (isDrifting && score > 10) {
                    // Accumulate drift points
                    driftCombo += 0.25f
                    score += (1 + (driftCombo / 10).toInt())
                    
                    // Add tyres skid marks behind rear wheels
                    val wheelOffsetL = Offset(
                        carX - cos(carAngle) * 9f,
                        carY + sin(carAngle) * 9f
                    )
                    val wheelOffsetR = Offset(
                        carX + cos(carAngle) * 9f,
                        carY - sin(carAngle) * 9f
                    )
                    
                    skidMarks.add(TyreSkid(wheelOffsetL))
                    skidMarks.add(TyreSkid(wheelOffsetR))
                    if (skidMarks.size > 140) {
                        skidMarks.removeAt(0)
                    }

                    // Spawn backend smoke particle cloud
                    repeat(2) {
                        smokeParticles.add(
                            DriftSmoke(
                                x = carX - sin(carAngle) * 15f + Random.nextFloat() * 8f - 4f,
                                y = carY + cos(carAngle) * 15f + Random.nextFloat() * 8f - 4f,
                                vx = -vx * 0.3f + Random.nextFloat() * 1.5f - 0.75f,
                                vy = -vy * 0.3f + Random.nextFloat() * 0.5f,
                                color = Color.LightGray.copy(alpha = Random.nextFloat() * 0.4f + 0.2f),
                                size = Random.nextFloat() * 14f + 6f,
                                age = 0f,
                                maxAge = Random.nextFloat() * 25f + 15f
                            )
                        )
                    }
                } else {
                    // Decay combo multiplier if we stop drifting
                    driftCombo = (driftCombo - 0.5f).coerceAtLeast(0f)
                    score++
                }

                // Update existing smoke particles
                val smokerIter = smokeParticles.iterator()
                while (smokerIter.hasNext()) {
                    val p = smokerIter.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.age += 1f
                    if (p.age >= p.maxAge) {
                        smokerIter.remove()
                    }
                }

                // Decay existing skid marks transparency slightly
                skidMarks.forEach { it.alpha *= 0.985f }

                // 4. Track boundaries crash checks
                // world Y pos of the car
                val carWorldY = distanceTraveled
                // Get road center at car's vertical height
                // Note: carY remains stationary on screen; screen viewport scrolls down!
                val roadCenter = getRoadCenterX(carWorldY, 1080f) // Normalized baseline width 1080

                // Normalized horizontal bounds (scale based on current canvas dimension)
                // We'll calculate boundary collision precisely inside Canvas layout once sizes exist,
                // but let's do a reliable scaled check here:
                // If the car drifts too far off-center, it crashes!
                // (Done inside Canvas or via proportional calculation below)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Futuristic Slate dark
    ) {
        // App header
        TopAppBar(
            title = {
                Text(
                    "🏎️ HYPER DRIFT ARCADE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FFCC),
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF020617)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(clip = true)
            ) {
                val cw = size.width
                val ch = size.height

                // Auto initialize car horizontal center position if first frame, restart, or layout resize
                if (cw > 100f && (cw != prevCw || !isCarInitialized || carX == 0f)) {
                    carX = cw / 2f
                    carY = ch * 0.72f
                    prevCw = cw
                    isCarInitialized = true
                }

                if (!isCarInitialized || (!isRunning && !gameOver)) {
                    return@Canvas
                }

                // ---- Track layout variables ----
                val roadWidth = cw * 0.32f // Width of highway asphalt
                val curbWidth = cw * 0.02f // Red/White stripes curbs

                // 1. DRAW SCROLLING TRACK & ENVIRONMENT
                // We split the screen height into segments to draw the curving road segments
                val segments = 32
                val segHeight = ch / segments

                // Drawing grass/sand landscape baseline
                drawRect(color = Color(0xFF064E3B)) // Deep dark green grass

                // Assemble the road path ribbon coordinates
                val roadLeftPoints = ArrayList<Offset>()
                val roadRightPoints = ArrayList<Offset>()

                for (i in 0..segments) {
                    val segY = i * segHeight
                    // Corresponding world Y for this vertical pixel height
                    val segWorldY = distanceTraveled + (ch - segY)
                    val centerRoadX = getRoadCenterX(segWorldY, cw)

                    roadLeftPoints.add(Offset(centerRoadX - roadWidth / 2, segY))
                    roadRightPoints.add(Offset(centerRoadX + roadWidth / 2, segY))
                }

                // Render Curbs & Road Bed
                val roadPath = Path()
                roadPath.moveTo(roadLeftPoints[0].x, roadLeftPoints[0].y)
                for (i in 1 until roadLeftPoints.size) {
                    roadPath.lineTo(roadLeftPoints[i].x, roadLeftPoints[i].y)
                }
                for (i in roadRightPoints.indices.reversed()) {
                    roadPath.lineTo(roadRightPoints[i].x, roadRightPoints[i].y)
                }
                roadPath.close()

                // Asphalt gray highway fill
                drawPath(path = roadPath, color = Color(0xFF1E293B))

                // Left red-and-white curb blocks
                for (i in 0 until roadLeftPoints.size - 1) {
                    val p1 = roadLeftPoints[i]
                    val p2 = roadLeftPoints[i + 1]
                    val isWhite = ((distanceTraveled + p1.y).toInt() / 45) % 2 == 0
                    val curbColor = if (isWhite) Color.White else Color(0xFFEF4444)
                    
                    drawLine(
                        color = curbColor,
                        start = p1,
                        end = p2,
                        strokeWidth = curbWidth
                    )
                }

                // Right red-and-white curb blocks
                for (i in 0 until roadRightPoints.size - 1) {
                    val p1 = roadRightPoints[i]
                    val p2 = roadRightPoints[i + 1]
                    val isWhite = ((distanceTraveled + p1.y).toInt() / 45) % 2 == 0
                    val curbColor = if (isWhite) Color.White else Color(0xFFEF4444)
                    
                    drawLine(
                        color = curbColor,
                        start = p1,
                        end = p2,
                        strokeWidth = curbWidth
                    )
                }

                // Center dashed dividing line
                for (i in 0 until segments - 1 step 2) {
                    val p1Y = i * segHeight
                    val p2Y = (i + 1) * segHeight

                    val wY1 = distanceTraveled + (ch - p1Y)
                    val wY2 = distanceTraveled + (ch - p2Y)

                    val cx1 = getRoadCenterX(wY1, cw)
                    val cx2 = getRoadCenterX(wY2, cw)

                    drawLine(
                        color = Color(0xFFFBBF24).copy(alpha = 0.8f),
                        start = Offset(cx1, p1Y),
                        end = Offset(cx2, p2Y),
                        strokeWidth = 4f
                    )
                }

                // 2. COLLISION & BOUNDARIES EVALUATION (Calculated from actual canvas cw width)
                val currentCarCenterRoad = getRoadCenterX(distanceTraveled + (ch - carY), cw)
                val distanceFromRoadCenter = kotlin.math.abs(carX - currentCarCenterRoad)

                // Off-road Grass slowdown
                val isOffRoad = distanceFromRoadCenter > (roadWidth / 2 - 12f)
                if (isOffRoad && isRunning && !gameOver && isCarInitialized) {
                    // Screen shake visual effect!
                    // Decrease velocity
                    vx *= 0.85f
                    vy *= 0.85f
                    
                    // Spawn grass dirt particles
                    if (Random.nextInt(100) < 30) {
                        smokeParticles.add(
                            DriftSmoke(
                                x = carX + Random.nextFloat() * 12f - 6f,
                                y = carY + 10f,
                                vx = Random.nextFloat() * 3f - 1.5f,
                                vy = Random.nextFloat() * 2f + 1f,
                                color = Color(0xFF10B981).copy(alpha = 0.6f), // grass green particles
                                size = Random.nextFloat() * 8f + 4f,
                                age = 0f,
                                maxAge = 15f
                            )
                        )
                    }

                    // Fatal boundary Crash (hits concrete fence borders)
                    // Added a 600f traveled safety buffer so early frames/starts are completely safe
                    if (distanceTraveled > 600f && distanceFromRoadCenter > (roadWidth / 2 + 75f)) {
                        gameOver = true
                        if (score > highscore) {
                            highscore = score
                            onHighScoreUpdate(score)
                        }
                        if (!coinsAddedForThisRun) {
                            val earned = if (score > 10) (score / 40).coerceAtMost(30) else 0
                            if (earned > 0) {
                                onCoinsUpdate(coins + earned)
                            }
                            coinsAddedForThisRun = true
                        }
                    }
                }

                // 3. DRAW TYRE SKIDMARKS
                skidMarks.forEach { skid ->
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f * skid.alpha),
                        radius = 4f,
                        center = skid.pos
                    )
                }

                // 4. DRAW EXHAUST/DRIFT SMOKE
                smokeParticles.forEach { p ->
                    val alpha = (1f - (p.age / p.maxAge)).coerceIn(0f, 1f)
                    drawCircle(
                        color = p.color.copy(alpha = p.color.alpha * alpha),
                        radius = p.size * (1f + p.age * 0.05f),
                        center = Offset(p.x, p.y)
                    )
                }

                // 5. DRAW THE CYBERPUNK DRIFT CAR
                drawContext.canvas.save()
                drawContext.canvas.translate(carX, carY)
                drawContext.canvas.rotate(Math.toDegrees(carAngle.toDouble()).toFloat())

                // Dimensions of cyber sports car
                val carW = cw * 0.048f
                val carH = carW * 2.1f

                // Draw tires
                val tireW = carW * 0.28f
                val tireH = carH * 0.22f
                // Back left & right wheels
                drawRect(Color.Black, topLeft = Offset(-carW/2 - tireW/3, carH/2 - tireH * 1.2f), size = Size(tireW, tireH))
                drawRect(Color.Black, topLeft = Offset(carW/2 - tireW * 0.7f, carH/2 - tireH * 1.2f), size = Size(tireW, tireH))
                // Front left & right wheels (slightly turned with steering input)
                val steeringTurnAngle = if (steerLeftPressed) -24f else if (steerRightPressed) 24f else 0f
                drawContext.canvas.save()
                drawContext.canvas.translate(-carW/2, -carH/2 + tireH)
                drawContext.canvas.rotate(steeringTurnAngle)
                drawRect(Color.Black, topLeft = Offset(-tireW/3, -tireH/2), size = Size(tireW, tireH))
                drawContext.canvas.restore()

                drawContext.canvas.save()
                drawContext.canvas.translate(carW/2 - tireW * 0.7f, -carH/2 + tireH)
                drawContext.canvas.rotate(steeringTurnAngle)
                drawRect(Color.Black, topLeft = Offset(0f, -tireH/2), size = Size(tireW, tireH))
                drawContext.canvas.restore()

                // Neon underglow aura
                val underglowColor = if (isDrifting) Color(0xFFFF00CC) else currentSkin.underglowColor
                drawRoundRect(
                    color = underglowColor.copy(alpha = 0.35f + sin((distanceTraveled * 0.1f)).coerceIn(-0.2f, 0.2f)),
                    topLeft = Offset(-carW * 0.7f, -carH * 0.6f),
                    size = Size(carW * 1.4f, carH * 1.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f),
                    style = Stroke(width = 8f)
                )

                // Sports car body frame hull
                // Hood area
                drawRoundRect(
                    color = currentSkin.bodyColor,
                    topLeft = Offset(-carW/2, -carH/2),
                    size = Size(carW, carH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                // Supercar aerodynamic lines
                drawRoundRect(
                    color = currentSkin.accentColor,
                    topLeft = Offset(-carW/2, -carH/2),
                    size = Size(carW, carH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = Stroke(width = 3f)
                )

                // Windshield cockpit glass (cyan gradient/neon)
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(-carW * 0.38f, -carH * 0.15f),
                    size = Size(carW * 0.76f, carH * 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = currentSkin.accentColor,
                    topLeft = Offset(-carW * 0.38f, -carH * 0.15f),
                    size = Size(carW * 0.76f, carH * 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = Stroke(width = 1.5f)
                )

                // Headlights glowing LEDs (white/amber forward facing vector)
                drawCircle(color = Color.White, radius = 4f, center = Offset(-carW * 0.32f, -carH/2 + 2f))
                drawCircle(color = Color.White, radius = 4f, center = Offset(carW * 0.32f, -carH/2 + 2f))

                // High Rear Spoiler Wing
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(-carW * 0.6f, carH/2 - 5f),
                    size = Size(carW * 1.2f, 6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
                drawLine(
                    color = currentSkin.accentColor,
                    start = Offset(-carW * 0.6f, carH/2 - 2f),
                    end = Offset(carW * 0.6f, carH/2 - 2f),
                    strokeWidth = 3f
                )

                drawContext.canvas.restore()
            }

            // HUD Overlays
            if (!isRunning && !gameOver) {
                // Main Entrance Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 12.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🏎️ HYPER DRIFT ARCADE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Drifte auf einer unendlichen kurvigen Autobahn! Halte die Tasten LINKS und RECHTS gedrückt, um dein Heck ausbrechen zu lassen. Berühre die Grasgrenzen nicht, sonst crashst du!",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                    if (highscore > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "BEST SCORE: $highscore",
                            color = Color(0xFFFF00CC),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Budget Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DEIN GUTHABEN: ",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "$coins 💰",
                            color = Color(0xFFFACC15),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "🏎️ DRIFT-GARAGE / SKINS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DriftCarSkins.forEach { skin ->
                            val isUnlocked = isSkinUnlocked(skin, highscore, unlockedSkins)
                            val isSelected = skin.id == selectedSkinId
                            
                            Box(
                                modifier = Modifier
                                    .width(135.dp)
                                    .background(
                                        if (isSelected) Color(0xFF1E293B) else Color(0xFF0A0F1D),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00FFCC) else Color(0xFF334155),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isUnlocked) {
                                            selectSkin(skin)
                                        } else if (skin.price > 0 && coins >= skin.price) {
                                            unlockSkin(skin)
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Mini Car Color preview block
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Underglow color indicator
                                        Box(modifier = Modifier.size(8.dp).background(skin.underglowColor, CircleShape).border(1.dp, Color.White, CircleShape))
                                        // Body color indicator
                                        Box(modifier = Modifier.size(12.dp).background(skin.bodyColor, RoundedCornerShape(2.dp)).border(1.dp, Color.White, RoundedCornerShape(2.dp)))
                                        // Accent color indicator
                                        Box(modifier = Modifier.size(8.dp).background(skin.accentColor, CircleShape).border(1.dp, Color.White, CircleShape))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        skin.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        skin.description,
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.height(22.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Purchase/Status Badge
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF00FFCC)
                                                    isUnlocked -> Color(0xFF1E293B)
                                                    else -> Color(0xFF0F172A)
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isSelected -> {
                                                Text("AKTIV 🏎️", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            isUnlocked -> {
                                                Text("WÄHLEN", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            skin.isUnlockByScore -> {
                                                Text("🏆 HS: ${skin.requiredScore}", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            else -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${skin.price}", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("💰", fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isRunning = true
                            gameOver = false
                            // Reset state
                            score = 0
                            driftCombo = 0f
                            steerLeftPressed = false
                            steerRightPressed = false
                            coinsAddedForThisRun = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GAS GEBEN! 🚨", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                // HUD scoring overlay
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Score on Top-Left
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        Text("SCORE", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "$score", 
                            color = Color.White, 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Black, 
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Drift indicator in Center Top
                    if (driftCombo > 10f) {
                        val multiplier = 1 + (driftCombo / 20).toInt()
                        Column(
                            modifier = Modifier.align(Alignment.TopCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "DRIFTING!",
                                color = Color(0xFFFF00CC),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = 1f + sin(distanceTraveled * 0.2f) * 0.08f
                                    scaleY = 1f + sin(distanceTraveled * 0.2f) * 0.08f
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF00CC).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFF00CC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "MULTIPLIKATOR: x$multiplier",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Speed indicator Top-Right
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("SPEED", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${(110 * speedMultiplier).toInt()} km/h", 
                            color = Color(0xFF00FFCC), 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bottom Steer inputs - Tactile hold buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // STEER LEFT button
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    color = if (steerLeftPressed) Color(0xFF00FFCC).copy(alpha = 0.72f) else Color(0xFF00FFCC).copy(alpha = 0.22f),
                                    shape = CircleShape
                                )
                                .border(3.dp, Color(0xFF00FFCC), CircleShape)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val isPressed = event.changes.any { it.pressed }
                                            steerLeftPressed = isPressed
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "◀ LINKS", 
                                color = if (steerLeftPressed) Color.Black else Color.White, 
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // STEER RIGHT button
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    color = if (steerRightPressed) Color(0xFFFF00CC).copy(alpha = 0.72f) else Color(0xFFFF00CC).copy(alpha = 0.22f),
                                    shape = CircleShape
                                )
                                .border(3.dp, Color(0xFFFF00CC), CircleShape)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val isPressed = event.changes.any { it.pressed }
                                            steerRightPressed = isPressed
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "RECHTS ▶", 
                                color = if (steerRightPressed) Color.Black else Color.White, 
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Game over popup dialog
            if (gameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFF0B132B), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "💥 CRASHED! 💥",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            "Du hast die Leitplanke gerammt!",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )

                        val coinsEarned = if (score > 10) (score / 40).coerceAtMost(30) else 0
                        if (coinsEarned > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "💰 COIN-BELOHNUNG: +$coinsEarned COINS!",
                                color = Color(0xFFFACC15),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "NEUES GUTHABEN: $coins 💰",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DEIN SCORE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    "$score",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BESTE DRIFT-PUNKTE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    "$highscore",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // Trigger canvas re-centering values on restart
                                isCarInitialized = false
                                carX = 0f 
                                isRunning = true
                                gameOver = false
                                score = 0
                                driftCombo = 0f
                                steerLeftPressed = false
                                steerRightPressed = false
                                distanceTraveled = 0f
                                coinsAddedForThisRun = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Neustart")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RETRY", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                isRunning = false
                                gameOver = false
                            }
                        ) {
                            Text("LOBBY", color = Color.LightGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
