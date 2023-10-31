package org.celery.smokedetect_compose.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.celery.smokedetect_compose.R
import java.io.File
import kotlin.math.max
import kotlin.math.min


class AnalyseActivity : AppCompatActivity() {
    companion object {
        const val IMAGE_DATA = "AnalyseActivityImageData"
        private const val TAG = "CameraXApp"
        inline fun rgbToGrayscale(argb: Int): Int {
            // 将颜色值转换为灰度值
            return rgbToGrayscale(argb shr 16 and 0xFF,argb shr 8 and 0xFF,argb and 0xFF)
        }
        inline fun rgbToGrayscale(red: Int, green: Int, blue: Int): Int {
            // 将颜色值转换为灰度值
            return (0.2989 * red + 0.5870 * green + 0.1140 * blue).toInt()
        }
    }

    fun cropRawPhoto(uri: Uri, toUri: Uri) {
        // 修改配置参数（我这里只是列出了部分配置，并不是全部）
        val options = UCrop.Options()
        // 修改标题栏颜色
//        options.setToolbarColor(getResources().getColor(R.color.colorPrimary))
        // 修改状态栏颜色
//        options.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark))
        // 隐藏底部工具
        options.setHideBottomControls(false)
        // 图片格式
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        // 设置图片压缩质量
        options.setCompressionQuality(100)
        // 是否让用户调整范围(默认false)，如果开启，可能会造成剪切的图片的长宽比不是设定的
        // 如果不开启，用户不能拖动选框，只能缩放图片
        options.setFreeStyleCropEnabled(true)

        // 设置源uri及目标uri
        UCrop.of(
            uri, toUri
        ) // 长宽比
//            .withAspectRatio(1f, 1f) // 图片大小
//            .withMaxResultSize(200, 200) // 配置参数
            .withOptions(options).start(this)
    }

    var avg by mutableStateOf<Double>(0.0)
    var bitmap by mutableStateOf<Bitmap?>(null)
    var percentageStartX by mutableStateOf<Float>(.4f)
    var percentageStartY by mutableStateOf<Float>(.4f)
    var percentageEndX by mutableStateOf<Float>(.6f)
    var percentageEndY by mutableStateOf<Float>(.6f)
    var canvasX = 1f
    var canvasY = 1f
    var onCancel :(()->Unit)?=null
    var oriUri: Uri? = null
    var curUri: Uri? = null
    var isChanged = true
    val calcScoop = CoroutineScope(SupervisorJob())


