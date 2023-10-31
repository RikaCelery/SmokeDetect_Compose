package org.celery.smokedetect_compose.component

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.celery.smokedetect_compose.AnalyseActivity
import org.celery.smokedetect_compose.R

/**
 * 顶部的预览组件
 */
@Composable
fun PreviewBox(
    previewImage: ImageRequest,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDraw: DrawScope.() -> Unit,
    colorfilter: ColorFilter?,
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
                .aspectRatio(1f),
            colorFilter = colorfilter
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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