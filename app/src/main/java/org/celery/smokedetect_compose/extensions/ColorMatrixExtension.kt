package org.celery.smokedetect_compose.extensions

import androidx.compose.ui.graphics.ColorMatrix

fun ColorMatrix.setConcat(matB: ColorMatrix): ColorMatrix {

    // 将matA和matB展开为二维数组
    val a: FloatArray = values
    val b: FloatArray = matB.values

    // 结果矩阵
    val result = FloatArray(20)

    // 进行矩阵乘法计算
    for (i in 0..3) {
        for (j in 0..4) {
            var sum = 0f
            for (k in 0..4) {
                sum += a[i * 5 + k] * b[k * 5 + j]
            }
            result[i * 5 + j] = sum
        }
    }

    // 将结果矩阵赋值给当前ColorMatrix
    set(ColorMatrix(result))
    return this
}