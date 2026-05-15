package cz.nicolsburg.boardflow.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

data class ScanQualityIssue(val kind: String, val detail: String)

data class ScanQualityResult(
    val isAcceptable: Boolean,
    val issues: List<ScanQualityIssue>
)

object ScanImageQualityAnalyzer {

    private const val TAG = "ScanQuality"
    private const val ANALYSIS_MAX_DIM = 256   // pixels for luma/blur sampling
    private const val LUMA_THRESHOLD   = 45.0  // avg luma below this → too dark
    private const val BLUR_THRESHOLD   = 80.0  // Laplacian variance below this → too blurry / too far
    private const val MIN_DIMENSION_PX = 640   // original short edge below this → too small

    fun analyze(file: File): ScanQualityResult {
        val issues = mutableListOf<ScanQualityIssue>()

        // 1. Read original dimensions without decoding pixels.
        val sizeOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, sizeOpts)
        val origW = sizeOpts.outWidth
        val origH = sizeOpts.outHeight
        val shortEdge = if (origW > 0 && origH > 0) minOf(origW, origH) else 0

        if (shortEdge in 1 until MIN_DIMENSION_PX) {
            issues += ScanQualityIssue("resolution", "Image too small: ${origW}x${origH}")
            Log.d(TAG, "resolution: ${origW}x${origH} < $MIN_DIMENSION_PX")
        }

        // 2. Decode at reduced size for pixel-level checks.
        val sample = if (shortEdge > 0) maxOf(1, shortEdge / ANALYSIS_MAX_DIM) else 1
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: run {
            Log.w(TAG, "Could not decode image for quality check")
            return ScanQualityResult(isAcceptable = true, issues = emptyList())
        }

        try {
            // 3. Brightness.
            val luma = averageLuma(bitmap)
            Log.d(TAG, "avg luma=${"%.1f".format(luma)}")
            if (luma < LUMA_THRESHOLD) {
                issues += ScanQualityIssue("brightness", "Too dark (luma=${"%.1f".format(luma)})")
            }

            // 4. Sharpness — Laplacian variance. Low value means blurry or subject too far away.
            val blurScore = laplacianVariance(bitmap)
            Log.d(TAG, "laplacian variance=${"%.1f".format(blurScore)}")
            if (blurScore < BLUR_THRESHOLD) {
                val kind = if (shortEdge >= MIN_DIMENSION_PX) "distance" else "blur"
                issues += ScanQualityIssue(kind, "Low sharpness (score=${"%.1f".format(blurScore)})")
            }
        } finally {
            bitmap.recycle()
        }

        val ok = issues.isEmpty()
        Log.d(TAG, "result: ${if (ok) "OK" else "POOR ${issues.map { it.kind }}"}")
        return ScanQualityResult(isAcceptable = ok, issues = issues)
    }

    private fun averageLuma(bitmap: Bitmap): Double {
        val w = bitmap.width; val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return pixels.sumOf { luma(it) } / pixels.size
    }

    /** Mean squared Laplacian — standard single-pass blur score. */
    private fun laplacianVariance(bitmap: Bitmap): Double {
        val w = bitmap.width; val h = bitmap.height
        if (w < 3 || h < 3) return Double.MAX_VALUE
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var sumSq = 0.0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val lap = luma(pixels[(y - 1) * w + x]) + luma(pixels[(y + 1) * w + x]) +
                          luma(pixels[y * w + (x - 1)]) + luma(pixels[y * w + (x + 1)]) -
                          4.0 * luma(pixels[y * w + x])
                sumSq += lap * lap
            }
        }
        return sumSq / ((w - 2) * (h - 2))
    }

    private fun luma(pixel: Int): Double {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299 * r + 0.587 * g + 0.114 * b
    }
}
