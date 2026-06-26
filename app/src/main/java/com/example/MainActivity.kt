package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.example.ui.AppViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var viewModel: AppViewModel
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Shake debounce variables
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 2.4f // g-force thresh
    private val SHAKE_COOLDOWN_MS = 1000L

    // Face-down debounce variables
    private var faceDownStartTime = 0L
    private val FACE_DOWN_DEBOUNCE_MS = 200L // require 200ms of face down to trigger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}

        // Setup ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Setup sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            MyApplicationTheme {
                LaunchedEffect(viewModel.isFullScreen) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (viewModel.isFullScreen) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
                
                MainAppScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.let { sm ->
            accelerometer?.let { acc ->
                sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
            }
            gyroscope?.let { gyro ->
                sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (!::viewModel.isInitialized) return

        if (!viewModel.isSecretUnlocked || !viewModel.isPanicLockEnabled) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // 1. Shake Detection via Accelerometer (G-force calculation)
                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                if (gForce > SHAKE_THRESHOLD) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                        lastShakeTime = now
                        triggerPanicLock()
                    }
                }

                // 2. Face Down Detection (z goes negative when screen faces down)
                if (z < -8.5f) {
                    if (faceDownStartTime == 0L) {
                        faceDownStartTime = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - faceDownStartTime > FACE_DOWN_DEBOUNCE_MS) {
                            triggerPanicLock()
                        }
                    }
                } else {
                    faceDownStartTime = 0L
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val rx = event.values[0]
                val ry = event.values[1]
                val rz = event.values[2]

                // Calculate rotational speed (angular velocity magnitude)
                val rotationSpeed = sqrt((rx * rx + ry * ry + rz * rz).toDouble())
                // Threshold for quick rotation/panic flip / shaking
                if (rotationSpeed > 5.0) { // rad/s
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                        lastShakeTime = now
                        triggerPanicLock()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun triggerPanicLock() {
        runOnUiThread {
            if (viewModel.isSecretUnlocked) {
                viewModel.lockSecretMode()
            }
        }
    }
}
