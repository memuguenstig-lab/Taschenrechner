package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Projectile & entity structs for Crowd Runner
data class CrowdMathGate(
    val x: Float,
    var y: Float,
    val operation: String,
    val value: Int,
    val isBad: Boolean
)

data class CrowdRunnerProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var active: Boolean = true
)

data class BossPlasmaBullet(
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    var active: Boolean = true
)

data class CrowdSpark(
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val color: Color,
    var age: Float,
    val maxAge: Float
)

enum class CrowdGameState {
    LOBBY,
    RUNNING,
    BOSS_WARNING,
    BOSS_FIGHT,
    DEFEAT,
    VICTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdRunnerGame(onBack: () -> Unit) {
    var gameState by remember { mutableStateOf(CrowdGameState.LOBBY) }
    var score by remember { mutableStateOf(0) }
    var crowdCount by remember { mutableStateOf(10) } // starts with 10 runners
    var playerX by remember { mutableStateOf(50f) } // horizontal position (0..100)
    var choiceCount by remember { mutableStateOf(0) }
    val maxChoices = 15

    // Boss State
    var bossHP by remember { mutableStateOf(500) }
    val bossMaxHP = 500
    var bossX by remember { mutableStateOf(50f) }
    var bossY by remember { mutableStateOf(-20f) } // Descends in fight
    var bossDirection by remember { mutableStateOf(1f) }

    // List of active entities
    var gateLeft by remember { mutableStateOf<CrowdMathGate?>(null) }
    var gateRight by remember { mutableStateOf<CrowdMathGate?>(null) }
    
    val runnerProjectiles = remember { mutableStateListOf<CrowdRunnerProjectile>() }
    val bossBullets = remember { mutableStateListOf<BossPlasmaBullet>() }
    val sparks = remember { mutableStateListOf<CrowdSpark>() }

    // Track frame ticks for intervals
    var frameCount by remember { mutableStateOf(0) }

    // Generates a risk/reward pair where one is frequently bad, requiring skilled decision and steering
    fun generateGates() {
        if (choiceCount >= maxChoices) {
            gateLeft = null
            gateRight = null
            return
        }

        // 65% chance of spawning a warning (bad) gate alongside a good one
        val spawnBadGate = Random.nextFloat() < 0.65f

        if (spawnBadGate) {
            val isLeftBad = Random.nextBoolean()

            // Left Gate parameters
            val opLeft: String
            val valLeft: Int
            val isLeftBadFlag: Boolean
            if (isLeftBad) {
                opLeft = if (Random.nextBoolean()) "-" else "÷"
                valLeft = if (opLeft == "÷") Random.nextInt(2, 4) else Random.nextInt(10, 45)
                isLeftBadFlag = true
            } else {
                opLeft = if (Random.nextBoolean()) "+" else "x"
                valLeft = if (opLeft == "x") Random.nextInt(2, 5) else Random.nextInt(15, 60)
                isLeftBadFlag = false
            }

            // Right Gate parameters
            val opRight: String
            val valRight: Int
            val isRightBadFlag: Boolean
            if (!isLeftBad) {
                opRight = if (Random.nextBoolean()) "-" else "÷"
                valRight = if (opRight == "÷") Random.nextInt(2, 4) else Random.nextInt(10, 45)
                isRightBadFlag = true
            } else {
                opRight = if (Random.nextBoolean()) "+" else "x"
                valRight = if (opRight == "x") Random.nextInt(2, 5) else Random.nextInt(15, 60)
                isRightBadFlag = false
            }

            gateLeft = CrowdMathGate(25f, -15f, opLeft, valLeft, isLeftBadFlag)
            gateRight = CrowdMathGate(75f, -15f, opRight, valRight, isRightBadFlag)
        } else {
            // Both are positive (pure boost)
            val leftOp = if (Random.nextBoolean()) "+" else "x"
            val rightOp = if (Random.nextBoolean()) "+" else "x"

            val leftVal = if (leftOp == "x") Random.nextInt(2, 4) else Random.nextInt(10, 40)
            val rightVal = if (rightOp == "x") Random.nextInt(2, 4) else Random.nextInt(10, 40)

            gateLeft = CrowdMathGate(25f, -15f, leftOp, leftVal, isBad = false)
            gateRight = CrowdMathGate(75f, -15f, rightOp, rightVal, isBad = false)
        }
    }

    // Exploder helper
    fun spawnSparks(x: Float, y: Float, color: Color, count: Int = 10) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 4f + 1.5f
            sparks.add(
                CrowdSpark(
                    x = x,
                    y = y,
                    dx = cos(angle) * speed,
                    dy = sin(angle) * speed,
                    color = color,
                    age = 0f,
                    maxAge = Random.nextFloat() * 20f + 10f
                )
            )
        }
    }

    // Core Game Tick Mechanics
    LaunchedEffect(gameState) {
        if (gameState == CrowdGameState.RUNNING) {
            // Reset gameplay states starting running phase
            choiceCount = 0
            bossHP = bossMaxHP
            bossY = -20f
            runnerProjectiles.clear()
            bossBullets.clear()
            sparks.clear()
            generateGates()

            while (gameState == CrowdGameState.RUNNING) {
                delay(16)
                frameCount++

                // Update falling gates
                if (gateLeft != null && gateRight != null) {
                    gateLeft!!.y += 1.25f
                    gateRight!!.y += 1.25f

                    // Collision check with player's gates zone (vertical coordinates ~76f..80f)
                    if (gateLeft!!.y >= 78f && gateLeft!!.y <= 81f) {
                        val selectedGate = if (playerX < 50f) gateLeft else gateRight
                        if (selectedGate != null) {
                            val prevCount = crowdCount
                            when (selectedGate.operation) {
                                "+" -> crowdCount += selectedGate.value
                                "x" -> crowdCount *= selectedGate.value
                                "-" -> crowdCount = (crowdCount - selectedGate.value).coerceAtLeast(1)
                                "÷" -> crowdCount = (crowdCount / selectedGate.value).coerceAtLeast(1)
                            }
                            // Spawn sparks indicating crowd modification
                            val col = if (selectedGate.isBad) Color(0xFFEF4444) else Color(0xFF00FFCC)
                            spawnSparks(playerX, 78f, col, 15)
                            score += (crowdCount - prevCount).coerceAtLeast(0) * 5
                        }
                        
                        choiceCount++
                        if (choiceCount < maxChoices) {
                            generateGates()
                        } else {
                            gateLeft = null
                            gateRight = null
                        }
                    }
                }

                // If running gates are done, trigger transition warning to Boss
                if (choiceCount >= maxChoices && gateLeft == null) {
                    gameState = CrowdGameState.BOSS_WARNING
                }
            }
        } else if (gameState == CrowdGameState.BOSS_WARNING) {
            // Flash a warning alert and slide the boss down
            bossY = -20f
            runnerProjectiles.clear()
            bossBullets.clear()
            
            var ticks = 0
            while (gameState == CrowdGameState.BOSS_WARNING) {
                delay(16)
                ticks++
                
                // Animate entry of Cyber Boss down from ceiling
                if (bossY < 24f) {
                    bossY += 0.4f
                }
                
                if (ticks > 120) { // ~2 seconds of alarms
                    gameState = CrowdGameState.BOSS_FIGHT
                }
            }
        } else if (gameState == CrowdGameState.BOSS_FIGHT) {
            // Real, active battle sequence!
            while (gameState == CrowdGameState.BOSS_FIGHT) {
                delay(16)
                frameCount++

                // 1. Oscillate Boss side-to-side
                bossX += bossDirection * 0.9f
                if (bossX > 80f || bossX < 20f) {
                    bossDirection *= -1f
                }

                // 2. Spawn runner homing projectiles (launching forces towards boss!)
                // Attack speed scales based on physical crowd size
                val spawnInterval = when {
                    crowdCount > 200 -> 3
                    crowdCount > 80 -> 6
                    crowdCount > 20 -> 12
                    else -> 18
                }
                
                if (frameCount % spawnInterval == 0 && crowdCount > 0) {
                    // One runner launches from player crowd at (playerX, 78f) up toward (bossX, bossY)
                    val dx = bossX - playerX
                    val dy = bossY - 78f
                    val distance = kotlin.math.hypot(dx, dy)
                    val speed = 4.5f
                    val vx = (dx / distance) * speed
                    val vy = (dy / distance) * speed
                    
                    runnerProjectiles.add(
                        CrowdRunnerProjectile(
                            x = playerX + Random.nextFloat() * 12f - 6f,
                            y = 78f + Random.nextFloat() * 10f - 5f,
                            vx = vx,
                            vy = vy
                        )
                    )
                    
                    // Consume 1 from the remaining runners pool as they charge
                    crowdCount--
                }

                // 3. Update Runner Projectiles
                val rIter = runnerProjectiles.iterator()
                while (rIter.hasNext()) {
                    val r = rIter.next()
                    r.x += r.vx
                    r.y += r.vy

                    // Collision with boss (boss centered around bossX, bossY)
                    val distToBoss = kotlin.math.hypot(r.x - bossX, r.y - bossY)
                    if (distToBoss < 9f) {
                        // Impact!
                        rIter.remove()
                        bossHP = (bossHP - 12).coerceAtLeast(0)
                        spawnSparks(bossX, bossY, Color(0xFFFBBF24), 6) // Golden sparks on armor hit
                        score += 25
                        if (bossHP <= 0) {
                            spawnSparks(bossX, bossY, Color.Red, 45) // Massive final explosion
                            gameState = CrowdGameState.VICTORY
                        }
                    } else if (r.y < 0f || r.x < 0f || r.x > 100f) {
                        rIter.remove()
                    }
                }

                // 4. Boss Mech Attack Sequence! (Spitting plasma bursts)
                val fireInterval = (35 - (score / 1500)).coerceAtLeast(15)
                if (frameCount % fireInterval == 0) {
                    // Fire 3-way vertical bullets down from boss center
                    for (angleDeg in arrayOf(75, 90, 105)) {
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val bulletVel = 1.6f
                        val bvx = cos(rad).toFloat() * bulletVel
                        val bvy = sin(rad).toFloat() * bulletVel
                        bossBullets.add(
                            BossPlasmaBullet(
                                x = bossX,
                                y = bossY + 4f,
                                dx = bvx,
                                dy = bvy
                            )
                        )
                    }
                }

                // 5. Update Boss Plasma Bullets
                val bIter = bossBullets.iterator()
                while (bIter.hasNext()) {
                    val b = bIter.next()
                    b.x += b.dx
                    b.y += b.dy

                    // Collision with player crowd zone (horizontal bounds around playerX)
                    val distToPlayer = kotlin.math.hypot(b.x - playerX, b.y - 78f)
                    if (b.y >= 75f && b.y <= 81f && distToPlayer < 9f) {
                        // Impact players!
                        bIter.remove()
                        // Decimate 12% of crowd or flat 15 runners
                        val penalty = (crowdCount * 0.12f).toInt().coerceAtLeast(15)
                        crowdCount = (crowdCount - penalty).coerceAtLeast(0)
                        spawnSparks(playerX, 78f, Color(0xFFEF4444), 16) // Blood red impact explosion
                        if (crowdCount <= 0) {
                            gameState = CrowdGameState.DEFEAT
                        }
                    } else if (b.y > 100f || b.x < 0f || b.x > 100f) {
                        bIter.remove()
                    }
                }

                // Lose condition: If crowd drops to zero and no projectiles are left to deal final damage
                if (crowdCount <= 0 && runnerProjectiles.isEmpty() && bossHP > 0) {
                    gameState = CrowdGameState.DEFEAT
                }
            }
        }
    }

    // Animate Spark Particles separately to run even after fight completes
    LaunchedEffect(true) {
        while (true) {
            delay(16)
            if (!sparks.isEmpty()) {
                val sIter = sparks.iterator()
                while (sIter.hasNext()) {
                    val s = sIter.next()
                    s.x += s.dx * 0.95f
                    s.y += s.dy * 0.95f
                    s.age += 1f
                    if (s.age >= s.maxAge) {
                        sIter.remove()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Futuristic Deep Slate
    ) {
        TopAppBar(
            title = {
                Text(
                    "🏃 CROWD RUNNER EVO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FFCC)
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
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (gameState == CrowdGameState.LOBBY) {
                // Lobby overview
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "👾 CROWD RUNNER EVO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sammle Läufer durch Rechen-Gates! Aber Achtung: In dieser Evolution gibt es fiese rote Minus- und Divisionstüren, die deine Gruppe dezimieren! Drifte geschickt vorbei, um die größte Armee für den Boss Mech aufzubauen!",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            crowdCount = 10
                            playerX = 50f
                            gameState = CrowdGameState.RUNNING
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RUN & FIGHT 🏃💥", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                // Active Game Render Canvas
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)) // Cool dark cyber track
                    ) {
                        val w = size.width
                        val h = size.height

                        // Draw procedural background grid/lines
                        val gridLineCount = 12
                        val gridSegment = h / gridLineCount
                        val trackOffset = (frameCount * 2) % gridSegment.toInt()
                        for (i in -1..gridLineCount) {
                            val lineY = i * gridSegment + trackOffset
                            drawLine(
                                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                start = Offset(w * 0.15f, lineY),
                                end = Offset(w * 0.85f, lineY),
                                strokeWidth = 1.5f
                            )
                        }

                        // Drawing central asphalt running track ribbon
                        drawRect(
                            color = Color(0xFF020617), // pitch black speed track
                            topLeft = Offset(w * 0.15f, 0f),
                            size = Size(w * 0.70f, h)
                        )
                        // Neon border rails
                        drawLine(Color(0xFF00FFCC), start = Offset(w * 0.15f, 0f), end = Offset(w * 0.15f, h), strokeWidth = 4f)
                        drawLine(Color(0xFF00FFCC), start = Offset(w * 0.85f, 0f), end = Offset(w * 0.85f, h), strokeWidth = 4f)

                        // 1. DRAW SPARKS EXPLOSIONS
                        sparks.forEach { s ->
                            val force = (1f - (s.age / s.maxAge)).coerceIn(0f, 1f)
                            drawCircle(
                                color = s.color.copy(alpha = force),
                                radius = Random.nextFloat() * 4f + 2f,
                                center = Offset((s.x / 100f) * w, (s.y / 100f) * h)
                            )
                        }

                        // 2. DRAW MATH GATES (if running phase)
                        if (gameState == CrowdGameState.RUNNING) {
                            gateLeft?.let { g ->
                                val gateColor = if (g.isBad) Color(0xFFEF4444) else Color(0xFF00FFCC)
                                drawRect(
                                    color = gateColor.copy(alpha = 0.25f),
                                    topLeft = Offset(w * 0.15f, (g.y / 100f) * h),
                                    size = Size(w * 0.35f, 35f)
                                )
                                drawLine(
                                    color = gateColor,
                                    start = Offset(w * 0.15f, (g.y / 100f) * h),
                                    end = Offset(w * 0.50f, (g.y / 100f) * h),
                                    strokeWidth = 6f
                                )
                            }
                            gateRight?.let { g ->
                                val gateColor = if (g.isBad) Color(0xFFEF4444) else Color(0xFF00FFCC)
                                drawRect(
                                    color = gateColor.copy(alpha = 0.25f),
                                    topLeft = Offset(w * 0.50f, (g.y / 100f) * h),
                                    size = Size(w * 0.35f, 35f)
                                )
                                drawLine(
                                    color = gateColor,
                                    start = Offset(w * 0.50f, (g.y / 100f) * h),
                                    end = Offset(w * 0.85f, (g.y / 100f) * h),
                                    strokeWidth = 6f
                                )
                            }
                        }

                        // 3. DRAW HOMING RUNNER ATTACK PROJECTILES
                        runnerProjectiles.forEach { r ->
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 6f,
                                center = Offset((r.x / 100f) * w, (r.y / 100f) * h)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = Offset((r.x / 100f) * w, (r.y / 100f) * h)
                            )
                        }

                        // 4. DRAW BOSS PLASMA PROJECTILES
                        bossBullets.forEach { b ->
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.32f),
                                radius = 15f,
                                center = Offset((b.x / 100f) * w, (b.y / 100f) * h)
                            )
                            drawCircle(
                                color = Color(0xFFFF00CC),
                                radius = 8f,
                                center = Offset((b.x / 100f) * w, (b.y / 100f) * h)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = Offset((b.x / 100f) * w, (b.y / 100f) * h)
                            )
                        }

                        // 5. DRAW THE GIANT CYBER BOSS MECH (descended down in battle state)
                        if (gameState == CrowdGameState.BOSS_WARNING || gameState == CrowdGameState.BOSS_FIGHT) {
                            val bx = (bossX / 100f) * w
                            val by = (bossY / 100f) * h

                            // Boss Core body
                            drawCircle(
                                color = Color(0xFF7F1D1D),
                                radius = 55f,
                                center = Offset(bx, by)
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 55f,
                                center = Offset(bx, by),
                                style = Stroke(width = 4f)
                            )

                            // Left and right mechanical floating booster arms
                            drawRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(bx - 90f, by - 20f),
                                size = Size(35f, 50f)
                            )
                            drawRect(
                                color = Color(0xFFEF4444),
                                topLeft = Offset(bx - 90f, by - 20f),
                                size = Size(35f, 50f),
                                style = Stroke(width = 2f)
                            )

                            drawRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(bx + 55f, by - 20f),
                                size = Size(35f, 50f)
                            )
                            drawRect(
                                color = Color(0xFFEF4444),
                                topLeft = Offset(bx + 55f, by - 20f),
                                size = Size(35f, 50f),
                                style = Stroke(width = 2f)
                            )

                            // Glowing central laser eye
                            drawCircle(
                                color = Color(0xFFFF0066),
                                radius = 10f + sin(frameCount * 0.2f) * 3f,
                                center = Offset(bx, by + 10f)
                            )
                        }

                        // 6. DRAW THE CROWD SWARMING PARTICLE GROUP (follows playerX organically)
                        // Player position baseline height
                        val px = (playerX / 100f) * w
                        val py = h * 0.78f

                        // Draw a glowing base under the active crowd center
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.2f),
                            radius = (crowdCount.toFloat() * 1.2f).coerceIn(24f, 150f),
                            center = Offset(px, py)
                        )

                        // Calculate how many individual swarm dots to render
                        val maxDotsToRender = minOf(crowdCount, 40)
                        for (dot in 0 until maxDotsToRender) {
                            // Organic swarm spacing using deterministic trigonometric noise offsets
                            val radiusScale = (crowdCount.toFloat() * 1.5f).coerceIn(20f, 120f)
                            val angleNoise = (dot * 137.5f) * (Math.PI / 180f) // golden angle spacing
                            
                            // Let dots distribute within a circle around playerX
                            val distanceNoise = kotlin.math.sqrt(dot.toFloat() / maxDotsToRender.toFloat()) * radiusScale
                            val dotX = px + cos(angleNoise).toFloat() * distanceNoise
                            val dotY = py + sin(angleNoise).toFloat() * distanceNoise

                            // Individual crowd running stick head
                            drawCircle(
                                color = if (gameState == CrowdGameState.BOSS_FIGHT) Color(0xFF00FFCC) else Color(0xFF1E40AF),
                                radius = 5f,
                                center = Offset(dotX, dotY)
                            )
                            drawCircle(
                                color = if (gameState == CrowdGameState.BOSS_FIGHT) Color.White else Color(0xFF60A5FA),
                                radius = 2f,
                                center = Offset(dotX, dotY)
                            )
                        }
                    }

                    // --- HTML Elements & Texts on top of canvas using Composable Row/Column Overlay ---
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Math gate labels
                        if (gameState == CrowdGameState.RUNNING) {
                            gateLeft?.let { g ->
                                val gateColor = if (g.isBad) Color(0xFFEF4444) else Color(0xFF00FFCC)
                                Text(
                                    text = "${g.operation}${g.value}",
                                    color = gateColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 32.dp, top = ((g.y / 100f) * 600).dp.coerceAtLeast(0.dp))
                                )
                            }
                            gateRight?.let { g ->
                                val gateColor = if (g.isBad) Color(0xFFEF4444) else Color(0xFF00FFCC)
                                Text(
                                    text = "${g.operation}${g.value}",
                                    color = gateColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 32.dp, top = ((g.y / 100f) * 600).dp.coerceAtLeast(0.dp))
                                )
                            }
                        }

                        // 2. Boss Alarm overlay
                        if (gameState == CrowdGameState.BOSS_WARNING) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.25f + sin(frameCount * 0.2f) * 0.15f))
                                    .padding(vertical = 12.dp)
                                    .align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "🚨 ACHTUNG: CORE OVERLOAD 🚨",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "BOSS FIGHT BEGINNT!",
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // 3. Active Boss HUD (HP Bar)
                        if (gameState == CrowdGameState.BOSS_FIGHT) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Red, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "🛸 NEURO-MECH BOSS",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { bossHP.toFloat() / bossMaxHP.toFloat() },
                                    color = Color.Red,
                                    trackColor = Color.DarkGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .border(0.5.dp, Color.Red, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "HP: $bossHP / $bossMaxHP",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // 4. Common scoring HUD components
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                "SCORE: $score",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (gameState == CrowdGameState.RUNNING) {
                                Text(
                                    "GATES: ${choiceCount}/$maxChoices",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // 5. Crowd Unit marker above player position
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = (playerX * 3.6f).dp.coerceIn(40.dp, 300.dp),
                                    top = 540.dp
                                )
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "CROWD: $crowdCount",
                                color = Color(0xFF00FFCC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // 6. Direct Touch Hold Horizontal movement boundaries
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left division click/hold
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val isPressed = event.changes.any { it.pressed }
                                                if (isPressed) {
                                                    playerX = (playerX - 2.8f).coerceAtLeast(18f)
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                            )
                            // Right division click/hold
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val isPressed = event.changes.any { it.pressed }
                                                if (isPressed) {
                                                    playerX = (playerX + 2.8f).coerceAtMost(82f)
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Lose popup
            if (gameState == CrowdGameState.DEFEAT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "💥 ARMEE VERNICHTET!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Deine Armee konnte den Mech-Boss nicht überwinden. Trainiere deine Rechenkompetenzen und versuche es erneut!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "DEIN SCORE: $score",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                crowdCount = 10
                                playerX = 50f
                                gameState = CrowdGameState.RUNNING
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RETRY", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { gameState = CrowdGameState.LOBBY }
                        ) {
                            Text("HAUPTMENÜ", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Victory popup
            if (gameState == CrowdGameState.VICTORY) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🏆 SIEG GEGEN DEN BOSS!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Hervorragend! Deine unbesiegbare Armee hat den Neuro-Mech Boss in Schutt und Asche gelegt! Du bist der ultimative Rechenkünstler!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "FINALES SCORE: $score",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                crowdCount = 10
                                playerX = 50f
                                gameState = CrowdGameState.RUNNING
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Nochmals spielen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLAY AGAIN", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { gameState = CrowdGameState.LOBBY }
                        ) {
                            Text("HAUPTMENÜ", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
