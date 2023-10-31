package org.celery.smokedetect_compose

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.celery.smokedetect_compose.component.OptionSlider
import org.celery.smokedetect_compose.component.OptionSwitch
import org.celery.smokedetect_compose.component.PreviewBox
import java.io.File
import java.io.FileOutputStream
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

    //用于驱动页面刷新的的一系列变量
    //选区平均亮度
    private var luma by mutableStateOf<Double>(0.0)

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

    //是否开启灰度化
    private var enableGrayScale by mutableStateOf(false)

    //亮度增益
    private var lumaOffset by mutableStateOf(0f)

    //协程作用域
    private val scoop = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: ")
        oriUri = intent.getParcelableExtra(IMAGE_DATA)
        curUri = oriUri
        bitmap = loadImage()
        editPhoto {
            //第一次若取消编辑说明不想要图片需要重新拍照或选照片
            finish()
        }
        scoop.launch {
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
                    //灰度和增益矩阵
                    val colorMatrix = if (enableGrayScale) ColorMatrix(
                        floatArrayOf(
                            0.2126f + 0.2126f * lumaOffset,
                            0.7152f + 0.7152f * lumaOffset,
                            0.0722f + 0.0722f * lumaOffset,
                            0.0f,
                            lumaOffset,
                            0.2126f + 0.2126f * lumaOffset,
                            0.7152f + 0.7152f * lumaOffset,
                            0.0722f + 0.0722f * lumaOffset,
                            0.0f,
                            lumaOffset,
                            0.2126f + 0.2126f * lumaOffset,
                            0.7152f + 0.7152f * lumaOffset,
                            0.0722f + 0.0722f * lumaOffset,
                            0.0f,
                            lumaOffset,
                            0.0f,
                            0.0f,
                            0.0f,
                            1.0f,
                            0F
                        )
                    )
                    //增益矩阵
                    else ColorMatrix(
                        floatArrayOf(
                            1f + lumaOffset,
                            0f,
                            0f,
                            0.0f,
                            0f,
                            0f,
                            1f + lumaOffset,
                            0f,
                            0.0f,
                            0f,
                            0f,
                            0f,
                            1f + lumaOffset,
                            0.0f,
                            0f,
                            0.0f,
                            0.0f,
                            0.0f,
                            1.0f,
                            0F
                        )
                    )
                    PreviewBox(
                        previewImage = previewImage,
                        onClick = onClick,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDraw = onDraw,
                        colorfilter = ColorFilter.colorMatrix(colorMatrix)
                    )
                    Text("选区平均亮度: $luma")
                    val blackLevel = luma2level(luma)
                    Text("对应黑度级别: $blackLevel")
                    LazyColumn(
                        contentPadding = PaddingValues(10.dp, 0.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        item {
                            Slider(percentageStartX, { percentageStartX = it;isChanged = true })
                            Slider(percentageStartY, {
                                percentageStartY = it;isChanged = true;Log.e(
                                TAG,
                                "change:  $isChanged",

                                )
                            })
                            Slider(percentageEndX, { percentageEndX = it;isChanged = true })
                            Slider(percentageEndY, { percentageEndY = it;isChanged = true })
                            OptionSwitch(
                                "开启灰度滤镜", enableGrayScale, Modifier.fillParentMaxWidth()
                            ) { enableGrayScale = it }
                            OptionSlider(
                                "亮度补偿 %.2f".format(lumaOffset),
                                lumaOffset,
                                Modifier.fillParentMaxWidth(),
                                -0.8f..1.5f
                            ) { lumaOffset = it;isChanged = true }
                            Row {
                                Button(onClick = {
                                    scoop.launch(Dispatchers.IO) {
                                        val rendered = renderCanvas()
                                        val fileName =
                                            System.currentTimeMillis().toString() + ".jpg"
                                        if (filesDir.resolve("cache").exists().not())
                                            filesDir.resolve("cache").mkdir()
                                        val file: File = File(filesDir.resolve("cache"), fileName)
                                        val fos = FileOutputStream(file)
                                        rendered.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                                        fos.flush()
                                        fos.close()
                                        Log.d(TAG, "img saved at ${file.absolutePath}")
                                        shareWeChat(FileProvider.getUriForFile(applicationContext,applicationContext.packageName,file))
                                    }}) {
                                    Icon(Icons.Rounded.Share, "Share")
                                }
                                Button(onClick = {
                                    scoop.launch(Dispatchers.IO) {
                                        val rendered = renderCanvas()
                                        val directoryPath =
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_PICTURES
                                            ).path
                                        val fileName =
                                            System.currentTimeMillis().toString() + ".jpg"
                                        val file: File = File(directoryPath, fileName)
                                        val fos = FileOutputStream(file)
                                        rendered.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                                        fos.flush()
                                        fos.close()
                                        Toast.makeText(applicationContext, "img saved at ${file.absolutePath}",Toast.LENGTH_SHORT).show()

                                    }
                                }) {
                                    Icon(Icons.Rounded.SaveAlt, "Save to local")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun shareWeChat(uri: Uri) {
        val shareIntent = Intent()
        //发送图片到朋友圈
        //ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        //发送图片给好友。
//        val comp = ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI")
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

//        shareIntent.setComponent(comp);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, "分享图片"))
    }

    fun renderCanvas(): Bitmap {
        val bitmap = Bitmap.createBitmap(bitmap!!.width, bitmap!!.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(this.bitmap!!, 0f, 0f, Paint())
        Log.d(TAG, "renderCanvas: canvas${canvas.width}x${canvas.height}")
        Log.d(
            TAG,
            "renderCanvas: draw rect[${canvasX * percentageStartX},${canvasY * percentageStartY},${canvasX * percentageEndX},${canvasY * percentageEndY}]"
        )
        val len = canvasX * percentageStartX
        val len2 = canvas.drawRect(canvasX * percentageStartX * (this.bitmap!!.width / canvasX),
            canvasY * percentageStartY * (this.bitmap!!.height / canvasY),
            canvasX * percentageEndX * (this.bitmap!!.width / canvasX),
            canvasY * percentageEndY * (this.bitmap!!.height / canvasY),
            Paint().apply {
                style = Paint.Style.STROKE
                this.color = 0xff00ffff.toInt()
                this.strokeWidth = 4f
            })
        canvas.drawText("luma:$luma", 10f, 50f, Paint().apply {
            textSize = 40f
        })
        canvas.drawText("level:${luma2level(luma)}", 10f, 100f, Paint().apply {
            textSize = 40f
        })
        canvas.save()
        canvas.restore()
        return bitmap
    }

    private fun luma2level(luma: Double): Int {
        val blackAmount = (255.0 - luma) / 255
        val blackLevel = when {
            blackAmount >= 0 && blackAmount < 0.2 -> 0
            blackAmount >= 0.2 && blackAmount < 0.4 -> 1
            blackAmount >= 0.4 && blackAmount < 0.6 -> 2
            blackAmount >= 0.6 && blackAmount < 0.8 -> 3
            blackAmount >= 0.8 && blackAmount < 1 -> 4
            blackAmount >= 1.0 -> 5
            else -> {
                Log.e(TAG, "avg can not be negative $luma");-1
            }
        }
        return blackLevel
    }

    private fun rotateImage(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
        options.setFreeStyleCropEnabled(false)

        // 设置源uri及目标uri
        UCrop.of(
            uri, toUri
        ) // 长宽比
            .withAspectRatio(1f, 1f) // 图片大小
//            .withMaxResultSize(200, 200) // 配置参数
            .withOptions(options).start(this)
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
            (percentageStartY * bigImageHeight).toInt(), (percentageEndY * bigImageHeight).toInt()
        )
        val endX = max(
            (percentageStartX * bigImageWidth).toInt(), (percentageEndX * bigImageWidth).toInt()
        )
        val endY = max(
            (percentageStartY * bigImageHeight).toInt(), (percentageEndY * bigImageHeight).toInt()
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
        Log.v(TAG, "avg luma: $avg1")
        luma = (avg1 + avg1 * lumaOffset).coerceAtMost(255.0)
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

