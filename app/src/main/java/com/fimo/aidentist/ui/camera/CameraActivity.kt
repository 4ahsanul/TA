package com.fimo.aidentist.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fimo.aidentist.MainActivity
import com.fimo.aidentist.R
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.databinding.ActivityCameraBinding
import com.fimo.aidentist.databinding.LayoutCameraBinding
import com.fimo.aidentist.helper.Constant
import com.fimo.aidentist.helper.PreferenceHelper
import com.fimo.aidentist.ml.Classifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageFile: File? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraBinding: LayoutCameraBinding

    private val mInputSize = 150
    private val mModelPath = "model.tflite"
    private val mLabelPath = "labels.txt"
    private lateinit var classifier: Classifier

    private lateinit var sharedPref: PreferenceHelper
    private val cameraViewModel: CameraViewModel by viewModels()

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPref = PreferenceHelper(this)

        binding.check.visibility = View.GONE
        binding.retake.visibility = View.GONE
        binding.layout.visibility = View.GONE

        cameraBinding = binding.cameraView

        // Load classifier in background to prevent ANR
        lifecycleScope.launch(Dispatchers.IO) {
            initClassifier()
            Log.d(TAG, "Classifier initialized on background thread")
        }

        binding.retake.setOnClickListener {
            binding.cameraView.root.visibility = View.VISIBLE
            binding.image.visibility = View.GONE
            binding.borderView.visibility = View.VISIBLE
            binding.check.visibility = View.GONE
            binding.retake.visibility = View.GONE
            binding.layout.visibility = View.GONE
        }

        binding.check.setOnClickListener {
            if (!::classifier.isInitialized) {
                Toast.makeText(this, "Model masih loading, coba lagi...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bitmap = ((binding.image).drawable as BitmapDrawable).bitmap
            cameraViewModel.classifyAndSave(bitmap, classifier)
        }

        //Request Camera Permissions
        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        cameraBinding = binding.cameraView
        cameraBinding.cameraCaptureButton.setOnClickListener {
            takePhoto()
            binding.check.visibility = View.VISIBLE
            binding.retake.visibility = View.VISIBLE
            binding.layout.visibility = View.VISIBLE
        }

        cameraBinding.switchCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

            startCamera()
        }

        cameraBinding.closeCamera.setOnClickListener { finish() }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        observeClassificationState()
    }

    private fun observeClassificationState() {
        lifecycleScope.launch {
            cameraViewModel.classificationState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        state.data?.let { result ->
                            Toast.makeText(this@CameraActivity, result.title, Toast.LENGTH_SHORT).show()
                            sharedPref.put(Constant.PREF_EMAIL, result.title)
                        }
                        val intent = Intent(this@CameraActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@CameraActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> { /* TODO: show loading indicator */ }
                    null -> { /* idle state */ }
                }
            }
        }
    }

    private fun initClassifier() {
        classifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)
    }

    private fun takePhoto() {
        //Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        //Create time stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        //Create ouput options object which contains file + metaData
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        //Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    onImageCaptured(savedUri)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                }
            })
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {
            File(
                it, resources.getString(R.string.app_name)
            ).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            //Used to bind lifecycle of camera to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val viewPort = ViewPort.Builder(Rational(350, 170), Surface.ROTATION_0).build()
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture!!)
                .setViewPort(viewPort)
                .build()

            try {
                //Unbind use cases before rebinding
                cameraProvider.unbindAll()

                //Bind use case to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.msg_camera_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun onImageCaptured(uri: Uri) {
        val file = File(uri.path!!)
        imageFile = file

        Glide.with(binding.image).load(file).into(binding.image)
        showImage()
    }

    private fun showImage() {
        binding.borderView.visibility = View.GONE
        binding.cameraView.root.visibility = View.GONE
        binding.image.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
