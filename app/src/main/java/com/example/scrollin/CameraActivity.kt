package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

private data class RepData(
    val timestamp: Long,
    val angle: Double,
    val quality: Float // 0-1 score
)

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvRepCount: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var btnComplete: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var poseDetector: PoseDetector? = null

    private var repCount = 0
    private var targetReps = 10
    private var activityType = "PUSHUPS"
    private var activityName = ""
    private var points = 0

    // ====== OPTIMIZED STATE TRACKING ======
    private var isInDownPosition = false
    private var lastRepTime = 0L
    private val MIN_REP_INTERVAL = 600L  // Reduced to 600ms for faster exercises
    private val repHistory = mutableListOf<RepData>()

    // Hysteresis thresholds - Critical for accuracy
    private val DOWN_THRESHOLD = mutableMapOf(
        "PUSHUPS" to 85.0,      // Elbow angle < 85° = DOWN
        "SQUATS" to 105.0,      // Knee angle < 105° = DOWN
        "JUMPING_JACKS" to 80.0 // Shoulder-wrist distance < 80 = DOWN (ARMS UP)
    )

    private val UP_THRESHOLD = mutableMapOf(
        "PUSHUPS" to 165.0,      // Elbow angle > 165° = UP
        "SQUATS" to 170.0,       // Knee angle > 170° = UP
        "JUMPING_JACKS" to 220.0 // Shoulder-wrist distance > 220 = DOWN (ARMS DOWN)
    )

    // Hysteresis zones - Dead band to prevent jitter
    private val HYSTERESIS_MARGIN = mutableMapOf(
        "PUSHUPS" to 15.0,
        "SQUATS" to 15.0,
        "JUMPING_JACKS" to 30.0
    )

    // Stability counters with increased requirements
    private var consecutiveConfirmedFrames = 0
    private val REQUIRED_STABLE_FRAMES = 5  // Increased from 3 for better stability

    // Confidence tracking
    private val LANDMARK_CONFIDENCE_THRESHOLD = 0.5f
    private var confidenceWarningCount = 0
    private val MAX_CONFIDENCE_WARNINGS = 15

    // Angle history for smoothing
    private val angleHistory = mutableListOf<Double>()
    private val MAX_HISTORY_SIZE = 5
    private var smoothedAngle = 0.0

    // Position validation
    private var lastValidAngle = 0.0
    private var lastValidDistance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: "PUSHUPS"
        targetReps = intent.getIntExtra("TARGET_REPS", 10)
        activityName = intent.getStringExtra("ACTIVITY_NAME") ?: "Exercise"
        points = intent.getIntExtra("POINTS", 10)

        createLayout()
        setupCamera()
    }

    private fun createLayout() {
        val mainLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        val tvTitle = TextView(this).apply {
            text = activityName
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 16)
            setTextColor(android.graphics.Color.WHITE)
        }
        mainLayout.addView(tvTitle)

        previewView = PreviewView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f }
        }
        mainLayout.addView(previewView)

        tvRepCount = TextView(this).apply {
            text = "0 / $targetReps reps"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 16)
            setTextColor(android.graphics.Color.GREEN)
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        mainLayout.addView(tvRepCount)

        tvInstructions = TextView(this).apply {
            text = getInitialInstructions()
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 16, 32, 32)
            setTextColor(android.graphics.Color.YELLOW)
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        mainLayout.addView(tvInstructions)

        btnComplete = Button(this).apply {
            text = "✅ Complete ($targetReps reps needed)"
            textSize = 16f
            setPadding(0, 48, 0, 48)
            isEnabled = false
            alpha = 0.5f
            setOnClickListener {
                completeActivity()
            }
        }
        mainLayout.addView(btnComplete)

        setContentView(mainLayout)
    }

    private fun getInitialInstructions(): String {
        return when (activityType) {
            "PUSHUPS" -> "Get in push-up position\nMake sure your FULL BODY is visible\nHold still for camera to detect you"
            "SQUATS" -> "Stand straight facing camera\nMake sure FULL BODY is visible\nStay still initially"
            "JUMPING_JACKS" -> "Stand straight, arms at sides\nMake sure FULL BODY is visible\nStay still initially"
            "YOGA" -> "Position your mat in view\nHold each pose for 5 seconds"
            else -> "Position yourself in front of camera"
        }
    }

    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
            processImage(imageProxy)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("CameraActivity", "Camera binding failed", e)
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            poseDetector?.process(image)
                ?.addOnSuccessListener { pose ->
                    detectExercise(pose)
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun detectExercise(pose: Pose) {
        // Check if person is detected with good confidence
        if (pose.allPoseLandmarks.isEmpty()) {
            runOnUiThread {
                tvInstructions.text = "⚠️ No person detected\nStep into camera view"
                tvInstructions.setTextColor(android.graphics.Color.RED)
            }
            resetStabilityCounters()
            return
        }

        // Check if all required landmarks are visible with GOOD CONFIDENCE
        val requiredLandmarks = when (activityType) {
            "SQUATS" -> listOf(
                PoseLandmark.LEFT_HIP,
                PoseLandmark.LEFT_KNEE,
                PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP,
                PoseLandmark.RIGHT_KNEE,
                PoseLandmark.RIGHT_ANKLE
            )
            "PUSHUPS" -> listOf(
                PoseLandmark.LEFT_SHOULDER,
                PoseLandmark.LEFT_ELBOW,
                PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.RIGHT_WRIST
            )
            "JUMPING_JACKS" -> listOf(
                PoseLandmark.LEFT_SHOULDER,
                PoseLandmark.LEFT_WRIST,
                PoseLandmark.LEFT_HIP,
                PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.RIGHT_WRIST,
                PoseLandmark.RIGHT_HIP
            )
            else -> emptyList()
        }

        // Validate landmarks exist AND have good confidence
        val allLandmarksVisible = requiredLandmarks.all { landmarkId ->
            val landmark = pose.getPoseLandmark(landmarkId)
            landmark != null && landmark.inFrameLikelihood > LANDMARK_CONFIDENCE_THRESHOLD
        }

        if (!allLandmarksVisible) {
            confidenceWarningCount++
            if (confidenceWarningCount >= MAX_CONFIDENCE_WARNINGS) {
                runOnUiThread {
                    tvInstructions.text = "⚠️ Full body not visible\nStep back so I can see you fully"
                    tvInstructions.setTextColor(android.graphics.Color.YELLOW)
                }
                resetStabilityCounters()
                confidenceWarningCount = 0
            }
            return
        }
        confidenceWarningCount = 0

        // Process exercise detection
        when (activityType) {
            "SQUATS" -> detectSquat(pose)
            "PUSHUPS" -> detectPushup(pose)
            "JUMPING_JACKS" -> detectJumpingJack(pose)
        }
    }
    
    private fun showFormFeedback(angle: Double, isGood: Boolean) {
        runOnUiThread {
            if (isGood) {
                tvInstructions.text = "✅ Good form! Keep going"
                tvInstructions.setTextColor(android.graphics.Color.GREEN)
            } else {
                tvInstructions.text = "⚠️ Go deeper/higher"
                tvInstructions.setTextColor(android.graphics.Color.YELLOW)
            }
        }
    }

    // ====== OPTIMIZED ANGLE SMOOTHING ======
    private fun updateAngleHistory(newAngle: Double): Double {
        angleHistory.add(newAngle)
        if (angleHistory.size > MAX_HISTORY_SIZE) {
            angleHistory.removeAt(0)
        }

        // Calculate median instead of average (more robust to outliers)
        smoothedAngle = angleHistory.sorted()[angleHistory.size / 2].toDouble()
        return smoothedAngle
    }

    // ====== OPTIMIZED SQUAT DETECTION ======
    private fun detectSquat(pose: Pose) {
        // Use BOTH sides for averaging
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP) ?: return
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE) ?: return
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE) ?: return

        val leftKneeAngle = getAngle(leftHip.position3D, leftKnee.position3D, leftAnkle.position3D)
        val rightKneeAngle = getAngle(rightHip.position3D, rightKnee.position3D, rightAnkle.position3D)

        // Average both angles for stability
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2.0
        val smoothedKneeAngle = updateAngleHistory(avgKneeAngle)

        processPositionChange(smoothedKneeAngle, "SQUATS")
    }

    // ====== OPTIMIZED PUSHUP DETECTION ======
    private fun detectPushup(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER) ?: return
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW) ?: return
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST) ?: return

        val leftElbowAngle = getAngle(leftShoulder.position3D, leftElbow.position3D, leftWrist.position3D)
        val rightElbowAngle = getAngle(rightShoulder.position3D, rightElbow.position3D, rightWrist.position3D)

        // Average both angles for stability
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2.0
        val smoothedElbowAngle = updateAngleHistory(avgElbowAngle)

        processPositionChange(smoothedElbowAngle, "PUSHUPS")
    }

    // ====== OPTIMIZED JUMPING JACK DETECTION ======
    private fun detectJumpingJack(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER) ?: return
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST) ?: return

        val leftDistance = abs(leftShoulder.position3D.y - leftWrist.position3D.y)
        val rightDistance = abs(rightShoulder.position3D.y - rightWrist.position3D.y)

        val avgDistance = (leftDistance + rightDistance) / 2.0

        processPositionChange(avgDistance, "JUMPING_JACKS")
    }

    // ====== CORE LOGIC: HYSTERESIS-BASED STATE MACHINE ======
    private fun processPositionChange(measurement: Double, exercise: String) {
        val downThresh = DOWN_THRESHOLD[exercise] ?: 90.0
        val upThresh = UP_THRESHOLD[exercise] ?: 170.0
        val hysteresis = HYSTERESIS_MARGIN[exercise] ?: 15.0

        val isDown: Boolean
        val isUp: Boolean

        if (!isInDownPosition) {
            // Currently in UP position - looking for DOWN
            isDown = measurement < downThresh
            isUp = false
        } else {
            // Currently in DOWN position - looking for UP
            isDown = false
            isUp = measurement > upThresh
        }

        // Log for debugging
        Log.d("ExerciseDetection", "$exercise | Measurement: $measurement | Down: $isDown | Up: $isUp | State: ${if (isInDownPosition) "DOWN" else "UP"}")

        when {
            isDown -> {
                consecutiveConfirmedFrames++

                if (consecutiveConfirmedFrames >= REQUIRED_STABLE_FRAMES && !isInDownPosition) {
                    isInDownPosition = true
                    angleHistory.clear()
                    runOnUiThread {
                        tvInstructions.text = "✅ Good form! Now complete the rep"
                        tvInstructions.setTextColor(android.graphics.Color.GREEN)
                    }
                }
            }

            isUp -> {
                consecutiveConfirmedFrames++

                if (consecutiveConfirmedFrames >= REQUIRED_STABLE_FRAMES && isInDownPosition) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastRepTime > MIN_REP_INTERVAL) {
                        isInDownPosition = false
                        lastRepTime = currentTime
                        repCount++
                        repHistory.add(RepData(currentTime, measurement, 1.0f)) // Add to history
                        consecutiveConfirmedFrames = 0
                        angleHistory.clear()
                        updateRepCount()
                    }
                }
            }

            else -> {
                // In hysteresis zone - keep counter but don't count
                if (consecutiveConfirmedFrames > 0) {
                    consecutiveConfirmedFrames--
                }
            }
        }
    }

    private fun resetStabilityCounters() {
        consecutiveConfirmedFrames = 0
        angleHistory.clear()
    }

    private fun getAngle(
        firstPoint: PointF3D,
        midPoint: PointF3D,
        lastPoint: PointF3D
    ): Double {
        var result = Math.toDegrees(
            atan2((lastPoint.y - midPoint.y).toDouble(), (lastPoint.x - midPoint.x).toDouble()) -
                    atan2((firstPoint.y - midPoint.y).toDouble(), (firstPoint.x - midPoint.x).toDouble())
        )
        result = abs(result)
        if (result > 180) {
            result = 360.0 - result
        }
        return result
    }

    private fun updateRepCount() {
        runOnUiThread {
            tvRepCount.text = "$repCount / $targetReps reps"

            if (repCount >= targetReps) {
                tvRepCount.setTextColor(android.graphics.Color.GREEN)
                tvInstructions.text = "🎉 TARGET ACHIEVED!\nClick Complete to claim $points points"
                tvInstructions.setTextColor(android.graphics.Color.GREEN)
                btnComplete.isEnabled = true
                btnComplete.alpha = 1.0f
                btnComplete.text = "✅ Claim $points Points!"

                Toast.makeText(this, "🎉 Amazing! You completed $targetReps reps!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun completeActivity() {
        val resultIntent = Intent()
        resultIntent.putExtra("REPS_COMPLETED", repCount)
        resultIntent.putExtra("POINTS_EARNED", points)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        poseDetector?.close()
    }
}