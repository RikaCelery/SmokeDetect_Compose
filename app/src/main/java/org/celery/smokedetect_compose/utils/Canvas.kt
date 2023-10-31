package org.celery.smokedetect_compose.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint


fun Canvas.drawScaledBitmap(bitmap: Bitmap, paint: Paint?) {
    val bitmapRatio: Float = bitmap.getWidth() / bitmap.getHeight().toFloat()
    val canvasRatio: Float = getWidth() / getHeight().toFloat()

    var limitWidth = 0
    var limitHeight = 0

    if (bitmapRatio > canvasRatio) {
        limitWidth = getWidth()
    } else {
        limitHeight = getHeight()
    }

    val matrix = Matrix()
    var scaleRatio = 0f
    if (limitWidth > 0) {
        scaleRatio = limitWidth / bitmap.getWidth().toFloat()
        matrix.setScale(scaleRatio, scaleRatio)
        matrix.postTranslate(-(limitWidth-bitmap.width)/2f,0f)
    } else if (limitHeight > 0) {
        scaleRatio = limitHeight / bitmap.getHeight().toFloat()
        matrix.setScale(scaleRatio, scaleRatio)
        matrix.postTranslate(0f,-(limitHeight-bitmap.height)/2f)
    }

    drawBitmap(bitmap, 0f,0f, paint)
}