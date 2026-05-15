package cz.nicolsburg.boardflow.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import kotlin.math.abs

data class ScanQualityIssue(
    val kind: String,
    val detail: String,
    val userMessage: String
)

data class ScanQualityResult(
    val isAcceptable: Boolean,
    val issues: List<ScanQualityIssue>
)

object ScanImageQualityAnalyzer {

    private const val TAG = "ScanQuality"
    private const val ANALYSIS_MAX_DIM = 256
    private const val LUMA_THRESHOLD = 45.0
    private const val BLUR_THRESHOLD = 80.0
    private const val MIN_DIMENSION_PX = 640
    private const val MIN_CONTENT_AREA_RATIO = 0.34

    fun analyze(file: File): ScanQualityResult {
        val issues = mutableListOf<ScanQualityIssue>()

        val sizeOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, sizeOpts)
        val origW = sizeOpts.outWidth
        val origH = sizeOpts.outHeight
        val shortEdge = if (origW > 0 && origH > 0) minOf(origW, origH) else 0

        if (shortEdge in 1 until MIN_DIMENSION_PX) {
            issues += ScanQualityIssue(
                kind = "resolution",
                detail = "Image too small: ${origW}x${origH}",
                userMessage = "Image resolution looks low"
            )
            Log.d(TAG, "resolution: ${origW}x${origH} < $MIN_DIMENSION_PX")
        }

        val sample = if (shortEdge > 0) maxOf(1, shortEdge / ANALYSIS_MAX_DIM) else 1
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: run {
            Log.w(TAG, "Could not decode image for quality check")
            return ScanQualityResult(isAcceptable = true, issues = emptyList())
        }

        try {
            val luma = averageLuma(bitmap)
            Log.d(TAG, "avg luma=${"%.1f".format(luma)}")
            if (luma < LUMA_THRESHOLD) {
                issues += ScanQualityIssue(
                    kind = "brightness",
                    detail = "Too dark (luma=${"%.1f".format(luma)})",
                    userMessage = "Image looks too dark"
                )
            }

            val blurScore = laplacianVariance(bitmap)
            Log.d(TAG, "laplacian variance=${"%.1f".format(blurScore)}")
            if (blurScore < BLUR_THRESHOLD) {
                issues += ScanQualityIssue(
                    kind = "blur",
                    detail = "Low sharpness (score=${"%.1f".format(blurScore)})",
                    userMessage = "Image looks blurry"
                )
            }

            val contentAreaRatio = contentAreaRatio(bitmap, luma)
            if (contentAreaRatio != null) {
                Log.d(TAG, "content area ratio=${"%.2f".format(contentAreaRatio)}")
                if (contentAreaRatio < MIN_CONTENT_AREA_RATIO) {
                    issues += ScanQualityIssue(
                        kind = "distance",
                        detail = "Likely score sheet area is small (${"%.2f".format(contentAreaRatio)})",
                        userMessage = "Score sheet looks too far away"
                    )
                }
            }
        } finally {
            bitmap.recycle()
        }

        val ok = issues.isEmpty()
        Log.d(TAG, "result: ${if (ok) "OK" else "POOR ${issues.map { it.kind }}"}")
        return ScanQualityResult(isAcceptable = ok, issues = issues)
    }

    private fun averageLuma(bitmap: Bitmap): Double {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return pixels.sumOf { luma(it) } / pixels.size
    }

    private fun laplacianVariance(bitmap: Bitmap): Double {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3) return Double.MAX_VALUE
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var sumSq = 0.0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val lap = luma(pixels[(y - 1) * w + x]) +
                    luma(pixels[(y + 1) * w + x]) +
                    luma(pixels[y * w + (x - 1)]) +
                    luma(pixels[y * w + (x + 1)]) -
                    4.0 * luma(pixels[y * w + x])
                sumSq += lap * lap
            }
        }
        return sumSq / ((w - 2) * (h - 2))
    }

    private fun contentAreaRatio(bitmap: Bitmap, averageLuma: Double): Double? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 8 || h < 8) return null

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var maxX = -1
        var minY = h
        var maxY = -1
        var contentPixels = 0

        for (y in 1 until h - 1 step 2) {
            for (x in 1 until w - 1 step 2) {
                val center = luma(pixels[y * w + x])
                val horizontal = abs(luma(pixels[y * w + x - 1]) - luma(pixels[y * w + x + 1]))
                val vertical = abs(luma(pixels[(y - 1) * w + x]) - luma(pixels[(y + 1) * w + x]))
                val darkInk = averageLuma > 80.0 && center < averageLuma - 48.0
                if (horizontal + vertical > 30.0 || darkInk) {
                    contentPixels++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (contentPixels < 40 || maxX < minX || maxY < minY) return null
        val contentArea = (maxX - minX + 1) * (maxY - minY + 1)
        return contentArea.toDouble() / (w * h).toDouble()
    }

    private fun luma(pixel: Int): Double {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299 * r + 0.587 * g + 0.114 * b
    }
}
