package org.celery.smokedetect_compose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.celery.smokedetect_compose.databinding.ActivityCaptureBinding
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * 拍照页面
 */
class CaptureActivity : AppCompatActivity() {
    /**
     * 亮度分析器
     */
    private class LuminosityAnalyzer(private val listener: (luma: Double) -> Unit) :
        ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    private lateinit var viewBinding: ActivityCaptureBinding

    /**
     * 是否已按下拍照按钮
     */
    private var isCapturing = false
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //****初始化****
        viewBinding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }// 设定拍照按钮


        if (allPermissionsGranted()) {
            // 启动照相机
            startCamera()
        } else {
            // 请求照相机权限
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this, "未取得照相机权限", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    //拍照
    private fun takePhoto() {
        if (isCapturing) return//正在处理时忽略后续请求
        isCapturing = true
        val imageCapture = imageCapture
        requireNotNull(imageCapture){
            "imageCapture is null."
        }

        //显示进度条和黑色遮挡
        viewBinding.progressBar2.visibility = View.VISIBLE
        viewBinding.bg.visibility = View.VISIBLE

        val photoFile = File(filesDir, "import.bmp")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()


        imageCapture.takePicture(outputOptions,ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            @OptIn(ExperimentalGetImage::class)
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                //启动分析页面
                val intent = Intent(this@CaptureActivity, AnalyseActivity::class.java)
                intent.putExtra(AnalyseActivity.IMAGE_DATA,  Uri.fromFile(photoFile))
                startActivity(intent)
                isCapturing = false
            }

            override fun onError(exc: ImageCaptureException) {
                isCapturing = false
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(
                    applicationContext, "拍照失败: ${exc.message}", Toast.LENGTH_LONG
                ).show()
            }

        })
    }

    override fun onResume() {
        super.onResume()
        //页面恢复时重启相机
        startCamera()
    }

    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalZeroShutterLag::class)
    private fun startCamera() {
        //隐藏进度条和黑色遮挡
        viewBinding.progressBar2.visibility = View.INVISIBLE
        viewBinding.bg.visibility = View.INVISIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().setIoExecutor(Dispatchers.IO.asExecutor())
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .setZslDisabled(false)
                .build()
            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->

                    viewBinding.lumaListener.text = "平均亮度: %.2f".format(luma)
                })
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        //关闭线程池
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CaptureActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}