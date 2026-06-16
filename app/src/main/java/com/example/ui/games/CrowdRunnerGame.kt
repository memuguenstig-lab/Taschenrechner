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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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

enum class WeaponType {
    RAPID_FIRE,
    SHOTGUN,
    PLASMA_BEAM,
    KAMEHAMEHA
}

data class CrowdWeaponPickup(
    val id: Int,
    var x: Float,
    var y: Float,
    val type: WeaponType,
    var active: Boolean = true
)

data class TrackEnemy(
    val id: Int,
    var x: Float,
    var y: Float,
    var hp: Int,
    val maxHp: Int,
    val color: Color,
    var active: Boolean = true
)

data class CrowdRunnerProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var active: Boolean = true,
    val isWeapon: Boolean = false,
    val color: Color = Color(0xFF00E5FF),
    val damage: Int = 12
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
fun CrowdRunnerGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var gameState by remember { mutableStateOf(CrowdGameState.LOBBY) }
    var score by remember { mutableStateOf(0) }
    var highscore by remember(highScore) { mutableStateOf(highScore) }

    LaunchedEffect(score) {
        if (score > highscore) {
            highscore = score
            onHighScoreUpdate(score)
        }
    }

    var crowdCount by remember { mutableStateOf(15) } // starts with 15 runners
    var playerX by remember { mutableStateOf(50f) } // horizontal position (0..100)
    var choiceCount by remember { mutableStateOf(0) }

    // Multi-boss Progressive Stages State
    var currentStage by remember { mutableStateOf(1) }

    val maxChoices = remember(currentStage) {
        when (currentStage) {
            1 -> 10
            2 -> 15
            3 -> 20
            else -> (20 + (currentStage - 3) * 2).coerceAtMost(35)
        }
    }

    val bossMaxHP = remember(currentStage) {
        when (currentStage) {
            1 -> 350
            2 -> 850
            3 -> 1800
            else -> 1800 + (currentStage - 3) * 600
        }
    }

    // Boss State
    var bossHP by remember { mutableStateOf(350) }
    var bossX by remember { mutableStateOf(50f) }
    var bossY by remember { mutableStateOf(-20f) } // Descends in fight
    var bossDirection by remember { mutableStateOf(1f) }

    // Weapon & Enemy State
    var activeWeapon by remember { mutableStateOf<WeaponType?>(null) }
    var weaponTimer by remember { mutableStateOf(0) }
    
    val trackEnemies = remember { mutableStateListOf<TrackEnemy>() }
    val weaponPickups = remember { mutableStateListOf<CrowdWeaponPickup>() }

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
            trackEnemies.clear()
            weaponPickups.clear()
            activeWeapon = null
            weaponTimer = 0
            generateGates()

            while (gameState == CrowdGameState.RUNNING) {
                delay(16)
                frameCount++

                // Update weapon timer
                if (weaponTimer > 0) {
                    weaponTimer--
                    if (weaponTimer <= 0) {
                        activeWeapon = null
                    }
                }

                // 1. SPAWN WEAPON PICKUPS
                // Spawns much less frequently (every 460 frames instead of 220) to have fewer weapons in general
                if (choiceCount < maxChoices - 1 && frameCount % 460 == 0) {
                    val randX = Random.nextFloat() * 56f + 22f // 22f to 78f
                    // Rare 7% chance to select Kamehameha, otherwise select a standard weapon
                    val wType = if (Random.nextFloat() < 0.07f) {
                        WeaponType.KAMEHAMEHA
                    } else {
                        val standardWeapons = listOf(WeaponType.RAPID_FIRE, WeaponType.SHOTGUN, WeaponType.PLASMA_BEAM)
                        standardWeapons[Random.nextInt(standardWeapons.size)]
                    }
                    weaponPickups.add(
                        CrowdWeaponPickup(
                            id = frameCount,
                            x = randX,
                            y = -10f,
                            type = wType
                        )
                    )
                }

                // Move & Check Weapon Pickups
                val pickupIter = weaponPickups.iterator()
                while (pickupIter.hasNext()) {
                    val p = pickupIter.next()
                    p.y += 1.25f
                    
                    // Collision check
                    if (p.y >= 75f && p.y <= 81f) {
                        if (kotlin.math.abs(p.x - playerX) < 12f) {
                            // Collected!
                            activeWeapon = p.type
                            weaponTimer = 220 // lasts ~3.5 seconds
                            val weaponColor = when (p.type) {
                                WeaponType.RAPID_FIRE -> Color(0xFF10B981)
                                WeaponType.SHOTGUN -> Color(0xFF3B82F6)
                                WeaponType.PLASMA_BEAM -> Color(0xFFD946EF)
                                WeaponType.KAMEHAMEHA -> Color(0xFF00E5FF)
                            }
                            spawnSparks(playerX, 78f, weaponColor, 18)
                            pickupIter.remove()
                        }
                    } else if (p.y > 105f) {
                        pickupIter.remove()
                    }
                }

                // 2. SPAWN TRACK ENEMIES
                // Spawns periodically based on current stage difficulty (easy early, much faster later)
                val spawnEnemyInterval = (75 - (currentStage * 6)).coerceAtLeast(25)
                if (choiceCount < maxChoices - 1 && frameCount % spawnEnemyInterval == 0) {
                    val randX = Random.nextFloat() * 56f + 22f
                    val maxHp = 15 + currentStage * 15
                    val enemyCol = when(currentStage) {
                        1 -> Color(0xFFF87171)
                        2 -> Color(0xFFEF4444)
                        else -> Color(0xFFB91C1C)
                    }
                    trackEnemies.add(
                        TrackEnemy(
                            id = frameCount,
                            x = randX,
                            y = -10f,
                            hp = maxHp,
                            maxHp = maxHp,
                            color = enemyCol
                        )
                    )
                }

                // Move & Update Track Enemies
                val enemyIter = trackEnemies.iterator()
                while (enemyIter.hasNext()) {
                    val enemy = enemyIter.next()
                    enemy.y += 0.95f + (currentStage * 0.05f)

                    // Collision check with players (take damage, reduce crowd count)
                    if (enemy.y >= 75f && enemy.y <= 81f) {
                        if (kotlin.math.abs(enemy.x - playerX) < 12f) {
                            // Hit player crowd! (Can push crowd count into negative!)
                            val damagePenalty = (8 + currentStage * 4).coerceAtMost(50)
                            crowdCount = (crowdCount - damagePenalty).coerceAtLeast(-100)
                            spawnSparks(playerX, 78f, Color.Red, 18)
                            enemyIter.remove()
                            if (crowdCount <= -100) {
                                gameState = CrowdGameState.DEFEAT
                                break
                            }
                        }
                    } else if (enemy.y > 105f) {
                         enemyIter.remove()
                    }
                }

                if (gameState != CrowdGameState.RUNNING) break

                // 3. AUTO-SHOOTING MECHANICS FOR RUNNING PHASE
                val hasActiveWeapon = activeWeapon != null
                val shouldShoot = hasActiveWeapon || !trackEnemies.isEmpty()
                if (shouldShoot) {
                    val fireRate = if (hasActiveWeapon) {
                        when (activeWeapon) {
                            WeaponType.RAPID_FIRE -> 9
                            WeaponType.SHOTGUN -> 22
                            WeaponType.PLASMA_BEAM -> 13
                            WeaponType.KAMEHAMEHA -> 4 // Extremely fast firing Kamehameha blast segments!
                            else -> 30
                        }
                    } else {
                        28 // Slow kinetic default
                    }

                    // Modified from "crowdCount > 0" to allow firing in negative territory!
                    if (frameCount % fireRate == 0) {
                        // Crucial rule: If we are in negative territory, shooting causes self-damage (deeper negative!)
                        if (crowdCount < 0) {
                            crowdCount = (crowdCount - 1).coerceAtLeast(-100)
                        }

                        if (hasActiveWeapon) {
                            when (activeWeapon) {
                                WeaponType.RAPID_FIRE -> {
                                    // Rapid fire green dual lasers
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX - 3f,
                                            y = 75f,
                                            vx = 0f,
                                            vy = -5.0f,
                                            isWeapon = true,
                                            color = Color(0xFF10B981),
                                            damage = 10 + currentStage * 2
                                        )
                                    )
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX + 3f,
                                            y = 75f,
                                            vx = 0f,
                                            vy = -5.0f,
                                            isWeapon = true,
                                            color = Color(0xFF10B981),
                                            damage = 10 + currentStage * 2
                                        )
                                    )
                                }
                                WeaponType.SHOTGUN -> {
                                    // 3-way blue spread
                                    for (angle in arrayOf(-0.5f, 0f, 0.5f)) {
                                        runnerProjectiles.add(
                                            CrowdRunnerProjectile(
                                                x = playerX,
                                                y = 75f,
                                                vx = angle * 2f,
                                                vy = -4.5f,
                                                isWeapon = true,
                                                color = Color(0xFF3B82F6),
                                                damage = 15 + currentStage * 3
                                            )
                                        )
                                    }
                                }
                                WeaponType.PLASMA_BEAM -> {
                                    // Heavy pink explosive energy shell
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX,
                                            y = 75f,
                                            vx = 0f,
                                            vy = -4.2f,
                                            isWeapon = true,
                                            color = Color(0xFFD946EF),
                                            damage = 30 + currentStage * 5
                                        )
                                    )
                                }
                                WeaponType.KAMEHAMEHA -> {
                                    // Massive cyan energy cannon blast segments!
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX,
                                            y = 75f,
                                            vx = 0f,
                                            vy = -6.5f,
                                            isWeapon = true,
                                            color = Color(0xFF00E5FF), // neon electric cyan
                                            damage = 160 + currentStage * 40
                                        )
                                    )
                                    // Trailing explosive gold sparks
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX - 1.2f,
                                            y = 74.5f,
                                            vx = -0.1f,
                                            vy = -6.4f,
                                            isWeapon = true,
                                            color = Color(0xFFFFF176), // bright dragonball yellow
                                            damage = 90 + currentStage * 20
                                        )
                                    )
                                    runnerProjectiles.add(
                                        CrowdRunnerProjectile(
                                            x = playerX + 1.2f,
                                            y = 74.5f,
                                            vx = 0.1f,
                                            vy = -6.4f,
                                            isWeapon = true,
                                            color = Color(0xFFFFF176), // bright dragonball yellow
                                            damage = 90 + currentStage * 20
                                        )
                                    )
                                }
                                else -> {}
                            }
                        } else {
                            // Weak yellow kinetic default shots
                            runnerProjectiles.add(
                                CrowdRunnerProjectile(
                                    x = playerX,
                                    y = 75f,
                                    vx = 0f,
                                    vy = -3.8f,
                                    isWeapon = true,
                                    color = Color(0xFFFBBF24),
                                    damage = 6
                                )
                            )
                        }
                    }
                }

                // Update runner bullets
                val bulletIter = runnerProjectiles.iterator()
                while (bulletIter.hasNext()) {
                    val bullet = bulletIter.next()
                    bullet.x += bullet.vx
                    bullet.y += bullet.vy

                    // Collision checking with track enemies
                    var bulletHit = false
                    val enemiesCopy = trackEnemies.toList()
                    for (enemy in enemiesCopy) {
                        val distance = kotlin.math.hypot(bullet.x - enemy.x, bullet.y - enemy.y)
                        if (distance < 11f && enemy.y > 0f) {
                            bulletHit = true
                            enemy.hp -= bullet.damage
                            spawnSparks(enemy.x, enemy.y, bullet.color, 4)
                            if (enemy.hp <= 0) {
                                spawnSparks(enemy.x, enemy.y, Color(0xFFFBBF24), 14)
                                score += 60 + currentStage * 10
                                trackEnemies.remove(enemy)
                            }
                            break
                        }
                    }

                    if (bulletHit || bullet.y < -15f || bullet.x < 0f || bullet.x > 100f) {
                        bulletIter.remove()
                    }
                }

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

                // If running gates are done and we have cleared track enemies, trigger transition warning to Boss
                if (choiceCount >= maxChoices && gateLeft == null && trackEnemies.isEmpty()) {
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

                // Update weapon timer
                if (weaponTimer > 0) {
                    weaponTimer--
                    if (weaponTimer <= 0) {
                        activeWeapon = null
                    }
                }

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
                
                if (frameCount % spawnInterval == 0 && crowdCount > -100) {
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
                    
                    // Consume 1 from the remaining runners pool as they charge (pushes count deeper negative if already negative!)
                    crowdCount--
                }

                // Weapon bonus projectile fire (Does not consume runner armies!)
                if (activeWeapon != null && frameCount % when(activeWeapon) {
                    WeaponType.RAPID_FIRE -> 8
                    WeaponType.SHOTGUN -> 24
                    WeaponType.PLASMA_BEAM -> 14
                    WeaponType.KAMEHAMEHA -> 3 // Devastating rapid Kamehameha blasts under boss pressure!
                    else -> 20
                } == 0) {
                    when(activeWeapon) {
                        WeaponType.RAPID_FIRE -> {
                            // Rapid fire targeted small green bullets
                            val dx = bossX - playerX
                            val dy = bossY - 78f
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                val speed = 5.5f
                                val vx = (dx / distance) * speed
                                val vy = (dy / distance) * speed
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX - 3f,
                                        y = 75f,
                                        vx = vx,
                                        vy = vy,
                                        isWeapon = true,
                                        color = Color(0xFF10B981),
                                        damage = 15 + currentStage * 3
                                    )
                                )
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX + 3f,
                                        y = 75f,
                                        vx = vx,
                                        vy = vy,
                                        isWeapon = true,
                                        color = Color(0xFF10B981),
                                        damage = 15 + currentStage * 3
                                    )
                                )
                            }
                        }
                        WeaponType.SHOTGUN -> {
                            // 3-way blue spread pointing towards boss center
                            val dx = bossX - playerX
                            val dy = bossY - 78f
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                val speed = 5.0f
                                val vx = (dx / distance) * speed
                                val vy = (dy / distance) * speed
                                
                                val ox = -vy * 0.15f
                                val oy = vx * 0.15f
                                
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX,
                                        y = 75f,
                                        vx = vx - ox,
                                        vy = vy - oy,
                                        isWeapon = true,
                                        color = Color(0xFF3B82F6),
                                        damage = 22 + currentStage * 5
                                    )
                                )
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX,
                                        y = 75f,
                                        vx = vx,
                                        vy = vy,
                                        isWeapon = true,
                                        color = Color(0xFF3B82F6),
                                        damage = 22 + currentStage * 5
                                    )
                                )
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX,
                                        y = 75f,
                                        vx = vx + ox,
                                        vy = vy + oy,
                                        isWeapon = true,
                                        color = Color(0xFF3B82F6),
                                        damage = 22 + currentStage * 5
                                    )
                                )
                            }
                        }
                        WeaponType.PLASMA_BEAM -> {
                            // Heavy pink explosive laser beam
                            val dx = bossX - playerX
                            val dy = bossY - 78f
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                val speed = 4.0f
                                val vx = (dx / distance) * speed
                                val vy = (dy / distance) * speed
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX,
                                        y = 75f,
                                        vx = vx,
                                        vy = vy,
                                        isWeapon = true,
                                        color = Color(0xFFD946EF),
                                        damage = 45 + currentStage * 8
                                    )
                                )
                            }
                        }
                        WeaponType.KAMEHAMEHA -> {
                            // High velocity cyan energy cannon and side helper gold beams targeting the boss
                            val dx = bossX - playerX
                            val dy = bossY - 78f
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                val speed = 7.0f
                                val vx = (dx / distance) * speed
                                val vy = (dy / distance) * speed
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX,
                                        y = 75f,
                                        vx = vx,
                                        vy = vy,
                                        isWeapon = true,
                                        color = Color(0xFF00E5FF),
                                        damage = 180 + currentStage * 40
                                    )
                                )
                                // Left/Right secondary blasts
                                val ox = -vy * 0.12f
                                val oy = vx * 0.12f
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX - 1f,
                                        y = 75f,
                                        vx = vx + ox * 0.5f,
                                        vy = vy + oy * 0.5f,
                                        isWeapon = true,
                                        color = Color(0xFFFFF176),
                                        damage = 90 + currentStage * 20
                                    )
                                )
                                runnerProjectiles.add(
                                    CrowdRunnerProjectile(
                                        x = playerX + 1f,
                                        y = 75f,
                                        vx = vx - ox * 0.5f,
                                        vy = vy - oy * 0.5f,
                                        isWeapon = true,
                                        color = Color(0xFFFFF176),
                                        damage = 90 + currentStage * 20
                                    )
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // 3. Update Runner Projectiles
                val rIter = runnerProjectiles.iterator()
                while (rIter.hasNext()) {
                    val r = rIter.next()
                    r.x += r.vx
                    r.y += r.vy

                    // Collision with boss (boss centered around bossX, bossY)
                    val distToBoss = kotlin.math.hypot(r.x - bossX, r.y - bossY)
                    if (distToBoss < 13f) {
                        // Impact!
                        val finalDmg = if (r.isWeapon) r.damage else 12
                        rIter.remove()
                        bossHP = (bossHP - finalDmg).coerceAtLeast(0)
                        spawnSparks(bossX, bossY, r.color, 6)
                        score += if (r.isWeapon) 15 else 25
                        if (bossHP <= 0) {
                            spawnSparks(bossX, bossY, Color.Red, 45)
                            // Auto transition to next stage immediately! No intermediate victory screen
                            currentStage++
                            crowdCount = crowdCount.coerceAtLeast(15) + 12
                            playerX = 50f
                            gameState = CrowdGameState.RUNNING
                        }
                    } else if (r.y < 0f || r.x < 0f || r.x > 100f) {
                        rIter.remove()
                    }
                }

                // 4. Boss Mech Attack Sequence! (Spitting plasma bursts)
                val fireInterval = (38 - currentStage * 4 - (score / 3000)).coerceAtLeast(10)
                if (frameCount % fireInterval == 0) {
                    val bulletVel = 1.4f + currentStage * 0.25f
                    
                    when (currentStage) {
                        1 -> {
                            for (angleDeg in arrayOf(75, 90, 105)) {
                                val rad = Math.toRadians(angleDeg.toDouble())
                                val bvx = cos(rad).toFloat() * bulletVel
                                val bvy = sin(rad).toFloat() * bulletVel
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, bvx, bvy))
                            }
                        }
                        2 -> {
                            for (angleDeg in arrayOf(65, 80, 100, 115)) {
                                val rad = Math.toRadians(angleDeg.toDouble())
                                val bvx = cos(rad).toFloat() * bulletVel
                                val bvy = sin(rad).toFloat() * bulletVel
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, bvx, bvy))
                            }
                        }
                        3 -> {
                            for (angleDeg in arrayOf(60, 75, 90, 105, 120)) {
                                val rad = Math.toRadians(angleDeg.toDouble())
                                val bvx = cos(rad).toFloat() * bulletVel
                                val bvy = sin(rad).toFloat() * bulletVel
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, bvx, bvy))
                            }
                            val dx = playerX - bossX
                            val dy = 78f - bossY
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                val speedScalar = bulletVel * 0.8f
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, (dx / distance) * speedScalar, (dy / distance) * speedScalar))
                            }
                        }
                        else -> {
                            val offsetDeg = (frameCount % 36) * 5f
                            for (angleDeg in arrayOf(60f + offsetDeg/2, 75f, 90f, 105f, 120f - offsetDeg/2)) {
                                val rad = Math.toRadians(angleDeg.toDouble())
                                val bvx = cos(rad).toFloat() * bulletVel
                                val bvy = sin(rad).toFloat() * bulletVel
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, bvx, bvy))
                            }
                            val dx = playerX - bossX
                            val dy = 78f - bossY
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 0) {
                                bossBullets.add(BossPlasmaBullet(bossX, bossY + 4f, (dx / distance) * bulletVel * 1.1f, (dy / distance) * bulletVel * 1.1f))
                            }
                        }
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
                        val absoluteCrowd = kotlin.math.abs(crowdCount)
                        val penalty = (absoluteCrowd * 0.12f).toInt().coerceAtLeast(15)
                        crowdCount = (crowdCount - penalty).coerceAtLeast(-100)
                        spawnSparks(playerX, 78f, Color(0xFFEF4444), 16)
                        if (crowdCount <= -100) {
                            gameState = CrowdGameState.DEFEAT
                        }
                    } else if (b.y > 100f || b.x < 0f || b.x > 100f) {
                        bIter.remove()
                    }
                }

                // Lose condition: If crowd drops below -100 limit and no projectiles are left to deal final damage
                if (crowdCount <= -100 && runnerProjectiles.isEmpty() && bossHP > 0) {
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
            .background(Color(0xFFF1F5F9)) // Clean Stadium Light Slate
    ) {
        TopAppBar(
            title = {
                Text(
                    "🏃 CROWD RUNNER EVO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color(0xFF0F172A))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE2E8F0))
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
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF475569), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🏃 CROWD RUNNER EVO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "🏆 BEST SCORE: $highscore",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sammle Läufer durch Rechen-Gates! Aber Achtung: In dieser Evolution gibt es fiese rote Minus- und Divisionstüren, die deine Gruppe dezimieren! Drifte geschickt vorbei, um die größte Armee für den Boss Mech aufzubauen! Du kannst dich jetzt sogar ins Minus schießen und deine eigene Gesundheit opfern!",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            currentStage = 1
                            score = 0
                            crowdCount = 15
                            playerX = 50f
                            gameState = CrowdGameState.RUNNING
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RUN & FIGHT 🏃💥", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                // Active Game Render Canvas
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8FAFC)) // Clean, bright stadiums outer fields
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
                                color = Color(0xFFCBD5E1), // Subtle light slate grids
                                start = Offset(w * 0.15f, lineY),
                                end = Offset(w * 0.85f, lineY),
                                strokeWidth = 1.5f
                            )
                        }

                        // Drawing central asphalt running track ribbon (Bright Snowy Highway)
                        drawRect(
                            color = Color(0xFFFFFFFF), // Pure white track
                            topLeft = Offset(w * 0.15f, 0f),
                            size = Size(w * 0.70f, h)
                        )
                        // Neo-cyan border rails for high legibility
                        drawLine(Color(0xFF0E7490), start = Offset(w * 0.15f, 0f), end = Offset(w * 0.15f, h), strokeWidth = 4f)
                        drawLine(Color(0xFF0E7490), start = Offset(w * 0.85f, 0f), end = Offset(w * 0.85f, h), strokeWidth = 4f)

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
                                val gateColor = if (g.isBad) Color(0xFFDC2626) else Color(0xFF059669)
                                drawRect(
                                    color = gateColor.copy(alpha = 0.15f),
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
                                val gateColor = if (g.isBad) Color(0xFFDC2626) else Color(0xFF059669)
                                drawRect(
                                    color = gateColor.copy(alpha = 0.15f),
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
                            val bulletCol = if (r.isWeapon) r.color else Color(0xFF00E5FF)
                            val rRadius = if (r.isWeapon && r.color == Color(0xFFD946EF)) 10f else 6f
                            drawCircle(
                                color = bulletCol,
                                radius = rRadius,
                                center = Offset((r.x / 100f) * w, (r.y / 100f) * h)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = rRadius * 0.4f,
                                center = Offset((r.x / 100f) * w, (r.y / 100f) * h)
                            )
                        }

                        // DRAW TRACK ENEMIES (Highly polished visual droid representation!)
                        if (gameState == CrowdGameState.RUNNING) {
                            trackEnemies.forEach { e ->
                                val ex = (e.x / 100f) * w
                                val ey = (e.y / 100f) * h
                                
                                // 1. Glowing outer circular energy shield
                                drawCircle(
                                    color = e.color.copy(alpha = 0.2f),
                                    radius = 22f,
                                    center = Offset(ex, ey)
                                )
                                drawCircle(
                                    color = e.color.copy(alpha = 0.45f),
                                    radius = 18f,
                                    center = Offset(ex, ey),
                                    style = Stroke(width = 1.5f)
                                )

                                // 2. Left side robotic wing (gorgeous custom path)
                                val leftWing = Path().apply {
                                    moveTo(ex, ey - 6f)
                                    lineTo(ex - 18f, ey + 4f)
                                    lineTo(ex - 6f, ey + 10f)
                                    close()
                                }
                                drawPath(
                                    path = leftWing,
                                    color = Color(0xFF64748B) // Sleek robot metal slate
                                )
                                
                                // 3. Right side robotic wing
                                val rightWing = Path().apply {
                                    moveTo(ex, ey - 6f)
                                    lineTo(ex + 18f, ey + 4f)
                                    lineTo(ex + 6f, ey + 10f)
                                    close()
                                }
                                drawPath(
                                    path = rightWing,
                                    color = Color(0xFF64748B) // Sleek robot metal slate
                                )

                                // 4. Hexagonal outer chassis inner core
                                val hexPath = Path().apply {
                                    moveTo(ex, ey - 10f)
                                    lineTo(ex + 9f, ey - 5f)
                                    lineTo(ex + 9f, ey + 5f)
                                    lineTo(ex, ey + 10f)
                                    lineTo(ex - 9f, ey + 5f)
                                    lineTo(ex - 9f, ey - 5f)
                                    close()
                                }
                                drawPath(
                                    path = hexPath,
                                    color = e.color // Primary neon faction color
                                )

                                // 5. Mechanical core glass shield
                                drawCircle(
                                    color = Color(0xFF0F172A), // Dark central lens
                                    radius = 6f,
                                    center = Offset(ex, ey)
                                )

                                // 6. Glowing white robotic optics (the "eye")
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5f,
                                    center = Offset(ex, ey - 1.5f)
                                )
                                
                                // 7. Horizontal stabilization lines (slits)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.7f),
                                    start = Offset(ex - 5f, ey + 3f),
                                    end = Offset(ex + 5f, ey + 3f),
                                    strokeWidth = 1f
                                )

                                // Draw health bar on top of enemy
                                val hpBarW = 32f
                                val hpPercent = e.hp.toFloat() / e.maxHp.toFloat()
                                drawRect(
                                    color = Color(0xFFCBD5E1),
                                    topLeft = Offset(ex - hpBarW / 2, ey - 22f),
                                    size = Size(hpBarW, 4.5f)
                                )
                                drawRect(
                                    color = Color(0xFF10B981), // Emerald green health indicator
                                    topLeft = Offset(ex - hpBarW / 2, ey - 22f),
                                    size = Size(hpBarW * hpPercent, 4.5f)
                                )
                            }
                        }

                        // DRAW WEAPON PICKUPS
                        if (gameState == CrowdGameState.RUNNING) {
                            weaponPickups.forEach { p ->
                                val px = (p.x / 100f) * w
                                val py = (p.y / 100f) * h
                                val pColor = when (p.type) {
                                    WeaponType.RAPID_FIRE -> Color(0xFF10B981)
                                    WeaponType.SHOTGUN -> Color(0xFF3B82F6)
                                    WeaponType.PLASMA_BEAM -> Color(0xFFD946EF)
                                    WeaponType.KAMEHAMEHA -> Color(0xFF00E5FF)
                                }
                                
                                // Glowing background ring
                                drawCircle(
                                    color = pColor.copy(alpha = 0.25f + sin(frameCount * 0.1f) * 0.1f),
                                    radius = 16f + sin(frameCount * 0.15f) * 3f,
                                    center = Offset(px, py)
                                )
                                // Inner weapon gear
                                drawRect(
                                    color = pColor,
                                    topLeft = Offset(px - 7f, py - 7f),
                                    size = Size(14f, 14f)
                                )
                                // Draw symbol shapes inside Weapon
                                when (p.type) {
                                    WeaponType.RAPID_FIRE -> {
                                        drawLine(Color.White, start = Offset(px - 4f, py), end = Offset(px + 4f, py), strokeWidth = 2.5f)
                                        drawLine(Color.White, start = Offset(px, py - 4f), end = Offset(px, py + 4f), strokeWidth = 2.5f)
                                    }
                                    WeaponType.SHOTGUN -> {
                                        drawCircle(color = Color.White, radius = 3.5f, center = Offset(px, py))
                                    }
                                    WeaponType.PLASMA_BEAM -> {
                                        drawRect(color = Color.White, topLeft = Offset(px - 3.5f, py - 3.5f), size = Size(7f, 7f))
                                    }
                                    WeaponType.KAMEHAMEHA -> {
                                        // Golden sparkling star symbol inside Weapon pickup item shape!
                                        val dragonBallStar = Path().apply {
                                            moveTo(px, py - 5f)
                                            lineTo(px + 1.5f, py - 1.5f)
                                            lineTo(px + 5f, py - 1f)
                                            lineTo(px + 2f, py + 1.5f)
                                            lineTo(px + 3f, py + 5f)
                                            lineTo(px, py + 3f)
                                            lineTo(px - 3f, py + 5f)
                                            lineTo(px - 2f, py + 1.5f)
                                            lineTo(px - 5f, py - 1f)
                                            lineTo(px - 1.5f, py - 1.5f)
                                            close()
                                        }
                                        drawPath(path = dragonBallStar, color = Color(0xFFFFF176))
                                    }
                                }
                            }
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
                                val gateColor = if (g.isBad) Color(0xFFDC2626) else Color(0xFF059669)
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
                                val gateColor = if (g.isBad) Color(0xFFDC2626) else Color(0xFF059669)
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
                            val bossName = when (currentStage) {
                                1 -> "🛸 CYBER SENTINEL"
                                2 -> "🛸 TITANIUM NEURO-OVERLORD"
                                3 -> "🛸 DOOM-BRINGER OBLITERATOR"
                                else -> "🛸 OMEGA DREADNOUGHT MK $currentStage"
                            }
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
                                    text = bossName,
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
                                    color = Color(0xFF0F172A), // Dark slate text
                                    fontSize = 11.sp,
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
                                color = Color(0xFF0F172A), // Dark slate text
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (gameState == CrowdGameState.RUNNING) {
                                Text(
                                    "GATES: ${choiceCount}/$maxChoices",
                                    color = Color(0xFF475569), // Darker slate gray text
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // STAGE & WEAPON HUD Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Current Stage info
                            Text(
                                "STAGE: $currentStage",
                                color = Color(0xFF0F172A), // Dark slate text
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Active Weapon display
                            if (activeWeapon != null) {
                                val wLabel = when (activeWeapon) {
                                    WeaponType.RAPID_FIRE -> "LASER GUNS"
                                    WeaponType.SHOTGUN -> "V-SHOTGUN"
                                    WeaponType.PLASMA_BEAM -> "PLASMA BLAST"
                                    else -> ""
                                }
                                val wColor = when (activeWeapon) {
                                    WeaponType.RAPID_FIRE -> Color(0xFF10B981)
                                    WeaponType.SHOTGUN -> Color(0xFF3B82F6)
                                    WeaponType.PLASMA_BEAM -> Color(0xFFD946EF)
                                    else -> Color.White
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(wColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, wColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(wColor, RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$wLabel (${weaponTimer / 60}s)",
                                        color = wColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
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

                        // 6. Direct Touch / Drag / Swipe to free move playerX smoothly
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val screenW = size.width.toFloat()
                                            if (screenW > 0f) {
                                                val deltaX = (dragAmount.x / screenW) * 100f
                                                playerX = (playerX + deltaX).coerceIn(18f, 82f)
                                            }
                                        },
                                        onDragStart = { offset ->
                                            val screenW = size.width.toFloat()
                                            if (screenW > 0f) {
                                                val touchPercent = (offset.x / screenW) * 100f
                                                playerX = touchPercent.coerceIn(18f, 82f)
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }

            // Lose popup (Bright theme)
            if (gameState == CrowdGameState.DEFEAT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A).copy(alpha = 0.5f)), // Modern soft shadow blur overlay
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White, RoundedCornerShape(16.dp))
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
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "DEIN SCORE: $score",
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                crowdCount = 15
                                playerX = 50f
                                gameState = CrowdGameState.RUNNING
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RETRY", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { gameState = CrowdGameState.LOBBY }
                        ) {
                            Text("HAUPTMENÜ", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
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
                            "🏆 STUFE $currentStage GEKLÄRT!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Hervorragend! Du hast den Boss besiegt und Stufe $currentStage gemeistert! Hol dir zusätzliche Rekruten und stürme die nächste Stufe!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "SCORE: $score",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Next Stage button as primary action
                        Button(
                            onClick = {
                                currentStage++
                                crowdCount = crowdCount.coerceAtLeast(15) + 12
                                playerX = 50f
                                gameState = CrowdGameState.RUNNING
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("NÄCHSTE STUFE (STAGE ${currentStage + 1}) 🚀", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Replay current stage
                        TextButton(
                            onClick = {
                                crowdCount = 15
                                playerX = 50f
                                gameState = CrowdGameState.RUNNING
                            }
                        ) {
                            Text("DIESE STUFE WIEDERHOLEN", color = Color.LightGray, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = {
                                gameState = CrowdGameState.LOBBY
                            }
                        ) {
                            Text("HAUPTMENÜ", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
