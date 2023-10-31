package org.celery.smokedetect_compose

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min


class AnalyseActivity : AppCompatActivity() {
    companion object {
        const val IMAGE_DATA = "AnalyseActivityImageData"
        const val TAG = "AnalyseActivity"

        /**
         * rgba 转 灰度值
         */
        inline fun rgbToGrayscale(argb: Int): Int {
            // 将颜色值转换为灰度值
            return rgbToGrayscale(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)
        }

        inline fun rgbToGrayscale(red: Int, green: Int, blue: Int): Int {
            // 将颜色值转换为灰度值
            return (0.2989 * red + 0.5870 * green + 0.1140 * blue).toInt()
        }
    }

    /**
     * 裁剪图片
     */
    private fun cropRawPhoto(uri: Uri, toUri: Uri) {
        // 修改配置参数（我这里只是列出了部分配置，并不是全部）
        val options = UCrop.Options()
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

    //用于驱动页面刷新的的一系列变量
    //选区平均亮度
    private var avg by mutableStateOf<Double>(0.0)

    //裁剪后的bimap
    private var bitmap by mutableStateOf<Bitmap?>(null)

    //选区起始和终点的XY坐标占Canvas大小的比例
    private var percentageStartX by mutableStateOf<Float>(.4f)
    private var percentageStartY by mutableStateOf<Float>(.4f)
    private var percentageEndX by mutableStateOf<Float>(.6f)
    private var percentageEndY by mutableStateOf<Float>(.6f)

    //Canvas大小
    private var canvasX = 1f
    private var canvasY = 1f

    //取消编辑时的动作
    private var onCancel: (() -> Unit)? = null

    //原图uri
    private var oriUri: Uri? = null

    //编辑后图片uri
    private var curUri: Uri? = null

    //选区是否变化
    private var isChanged = true

    //计算亮度的协程作用域
    private val calcScoop = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: ")
        oriUri = intent.getParcelableExtra(IMAGE_DATA)
        curUri = oriUri
        bitmap = loadImage()
        editPhoto {
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

            val previewImage = ImageRequest.Builder(LocalContext.current).apply {
                if (bitmap != null) data(bitmap)
            }.crossfade(true).build()
            //右上角预览按钮
            val onClick = { editPhoto() }
            //开始拖动
            val onDragStart: (Offset) -> Unit = {
                Log.v(TAG, "drag start at $it")
                val imgX = bitmap!!.height
                val imgY = bitmap!!.width
                val bitmapX = (it.x / canvasX) * imgX
                val bitmapY = (it.y / canvasY) * imgY
                percentageStartX = (bitmapX / imgX).coerceAtLeast(0f).coerceAtMost(1f)
                percentageStartY = (bitmapY / imgY).coerceAtLeast(0f).coerceAtMost(1f)
            }
            //处理用户拖动
            val onDrag = { change: PointerInputChange, offset: Offset ->
                Log.v(TAG, "drag ended at ${change.position} $offset")
                val imgX = bitmap!!.height
                val imgY = bitmap!!.width
                val bitmapX = (change.position.x / canvasX) * imgX
                val bitmapY = (change.position.y / canvasY) * imgY
                percentageEndX = (bitmapX / imgX).coerceAtLeast(0f).coerceAtMost(1f)
                percentageEndY = (bitmapY / imgY).coerceAtLeast(0f).coerceAtMost(1f)
                isChanged = true
            }
            //Canvas 选框绘制函数
            val onDraw: DrawScope.() -> Unit = {
                canvasX = drawContext.size.width
                canvasY = drawContext.size.height
                //外围框
                drawRect(
                    Color.Blue,
                    size = Size(canvasX, canvasY),
                    topLeft = Offset.Zero,
                    style = Stroke(4f),
                )
                //选区
                drawRect(
                    Color.Cyan,
                    size = Size(
                        canvasX * percentageEndX - canvasX * percentageStartX,
                        canvasY * percentageEndY - canvasY * percentageStartY
                    ),
                    topLeft = Offset(
                        canvasX * percentageStartX, canvasY * percentageStartY
                    ),
                    style = Stroke(4f),
                )
            }
            Surface {
                Column(Modifier.fillMaxSize()) {
                    PreviewBox(previewImage, onClick, onDragStart, onDrag, onDraw)
                    Text("选区平均亮度: $avg")
                    Slider(percentageStartX, { percentageStartX = it;isChanged = true })
                    Slider(percentageStartY, { percentageStartY = it;isChanged = true })
                    Slider(percentageEndX, { percentageEndX = it;isChanged = true })
                    Slider(percentageEndY, { percentageEndY = it;isChanged = true })
                }
            }
        }
    }


    /**
     * 获取编辑后的bitmap
     */
    private fun loadImage(): Bitmap {
        val stream = curUri?.let { contentResolver.openInputStream(it) }
        val byteArray = stream?.let { stream.readBytes() } ?: ByteArray(0).also {
            Log.e(TAG, "loadImage: uri is null")
        }
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return bitmap
    }

    /**
     * 计算选区亮度
     */
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
        Log.e(TAG, "avg luma: $avg1")
        avg = avg1
    }

    /**
     * 编辑图像
     */
    private fun editPhoto(onCancel: (() -> Unit)? = null) {
        val file = File(filesDir, "analyse.bmp")//输出位置
        if (!file.exists()) file.createNewFile()
        val toUri = Uri.fromFile(file)
        this.onCancel = onCancel
        cropRawPhoto(oriUri!!, toUri)
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            //正常编辑图片后点对勾
            val resultUri = UCrop.getOutput(data!!)
            Log.d(TAG, "result uri: $resultUri")
            //替换当前uri
            curUri = resultUri
            //重新加载bitmap，同时UI会自动更新
            bitmap = loadImage()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Log.e(
                TAG,
                "resultCode:$resultCode(resultCode == UCrop.RESULT_ERROR) requestCode:$requestCode $cropError"
            )
        } else {
            //点击了取消编辑。执行取消时的动作
            onCancel?.invoke()
            Log.e(TAG, "resultCode:$resultCode requestCode:$requestCode")
        }
    }
}

/**
 * 顶部的预览组件
 */
@Composable
private fun PreviewBox(
    previewImage: ImageRequest,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDraw: DrawScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(MaterialTheme.colorScheme.surface)
            .shadow(5.dp)
            .padding(20.dp)
    ) {
        AsyncImage(
            model = previewImage,
            placeholder = painterResource(R.drawable.ic_launcher_background),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(400.dp)
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .requiredHeight(400.dp)
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = onDragStart, onDragCancel = {
                        Log.v(AnalyseActivity.TAG, "drag canceled")
                    }, onDragEnd = {
                        Log.v(AnalyseActivity.TAG, "drag ended")
                    }, onDrag = onDrag
                    )
                }, onDraw
        )

        Button(
            onClick,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.TopEnd)
                .size(50.dp)
        ) {
            Text("edit")
        }
    }
}