    fun loadImage(): Bitmap {
        val stream = curUri?.let { contentResolver.openInputStream(it) }
        val byteArray = stream?.let { stream.readBytes() } ?: ByteArray(0).also {
            Log.e(TAG, "loadImage: uri is null")
        }
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return bitmap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: ")
        oriUri = intent.getParcelableExtra(IMAGE_DATA)
        curUri = oriUri
        bitmap = loadImage()
        editPhoto{
            finish()
        }
        calcScoop.launch {
            while (isActive) {
                if (isChanged) {
                    calculateSelectedLuma()
                    isChanged = false
                }
                delay(100)
            }
        }
        setContent {
            Surface {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .background(MaterialTheme.colorScheme.surface)
                            .shadow(5.dp)
                            .padding(20.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).apply {
                                if (bitmap != null) data(bitmap)
                            }.crossfade(true).build(),
                            placeholder = painterResource(R.drawable.ic_launcher_background),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeight(400.dp)
                        )
//                        var startOffset by remember { mutableStateOf(Offset(0f, 0f)) }
//                        var endOffset by remember { mutableStateOf(Offset(0f, 0f)) }
//                        var tempSelection = Pair(Offset.Zero, Offset.Zero)
//                        var job: Job? = null
//                        var onLastDrag: () -> Unit = {}
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(400.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(onDragStart = {
                                        Log.v(TAG, "drag start at $it")
                                        val imgX = bitmap!!.height
                                        val imgY = bitmap!!.width
                                        val bitmapX = (it.x / canvasX) * imgX
                                        val bitmapY = (it.y / canvasY) * imgY
                                        percentageStartX = (bitmapX / imgX)
                                            .coerceAtLeast(0f)
                                            .coerceAtMost(1f)
                                        percentageStartY = (bitmapY / imgY)
                                            .coerceAtLeast(0f)
                                            .coerceAtMost(1f)
                                    },
                                        onDragCancel = {
                                            Log.v(TAG, "drag canceled")
                                        },
                                        onDragEnd = {}) { change: PointerInputChange, offset: Offset ->
                                        Log.v(TAG, "drag ended at ${change.position} $offset")
                                        val imgX = bitmap!!.height
                                        val imgY = bitmap!!.width
                                        val bitmapX = (change.position.x / canvasX) * imgX
                                        val bitmapY = (change.position.y / canvasY) * imgY
                                        percentageEndX = (bitmapX / imgX)
                                            .coerceAtLeast(0f)
                                            .coerceAtMost(1f)
                                        percentageEndY = (bitmapY / imgY)
                                            .coerceAtLeast(0f)
                                            .coerceAtMost(1f)
                                        isChanged = true
                                    }
                                }) {
//                            Log.v(TAG, "canvas size: ${this.drawContext.size}")
//                            Log.v(TAG, "bitmap size: ${bitmap?.width}x${bitmap?.width}")
                            val imgX = bitmap!!.height
                            val imgY = bitmap!!.width

                            canvasX = drawContext.size.width
                            canvasY = drawContext.size.height
                            val imgPointStart =
                                Offset(imgX * percentageStartX, imgY * percentageStartY) // 图像上的点
                            val canvasPointStart = Offset(
                                imgPointStart.x * canvasX / imgX, imgPointStart.y * canvasY / imgY
                            )
                            val imgPointEnd =
                                Offset(imgX * percentageEndX, imgY * percentageEndY) // 图像上的点
                            val canvasPointEnd =
                                Size(imgPointEnd.x * canvasX / imgX, imgPointEnd.y * canvasY / imgY)

                            this.drawRect(
                                Color.Blue,
                                size = Size(canvasX, canvasY),
//                                size = Size(offsetEndX, offsetEndY),
                                topLeft = Offset.Zero,
                                style = Stroke(4f),
                            )
                            this.drawRect(
                                Color.Cyan,
                                size = Size(
                                    canvasX * percentageEndX - canvasX * percentageStartX,
                                    canvasY * percentageEndY - canvasY * percentageStartY
                                ),
//                                size = Size(offsetEndX, offsetEndY),
                                topLeft = Offset(
                                    canvasX * percentageStartX, canvasY * percentageStartY
                                ),
                                style = Stroke(4f),
                            )
                        }

                        Button(
                            { editPhoto() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.TopEnd)
                                .size(50.dp)
                        ) {
                            Text("edit")
                        }
                    }
                    Text("Result: $avg")
                    Slider(percentageStartX, { percentageStartX = it;isChanged = true })
                    Slider(percentageStartY, { percentageStartY = it;isChanged = true })
                    Slider(percentageEndX, { percentageEndX = it;isChanged = true })
                    Slider(percentageEndY, { percentageEndY = it;isChanged = true })
                }
            }
        }
    }

    private fun calculateSelectedLuma() {
        // 大图的大小
        val bigImageWidth = canvasX
        val bigImageHeight = canvasY

        // 小图的大小
        val smallImageWidth = bitmap!!.width
        val smallImageHeight = bitmap!!.height

        // 大图中区域的像素范围
        val startX = min(
            (percentageStartX * bigImageWidth).toInt(), (percentageEndX * bigImageWidth).toInt()
        )
        val startY = min(
            (percentageEndX * bigImageHeight).toInt(), (percentageEndY * bigImageHeight).toInt()
        )
        val endX = max(
            (percentageStartX * bigImageWidth).toInt(), (percentageEndX * bigImageWidth).toInt()
        )
        val endY = max(
            (percentageEndX * bigImageHeight).toInt(), (percentageEndY * bigImageHeight).toInt()
        )

        // 映射到小图中的坐标范围
        val mappedStartX = (startX.toDouble() / bigImageWidth * smallImageWidth).toInt()
        val mappedEndX = (endX.toDouble() / bigImageWidth * smallImageWidth).toInt()
        val mappedStartY = (startY.toDouble() / bigImageHeight * smallImageHeight).toInt()
        val mappedEndY = (endY.toDouble() / bigImageHeight * smallImageHeight).toInt()
        mappedStartX.coerceIn(0, smallImageWidth)
        mappedStartY.coerceIn(0, smallImageHeight)
        mappedEndX.coerceIn(0, smallImageWidth)
        mappedEndY.coerceIn(0, smallImageHeight)
        // 输出映射后的坐标范围
        Log.v(TAG, "Mapped coordinates in small image:")
        Log.v(TAG, "X Range: $mappedStartX to $mappedEndX")
        Log.v(TAG, "Y Range: $mappedStartY to $mappedEndY")
        if (mappedEndX - mappedStartX < 0) return
        var n = 0
        var sum = 0.0
        for (i in mappedStartX..mappedEndX) for (j in mappedStartY..mappedEndY) {
            try {
                sum += rgbToGrayscale(bitmap!!.getPixel(i, j) and 0xFFFFFF)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return
            }
            n++
        }

        val avg1 = sum / n
        Log.e(TAG, "avg1: $avg1")
        avg = avg1

        val intArray =
            intArrayOf(mappedEndX.minus(mappedStartX).times(mappedEndY.minus(mappedStartY)))
//        try {
//            Log.e(TAG, "bitmap: ${bitmap?.width}x${bitmap?.height}")
//            bitmap!!.getPixels(
//                intArray,
//                0,
//                smallImageWidth,
//                mappedStartX,
//                mappedStartY,
//                mappedEndX - mappedStartX,
//                mappedEndY - mappedStartY
//            )
//
//            val avg1 = intArray.map { it and 0xFF }.average()
//            Log.e(TAG, "avg1: $avg1")
//            avg = avg1
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    private fun editPhoto(onCancel:(()->Unit)?=null) {
//        Log.d(TAG, "tmp uri: ${bitmap!!.width}x${bitmap!!.height}")
        val file = File(filesDir, "analyse.bmp")
        if (!file.exists()) file.createNewFile()
        val toUri = Uri.fromFile(file)
        this.onCancel = onCancel
        cropRawPhoto(oriUri!!, toUri)
    }


    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            Log.d(TAG, resultUri.toString())
            curUri = resultUri
            bitmap = loadImage()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Log.e(TAG,"resultCode:$resultCode(resultCode == UCrop.RESULT_ERROR) requestCode:$requestCode ")
        }else{
            onCancel?.invoke()
            Log.e(TAG,"resultCode:$resultCode requestCode:$requestCode")
        }
    }
}

@Preview
@Composable
fun PreviewGreeting() {
    EditorView()
}
