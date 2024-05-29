package com.example.proyectopdi


import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proyectopdi.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding


    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private var imageAnalysis: ImageAnalysis? = null

    private var filtroNum: Int = 0


    var openUtils = OpenUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
//        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
//
//        viewBinding.applyFilterButton.setOnClickListener{  }

        viewBinding.filtrobtn.setOnClickListener{
            if(filtroNum == 2){
                filtroNum = 0
            }else{
                filtroNum = filtroNum + 1
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if(OpenCVLoader.initLocal()) {
            Toast.makeText(this, "load", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show()
        }
    }






    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder().build()

            imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        val bitmap: Bitmap? = BitmapUtils.getBitmap(image)

                        runOnUiThread {
                            if (bitmap != null) {
                                var newBitmap = when (filtroNum) {
                                    1 -> openUtils.setUtil(bitmap!!)
                                    2 -> openUtils.variableThreshold(bitmap!!)
                                    else -> bitmap
                                }

                                viewBinding.viewImage.setImageBitmap(newBitmap)

                                // Aquí guardamos el bitmap con el filtro aplicado en una variable global
                                filteredBitmap = newBitmap
                            } else {
                                Log.e(TAG, "Grayscale bitmap is null")
                            }
                        }
                        image.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Fallo al vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Variable global para almacenar el bitmap filtrado
    private var filteredBitmap: Bitmap? = null

    private fun takePhoto() {
        // Verificamos si tenemos un bitmap filtrado
        val bitmapToSave = filteredBitmap ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            if (outputStream != null) {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream?.close()

            val msg = "Photo capture succeeded: $it"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            Log.d(TAG, msg)
        }
    }


//    private fun takePhoto() {
//        val imageCapture = imageCapture ?: return
//
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues)
//            .build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults){
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//            }
//        )
//    }
//
//
//
//
//
//
//
//
//
//
//    private fun captureVideo() {}
//
//
//    @OptIn(ExperimentalGetImage::class) private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            imageCapture = ImageCapture.Builder().build()
//
//
//
//            imageAnalysis = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor) { image ->
//                        val bitmap: Bitmap? = BitmapUtils.getBitmap(image)
//
//
//
//
//
//                        runOnUiThread {
//                            if (bitmap != null) {
//                                // val grayBit: Bitmap = toGrayscale(bitmap)
//                                // val bitmapengray = toGrayscale(grayBit)
//
//                                var newBitmap = bitmap
//                                if(filtroNum == 0){
//                                    newBitmap = bitmap
//                                }
//                                if(filtroNum == 1){
//                                    newBitmap = openUtils.setUtil(bitmap!!)
//                                }
//                                if(filtroNum == 2){
//                                    newBitmap = openUtils.variableThreshold(bitmap!!)
//                                }
//
//                                viewBinding.viewImage.setImageBitmap(newBitmap)
//
//                            } else {
//                                Log.e(TAG, "Grayscale bitmap is null")
//                            }
//                        }
//                        image.close()
//                    }
//                }
//
//            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, imageAnalysis, imageCapture)
//            } catch(exc: Exception) {
//                Log.e(TAG, "Fallo al vincular casos de uso", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }



    private fun toGrayscale(bmpOriginal: Bitmap): Bitmap {
        val height = bmpOriginal.height
        val width = bmpOriginal.width

        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val f = ColorMatrixColorFilter(cm)
        paint.setColorFilter(f)
        c.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }




    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }





    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}