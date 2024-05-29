package com.example.proyectopdi

import android.graphics.Bitmap
import android.graphics.Color
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class OpenUtils {
    fun setUtil(bitmap: Bitmap):Bitmap{
        val mat = Mat()

        Utils.bitmapToMat(bitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        Imgproc.Laplacian(mat, mat, CvType.CV_8U)

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }

    fun variableThreshold(bitmap: Bitmap): Bitmap {
        val blockSize = 10
        val c = 10

        val width = bitmap.width
        val height = bitmap.height

        val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                var blockSum = 0
                var pixelCount = 0

                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        blockSum += gray
                        pixelCount++
                    }
                }

                val blockMean = blockSum.toDouble() / pixelCount
                val threshold = blockMean - c

                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        pixels[j * width + i] = if (gray > threshold) Color.WHITE else Color.BLACK
                    }
                }
            }
        }

        thresholdBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return thresholdBitmap
    }
}