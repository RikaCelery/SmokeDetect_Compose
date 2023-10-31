package org.celery.smokedetect_compose.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.celery.smokedetect_compose.R
import org.celery.smokedetect_compose.ui.theme.SmokeDetect_ComposeTheme

class EditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        setContent {
            SmokeDetect_ComposeTheme {
                Surface(
                   color = MaterialTheme.colorScheme.surface
                ){
                    EditorView()
                }
            }
        }
    }
}
@Composable
fun EditorView(){
    val image =
        "http://159.75.127.83/d/public/Snipaste_2022-12-11_22-11-20.png"
    Column {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_launcher_background),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.clip(CircleShape)
        )
        Divider(Modifier.fillMaxWidth())
        Text("上面放的是图片", color = MaterialTheme.colorScheme.primary)
        var pos by remember {
            mutableStateOf(1f)
        }

        Slider(pos , onValueChange = {
            pos = it
        })
    }
}