package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Game state variables
data class SpaceStar(val id: Int, var x: Float, var y: Float, val speed: Float, val size: Float)
data class PlayerLaser(var x: Float, var y: Float, val isDoublePart: Boolean = false)
data class BossBullet(var x: Float, var y: Float, val dx: Float, val dy: Float)

enum class EnemyType { FIGHTER, SWEEPER, HEAVY }
data class SpaceEnemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var hp: Int,
    val speed: Float,
    var horizontalDir: Float = 1f,
    val width: Float = 40f,
    val height: Float = 40f
)

enum class PowerUpType { SHIELD, DOUBLE_SHOT }
data class SpacePowerUp(val id: Int, val type: PowerUpType, var x: Float, var y: Float, val speed: Float = 3f)

data class SpaceParticle(
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val color: Color,
    val size: Float,
    var currentLife: Int,
    val maxLife: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroSpaceShooterGame(onBack: () -> Unit) {
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }

    // Space dimensions defined out of 100 for easy relative coordinates
    var playerX by remember { mutableStateOf(50f) }
    val playerY = 85f // Fixed Y near bottom

    // Active entities in game loop
    val stars = remember { mutableStateListOf<SpaceStar>() }
    val lasers = remember { mutableStateListOf<PlayerLaser>() }
    val enemies = remember { mutableStateListOf<SpaceEnemy>() }
    val powerUps = remember { mutableStateListOf<SpacePowerUp>() }
    val particles = remember { mutableStateListOf<SpaceParticle>() }
    val bossBullets = remember { mutableStateListOf<BossBullet>() }

    // Shields & weapons
    var hasShield by remember { mutableStateOf(false) }
    var doubleWeaponTimer by remember { mutableStateOf(0) } // frames left of double fire

    // Boss State
    var bossActive by remember { mutableStateOf(false) }
    var bossHP by remember { mutableStateOf(100) }
    var bossMaxHP = 100
    var bossX by remember { mutableStateOf(50f) }
    var bossY by remember { mutableStateOf(-20f) } // starts content offscreen
    var bossMoveDir by remember { mutableStateOf(1f) }
    var frameCount by remember { mutableStateOf(0) }

    // Trigger explosive particles helper
    fun spawnExplosion(cx: Float, cy: Float, col: Color, count: Int = 12) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val vel = Random.nextFloat() * 4f + 1f
            particles.add(
                SpaceParticle(
                    x = cx,
                    y = cy,
                    dx = cos(angle) * vel,
                    dy = sin(angle) * vel,
                    color = col,
                    size = Random.nextFloat() * 6f + 2f,
                    currentLife = 0,
                    maxLife = Random.nextInt(20, 50)
                )
            )
        }
    }

    // Initialize Stars once
    LaunchedEffect(Unit) {
        if (stars.isEmpty()) {
            for (i in 0..40) {
                stars.add(
                    SpaceStar(
                        id = Random.nextInt(),
                        x = Random.nextFloat() * 100f,
                        y = Random.nextFloat() * 100f,
                        speed = Random.nextFloat() * 1.5f + 0.5f,
                        size = Random.nextFloat() * 3f + 1f
                    )
                )
            }
        }
    }

    // Core Game loop and state handlers
    LaunchedEffect(gameStarted, gameOver, gameWon) {
        if (gameStarted && !gameOver && !gameWon) {
            // Reset state
            score = 0
            lives = 3
            playerX = 50f
            lasers.clear()
            enemies.clear()
            powerUps.clear()
            particles.clear()
            bossBullets.clear()
            hasShield = false
            doubleWeaponTimer = 0
            bossActive = false
            bossHP = 100
            bossY = -20f
            
            frameCount = 0
            while (gameStarted && !gameOver && !gameWon) {
                delay(16) // ~60fps
                frameCount++

                // 1. Move Background Stars
                stars.forEach { star ->
                    star.y += star.speed
                    if (star.y > 100f) {
                        star.y = 0f
                        star.x = Random.nextFloat() * 100f
                    }
                }

                // 2. Weapon timers
                if (doubleWeaponTimer > 0) {
                    doubleWeaponTimer--
                }

                // 3. Autoshot weapon interval (every 18 frames)
                if (frameCount % 18 == 0) {
                    if (doubleWeaponTimer > 0) {
                        lasers.add(PlayerLaser(playerX - 6f, playerY - 3f, true))
                        lasers.add(PlayerLaser(playerX + 6f, playerY - 3f, true))
                    } else {
                        lasers.add(PlayerLaser(playerX, playerY - 3f, false))
                    }
                }

                // 4. Update lasers
                val laserIter = lasers.iterator()
                while (laserIter.hasNext()) {
                    val l = laserIter.next()
                    l.y -= 4f
                    if (l.y < 0f) {
                        laserIter.remove()
                    }
                }

                // 5. Spawn Enemies (if boss not active or defeated)
                if (!bossActive && score < 350) {
                    // Spawn normal enemy waves
                    if (frameCount % 45 == 0) {
                        val randX = Random.nextFloat() * 80f + 10f
                        val type = when (Random.nextInt(100)) {
                            in 0..60 -> EnemyType.FIGHTER
                            in 61..85 -> EnemyType.SWEEPER
                            else -> EnemyType.HEAVY
                        }
                        val eHp = if (type == EnemyType.HEAVY) 3 else 1
                        val eSpd = when (type) {
                            EnemyType.FIGHTER -> 1.0f
                            EnemyType.SWEEPER -> 1.6f
                            EnemyType.HEAVY -> 0.6f
                        }
                        enemies.add(
                            SpaceEnemy(
                                id = Random.nextInt(),
                                type = type,
                                x = randX,
                                y = -5f,
                                hp = eHp,
                                speed = eSpd
                            )
                        )
                    }
                } else if (score >= 350 && !bossActive) {
                    // Start Boss Fight Phase!
                    bossActive = true
                    bossHP = bossMaxHP
                    bossX = 50f
                    bossY = -15f
                    // Clear other minor enemies to make room
                    enemies.clear()
                }

                // 6. Update normal Enemies
                val enemyIter = enemies.iterator()
                while (enemyIter.hasNext()) {
                    val e = enemyIter.next()
                    
                    e.y += e.speed
                    
                    // Sweeper moves side-to-side dynamically
                    if (e.type == EnemyType.SWEEPER) {
                        e.x += e.horizontalDir * 1.5f
                        if (e.x > 90f || e.x < 10f) {
                            e.horizontalDir *= -1f
                        }
                    }

                    // Check boundaries
                    if (e.y > 100f) {
                        enemyIter.remove()
                        lives--
                        if (lives <= 0) {
                            gameOver = true
                        }
                    }
                }

                // 7. Update Boss Actions
                if (bossActive) {
                    // descend to 25f
                    if (bossY < 25f) {
                        bossY += 0.4f
                    } else {
                        // oscillate side to side
                        bossX += bossMoveDir * 0.8f
                        if (bossX > 80f || bossX < 20f) {
                            bossMoveDir *= -1f
                        }

                        // Boss shooting bullet storm (every 40 frames)
                        if (frameCount % 40 == 0) {
                            // circular burst
                            for (angleDeg in arrayOf(45, 65, 90, 115, 135)) {
                                val rad = Math.toRadians(angleDeg.toDouble())
                                val dx = cos(rad).toFloat() * 1.8f
                                val dy = sin(rad).toFloat() * 1.8f
                                bossBullets.add(BossBullet(bossX, bossY + 8f, dx, dy))
                            }
                        }
                    }
                }

                // 8. Update Boss Bullets
                val bBulletIter = bossBullets.iterator()
                while (bBulletIter.hasNext()) {
                    val b = bBulletIter.next()
                    b.x += b.dx
                    b.y += b.dy
                    
                    // Boundary check
                    if (b.y > 100f || b.x < 0f || b.x > 100f) {
                        bBulletIter.remove()
                        continue
                    }

                    // Collision with player
                    val distToPlayer = kotlin.math.abs(b.x - playerX)
                    if (b.y >= playerY - 3f && b.y <= playerY + 3f && distToPlayer < 7f) {
                        bBulletIter.remove()
                        spawnExplosion(playerX, playerY, Color.Red, 15)
                        if (hasShield) {
                            hasShield = false
                        } else {
                            lives--
                            if (lives <= 0) {
                                gameOver = true
                            }
                        }
                    }
                }

                // 9. Update Power-Ups
                val puIter = powerUps.iterator()
                while (puIter.hasNext()) {
                    val pu = puIter.next()
                    pu.y += pu.speed
                    
                    // Collision with player
                    if (pu.y >= playerY - 3f && pu.y <= playerY + 3f && kotlin.math.abs(pu.x - playerX) < 8f) {
                        spawnExplosion(pu.x, pu.y, Color(0xFF00FFCC), 8)
                        when (pu.type) {
                            PowerUpType.SHIELD -> hasShield = true
                            PowerUpType.DOUBLE_SHOT -> doubleWeaponTimer = 400 // ~7 seconds activity
                        }
                        puIter.remove()
                    } else if (pu.y > 100f) {
                        puIter.remove()
                    }
                }

                // 10. Lasers colliding with Enemies
                val activeLasers = lasers.toList()
                for (laser in activeLasers) {
                    // Check boss collisions
                    if (bossActive && bossY > 0f) {
                        if (laser.y >= bossY - 10f && laser.y <= bossY + 10f && kotlin.math.abs(laser.x - bossX) < 18f) {
                            lasers.remove(laser)
                            bossHP--
                            spawnExplosion(laser.x, laser.y, Color.Yellow, 2)
                            if (bossHP <= 0) {
                                spawnExplosion(bossX, bossY, Color.Red, 35)
                                score += 1000
                                bossActive = false
                                gameWon = true
                            }
                            continue
                        }
                    }

                    // Check minor enemies collisions
                    val hitEnemy = enemies.find { e ->
                        kotlin.math.abs(laser.x - e.x) < 8f && kotlin.math.abs(laser.y - e.y) < 6f
                    }
                    if (hitEnemy != null) {
                        lasers.remove(laser)
                        hitEnemy.hp--
                        
                        if (hitEnemy.hp <= 0) {
                            // Explode!
                            val pColor = when (hitEnemy.type) {
                                EnemyType.FIGHTER -> Color(0xFF10B981) // emerald
                                EnemyType.SWEEPER -> Color(0xFFEF4444) // red
                                EnemyType.HEAVY -> Color(0xFFF59E0B) // amber
                            }
                            spawnExplosion(hitEnemy.x, hitEnemy.y, pColor, 15)
                            enemies.remove(hitEnemy)
                            score += when (hitEnemy.type) {
                                EnemyType.FIGHTER -> 10
                                EnemyType.SWEEPER -> 25
                                EnemyType.HEAVY -> 50
                            }

                            // Spawn power-up opportunity (15% chance)
                            if (Random.nextInt(100) < 18) {
                                val pType = if (Random.nextBoolean()) PowerUpType.SHIELD else PowerUpType.DOUBLE_SHOT
                                powerUps.add(SpacePowerUp(Random.nextInt(), pType, hitEnemy.x, hitEnemy.y))
                            }
                        } else {
                            spawnExplosion(laser.x, laser.y, Color.White, 3)
                        }
                    }
                }

                // 11. Minor enemy crashing directly into player
                val crashEnemy = enemies.find { e ->
                    kotlin.math.abs(e.x - playerX) < 8f && kotlin.math.abs(e.y - playerY) < 6f
                }
                if (crashEnemy != null) {
                    enemies.remove(crashEnemy)
                    spawnExplosion(crashEnemy.x, crashEnemy.y, Color.Red, 12)
                    if (hasShield) {
                        hasShield = false
                    } else {
                        lives--
                        if (lives <= 0) {
                            gameOver = true
                        }
                    }
                }

                // 12. Update Exploding Particles
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.dx
                    p.y += p.dy
                    p.currentLife++
                    if (p.currentLife >= p.maxLife) {
                        pIter.remove()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A)) // Deep galaxy pitch black
    ) {
        TopAppBar(
            title = {
                Text(
                    "🌌 RETRO ARCADE: SPACE SHOOTER",
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        if (!gameStarted) {
            // Lobby/Lauch screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🌌 SPACE SHOOTER",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Bewege dein Raumschiff durch Schieben (Drag) des Fingers am Bildschirm. Schieße feindliche Schiffe ab und erledige den Boss!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Power up Guide
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF080F1E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔋 FORCE POWER-UPS", color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF00FFCC), RoundedCornerShape(5.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schild - Verhindert die nächste Schadensquelle", color = Color.White, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF00CC), RoundedCornerShape(5.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Double-Shot - Feuert doppelte Frontal-Lasersphären", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { gameStarted = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LAUNCH MISSION 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            // Game screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Detect continuous dragging to move the ship
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Update player x relative from grid 0..100
                            val dxPercent = (dragAmount.x / size.width.toFloat()) * 100f
                            playerX = (playerX + dxPercent).coerceIn(8f, 92f)
                        }
                    }
            ) {
                // Vector Canvas drawing representing the deep space battle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Relative scaling units to convert 0..100 boundary system
                    val scaleX = w / 100f
                    val scaleY = h / 100f

                    // 1. Draw Space scrolling Background stars
                    stars.forEach { star ->
                        drawCircle(
                            color = Color.White.copy(alpha = if (star.speed > 1.2f) 0.9f else 0.4f),
                            radius = star.size,
                            center = Offset(star.x * scaleX, star.y * scaleY)
                        )
                    }

                    // 2. Draw falling Power-Ups
                    powerUps.forEach { pu ->
                        val px = pu.x * scaleX
                        val py = pu.y * scaleY
                        
                        val col = if (pu.type == PowerUpType.SHIELD) Color(0xFF00FFCC) else Color(0xFFFF00CC)
                        // outer rotating hexagon / shield look
                        drawCircle(
                            color = col.copy(alpha = 0.4f),
                            radius = 22f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = col,
                            radius = 12f,
                            center = Offset(px, py)
                        )
                    }

                    // 3. Draw Player Weapons (lasers)
                    lasers.forEach { laser ->
                        val lx = laser.x * scaleX
                        val ly = laser.y * scaleY
                        
                        drawRoundRect(
                            color = if (laser.isDoublePart) Color(0xFFFF33CC) else Color(0xFF00FFCC),
                            topLeft = Offset(lx - 3f, ly - 15f),
                            size = Size(6f, 30f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                    }

                    // 4. Draw Boss bullets
                    bossBullets.forEach { bullet ->
                        drawCircle(
                            color = Color.Red,
                            radius = 12f,
                            center = Offset(bullet.x * scaleX, bullet.y * scaleY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(bullet.x * scaleX, bullet.y * scaleY)
                        )
                    }

                    // 5. Draw normal enemies
                    enemies.forEach { enemy ->
                        val ex = enemy.x * scaleX
                        val ey = enemy.y * scaleY
                        
                        val ecol = when (enemy.type) {
                            EnemyType.FIGHTER -> Color(0xFF10B981) // Green
                            EnemyType.SWEEPER -> Color(0xFFEF4444) // Red
                            EnemyType.HEAVY -> Color(0xFFFBBF24) // Yellow
                        }

                        // Render alien ships custom canvas vectors
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(ex, ey + 24f) // nose pointing down
                            lineTo(ex - 20f, ey - 18f) // wing left
                            lineTo(ex, ey - 8f) // hull back center
                            lineTo(ex + 20f, ey - 18f) // wing right
                            close()
                        }
                        drawPath(path = path, color = ecol)
                        
                        // draw eyes or reactors
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(ex - 8f, ey - 12f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(ex + 8f, ey - 12f)
                        )
                    }

                    // 6. Draw Boss Ship
                    if (bossActive && bossY > -10f) {
                        val bx = bossX * scaleX
                        val by = bossY * scaleY
                        
                        val bossPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(bx, by + 45f) // giant nose downward
                            lineTo(bx - 60f, by - 25f) // wing extreme left
                            lineTo(bx - 30f, by - 40f) // engines inner left
                            lineTo(bx, by - 15f) // hull center back
                            lineTo(bx + 30f, by - 40f) // engines inner right
                            lineTo(bx + 60f, by - 25f) // wing extreme right
                            close()
                        }
                        
                        // draw huge armored boss hull
                        drawPath(path = bossPath, color = Color(0xFF991B1B))
                        drawPath(path = bossPath, color = Color(0xFFEF4444), style = Stroke(width = 3f))

                        // giant engines glow
                        drawCircle(color = Color(0xFFF59E0B), radius = 10f, center = Offset(bx - 20f, by - 35f))
                        drawCircle(color = Color(0xFFF59E0B), radius = 10f, center = Offset(bx + 20f, by - 35f))
                        drawCircle(color = Color(0xFF00FFCC), radius = 14f, center = Offset(bx, by + 20f)) // laser emitter eye
                    }

                    // 7. Draw Player Spaceship
                    val px = playerX * scaleX
                    val py = playerY * scaleY
                    
                    val playerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(px, py - 30f) // nose pointing up
                        lineTo(px - 22f, py + 15f) // wing left
                        lineTo(px, py) // hull back
                        lineTo(px + 22f, py + 15f) // wing right
                        close()
                    }
                    // Cyber cyan player ship
                    drawPath(path = playerPath, color = Color(0xFF1E293B))
                    drawPath(path = playerPath, color = Color(0xFF00FFCC), style = Stroke(width = 4f))

                    // engine exhaust flare
                    drawCircle(
                        color = Color(0xFFFF3333).copy(alpha = Random.nextFloat() * 0.4f + 0.5f),
                        radius = 12f,
                        center = Offset(px, py + 12f)
                    )

                    // 8. Draw Shield sphere wrapper if active
                    if (hasShield) {
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.5f + sin((frameCount * 0.25f).toDouble()).toFloat() * 0.15f),
                            radius = 50f,
                            center = Offset(px, py),
                            style = Stroke(width = 3f)
                        )
                    }

                    // 9. Draw Particle explosions
                    particles.forEach { p ->
                        val alpha = (1f - (p.currentLife.toFloat() / p.maxLife.toFloat())).coerceIn(0f, 1f)
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size,
                            center = Offset(p.x * scaleX, p.y * scaleY)
                        )
                    }
                }

                // HUD Stats overlay (score, life counter, active power-up flags)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score
                        Column {
                            Text("SCORE", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                String.format("%06d", score),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // HP/Lives bar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HP: ", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            for (i in 0 until 3) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow, // substitute for heart icon safely
                                    contentDescription = "Leben",
                                    tint = if (i < lives) Color.Red else Color.DarkGray,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // Boss Alarm / HP panel
                    if (bossActive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "⚠️ BOSS FIGHT IN PROGRESS",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Health bar
                            LinearProgressIndicator(
                                progress = bossHP.toFloat() / bossMaxHP.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .border(1.dp, Color.Red, RoundedCornerShape(5.dp)),
                                color = Color.Red,
                                trackColor = Color.DarkGray,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "BOSS INTENSITY: $bossHP / $bossMaxHP",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Active Weapon status indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (doubleWeaponTimer > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF00CC).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFF00CC), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "⚡ WEAPON LEVEL MAX: DOUBLE LASER (${doubleWeaponTimer / 60}s)",
                                    color = Color(0xFFFF00CC),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary screen Dialog on Win/Lose state
        if (gameOver || gameWon) {
            androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, if (gameWon) Color(0xFF00FFCC) else Color(0xFFEF4444), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (gameWon) "MISSION ERFÜLLT! 🏆" else "DIFFUSION CRITICAL! 💥",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameWon) Color(0xFF00FFCC) else Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("END_SCORE:", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "$score",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    gameStarted = false
                                    gameOver = false
                                    gameWon = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Lobby")
                            }

                            Button(
                                onClick = {
                                    gameOver = false
                                    gameWon = false
                                    gameStarted = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (gameWon) Color(0xFF00FFCC) else Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Retry",
                                    color = if (gameWon) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
