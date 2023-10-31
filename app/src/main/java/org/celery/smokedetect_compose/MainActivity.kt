package org.celery.smokedetect_compose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.celery.smokedetect_compose.ui.AnalyseActivity
import org.celery.smokedetect_compose.ui.CaptureActivity
import org.celery.smokedetect_compose.ui.theme.SmokeDetect_ComposeTheme

class MainActivity : ComponentActivity() {

    // 在某个地方（比如按钮点击事件）调用以下方法
    fun pickImageFromGallery() {
//        if (!allPermissionsGranted()){
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//            return
//        }
        val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
        pickIntent.type = "image/*"  // 设置选择的文件类型为图片
        startActivityForResult(pickIntent, Companion.PICK_IMAGE_REQUEST)
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                pickImageFromGallery()
            } else {
                Toast.makeText(
                    this, "Permissions not granted by the user.", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    // 在 Activity 或 Fragment 中，处理选择图片后的返回结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Companion.PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val selectedImageUri: Uri? = data.data
                val intent = Intent(this, AnalyseActivity::class.java)
                intent.putExtra(AnalyseActivity.IMAGE_DATA,  selectedImageUri)
                startActivity(intent)
                // 这里可以使用选定的图像URI进行后续操作，比如显示图片或上传文件
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Test.run()
        setContent {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally){
                            Text("看看你有多灰", fontSize = 40.sp, modifier = Modifier.paddingFromBaseline(230.dp,20.dp))
                        Spacer(
                            Modifier
                                .fillMaxHeight()
                                .weight(1f))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button({
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        CaptureActivity::class.java
                                    )
                                )
                            }) {
                                Icon(Icons.Rounded.Camera, "capture photo",Modifier.size(50.dp))
                            }
                            Button({
                                pickImageFromGallery()
                            }) {
                                Icon(Icons.Rounded.FolderCopy, "select from gallery",Modifier.size(50.dp))
                            }
                        }
                    }
            }
        }
    }

    companion object {

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).apply {
        }.toTypedArray()
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val PICK_IMAGE_REQUEST = 1  // 定义请求码
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = false)
@Composable
fun GreetingPreview() {
    SmokeDetect_ComposeTheme {
        IconButton({
        }){
//            Icon(Icons.Rounded.Camera,null)
            Icon(Icons.Rounded.FolderCopy,null)
        }
    }
}