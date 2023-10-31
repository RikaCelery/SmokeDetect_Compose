package org.celery.smokedetect_compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "看看你有多灰",
                        fontSize = 40.sp,
                        modifier = Modifier.paddingFromBaseline(230.dp, 20.dp)
                    )
                    Spacer(
                        Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button({
                            startActivity(
                                Intent(
                                    this@MainActivity, CaptureActivity::class.java
                                )
                            )
                        }) {
                            Icon(Icons.Rounded.Camera, "capture photo", Modifier.size(50.dp))
                        }
                        Button({
                            pickImageFromGallery()
                        }) {
                            Icon(
                                Icons.Rounded.FolderCopy,
                                "select from gallery",
                                Modifier.size(50.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val selectedImageUri: Uri? = data.data
                val intent = Intent(this, AnalyseActivity::class.java)
                intent.putExtra(AnalyseActivity.IMAGE_DATA, selectedImageUri)
                startActivity(intent)
            }
        }
    }

    private fun pickImageFromGallery() {
        val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
        pickIntent.type = "image/*"  // 设置选择的文件类型为图片
        startActivityForResult(pickIntent, PICK_IMAGE_REQUEST)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1  // 定义请求码
    }
}
