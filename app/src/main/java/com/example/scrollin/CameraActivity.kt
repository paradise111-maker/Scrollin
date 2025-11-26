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

    // Exercise state tracking - IMPROVED
    private var isInDownPosition = false
    private var lastRepTime = 0L  // NEW: Prevent rapid counting
    private val MIN_REP_INTERVAL = 800L  // NEW: Minimum 800ms between reps

    // Stability checks - NEW
    private var consecutiveDownFrames = 0  // NEW: Must be down for multiple frames
    private var consecutiveUpFrames = 0    // NEW: Must be up for multiple frames
    private val REQUIRED_STABLE_FRAMES = 3 // NEW: Need 3 frames to confirm position

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
            consecutiveDownFrames = 0
            consecutiveUpFrames = 0
            return
        }

        // Check if all required landmarks are visible
        val requiredLandmarks = when (activityType) {
            "SQUATS" -> listOf(
                PoseLandmark.LEFT_HIP,
                PoseLandmark.LEFT_KNEE,
                PoseLandmark.LEFT_ANKLE
            )
            "PUSHUPS" -> listOf(
                PoseLandmark.LEFT_SHOULDER,
                PoseLandmark.LEFT_ELBOW,
                PoseLandmark.LEFT_WRIST
            )
            "JUMPING_JACKS" -> listOf(
                PoseLandmark.LEFT_SHOULDER,
                PoseLandmark.LEFT_WRIST,
                PoseLandmark.LEFT_HIP
            )
            else -> emptyList()
        }

        val allLandmarksVisible = requiredLandmarks.all {
            pose.getPoseLandmark(it) != null
        }

        if (!allLandmarksVisible) {
            runOnUiThread {
                tvInstructions.text = "⚠️ Full body not visible\nStep back so I can see you fully"
                tvInstructions.setTextColor(android.graphics.Color.YELLOW)
            }
            consecutiveDownFrames = 0
            consecutiveUpFrames = 0
            return
        }

        // Process exercise detection
        when (activityType) {
            "SQUATS" -> detectSquat(pose)
            "PUSHUPS" -> detectPushup(pose)
            "JUMPING_JACKS" -> detectJumpingJack(pose)
        }
    }

    private fun detectSquat(pose: Pose) {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return

        val kneeAngle = getAngle(
            leftHip.position3D,
            leftKnee.position3D,
            leftAnkle.position3D
        )

        // More strict angle thresholds
        val isDown = kneeAngle < 110  // Stricter: was 120
        val isUp = kneeAngle > 165    // Stricter: was 160

        if (isDown) {
            consecutiveDownFrames++
            consecutiveUpFrames = 0

            if (consecutiveDownFrames >= REQUIRED_STABLE_FRAMES && !isInDownPosition) {
                isInDownPosition = true
                runOnUiThread {
                    tvInstructions.text = "✅ Good squat! Now stand up"
                    tvInstructions.setTextColor(android.graphics.Color.GREEN)
                }
            }
        } else if (isUp) {
            consecutiveUpFrames++
            consecutiveDownFrames = 0

            if (consecutiveUpFrames >= REQUIRED_STABLE_FRAMES && isInDownPosition) {
                // Check time since last rep
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRepTime > MIN_REP_INTERVAL) {
                    isInDownPosition = false
                    lastRepTime = currentTime
                    repCount++
                    updateRepCount()
                }
            }
        }
    }

    private fun detectPushup(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return

        val elbowAngle = getAngle(
            leftShoulder.position3D,
            leftElbow.position3D,
            leftWrist.position3D
        )

        // Stricter angle thresholds
        val isDown = elbowAngle < 90   // Stricter: was 100
        val isUp = elbowAngle > 165    // Stricter: was 160

        if (isDown) {
            consecutiveDownFrames++
            consecutiveUpFrames = 0

            if (consecutiveDownFrames >= REQUIRED_STABLE_FRAMES && !isInDownPosition) {
                isInDownPosition = true
                runOnUiThread {
                    tvInstructions.text = "✅ Good form! Now push up"
                    tvInstructions.setTextColor(android.graphics.Color.GREEN)
                }
            }
        } else if (isUp) {
            consecutiveUpFrames++
            consecutiveDownFrames = 0

            if (consecutiveUpFrames >= REQUIRED_STABLE_FRAMES && isInDownPosition) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRepTime > MIN_REP_INTERVAL) {
                    isInDownPosition = false
                    lastRepTime = currentTime
                    repCount++
                    updateRepCount()
                }
            }
        }
    }

    private fun detectJumpingJack(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return

        val shoulderWristDistance = abs(leftShoulder.position3D.y - leftWrist.position3D.y)

        // Stricter thresholds
        val isArmsUp = shoulderWristDistance < 80    // Stricter: was 100
        val isArmsDown = shoulderWristDistance > 220 // Stricter: was 200

        if (isArmsUp) {
            consecutiveUpFrames++
            consecutiveDownFrames = 0

            if (consecutiveUpFrames >= REQUIRED_STABLE_FRAMES && !isInDownPosition) {
                isInDownPosition = true
                runOnUiThread {
                    tvInstructions.text = "✅ Arms up! Now bring them down"
                    tvInstructions.setTextColor(android.graphics.Color.GREEN)
                }
            }
        } else if (isArmsDown) {
            consecutiveDownFrames++
            consecutiveUpFrames = 0

            if (consecutiveDownFrames >= REQUIRED_STABLE_FRAMES && isInDownPosition) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRepTime > MIN_REP_INTERVAL) {
                    isInDownPosition = false
                    lastRepTime = currentTime
                    repCount++
                    updateRepCount()
                }
            }
        }
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