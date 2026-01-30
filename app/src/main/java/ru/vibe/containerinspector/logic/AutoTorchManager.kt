package ru.vibe.containerinspector.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.CameraControl
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AutoTorchManager(
    context: Context,
    private val getCurrentStep: () -> Int
) : DefaultLifecycleObserver, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    
    private var cameraControl: CameraControl? = null
    private var isTorchOn = false

    fun setCameraControl(control: CameraControl?) {
        this.cameraControl = control
        // If we lost control, ensure we reset our internal state
        if (control == null) isTorchOn = false
    }

    override fun onResume(owner: LifecycleOwner) {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
        // Ensure torch is off when backgrounded
        disableTorch()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            val currentStep = getCurrentStep()
            
            // Steps 1-3 from DOMAIN_LOGIC.md (0, 1, 2 in 0-indexed code)
            // 1. Контейнер открыт и пуст.
            // 2. Груз (вид 1).
            // 3. Груз (вид 2).
            val isInteriorStep = currentStep in 0..2
            
            if (isInteriorStep && lux < 10f) {
                enableTorch()
            } else {
                disableTorch()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun enableTorch() {
        if (!isTorchOn) {
            cameraControl?.enableTorch(true)
            isTorchOn = true
        }
    }

    private fun disableTorch() {
        if (isTorchOn) {
            cameraControl?.enableTorch(false)
            isTorchOn = false
        }
    }
}
