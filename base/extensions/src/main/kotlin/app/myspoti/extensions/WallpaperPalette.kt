package app.myspoti.extensions

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The colours of a theme made from a wallpaper ("Match the wallpaper"): the wallpaper's main hue
 * gives the accent (vivid) and the page, cards, text and muted text (the same hue, in the dark
 * tones the app needs: see [ThemeSpec]). From a sample of the wallpaper's pixels, so a picture
 * and a frame of a video are treated the same. A grey or black-and-white wallpaper gives neutral
 * dark colours and no accent (the accent stays what it was).
 */
object WallpaperPalette {
    /** The keys are [ThemeSpec.COLORS]; the accent is missing for a wallpaper with no colour. Null: no pixels to look at. */
    fun derive(pixels: IntArray): Map<String, Int>? {
        if (pixels.isEmpty()) return null
        val weight = DoubleArray(BUCKETS)
        val sumR = DoubleArray(BUCKETS)
        val sumG = DoubleArray(BUCKETS)
        val sumB = DoubleArray(BUCKETS)
        var seen = 0
        for (p in pixels) {
            if ((p ushr 24) < 0x80) continue
            seen++
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            if (mx == 0) continue
            val v = mx / 255.0
            val s = (mx - mn).toDouble() / mx
            if (s < MIN_SATURATION || v < MIN_VALUE) continue
            val w = s * v
            val bucket = (hue(r, g, b) / (360.0 / BUCKETS)).toInt().coerceIn(0, BUCKETS - 1)
            weight[bucket] += w; sumR[bucket] += r * w; sumG[bucket] += g * w; sumB[bucket] += b * w
        }
        if (seen == 0) return null
        // The strongest hue, counting its neighbours (a hue sits on a bucket edge as easily as in the middle).
        var best = -1
        var bestScore = 0.0
        for (i in 0 until BUCKETS) {
            val score = weight[i] + 0.5 * weight[(i + 1) % BUCKETS] + 0.5 * weight[(i + BUCKETS - 1) % BUCKETS]
            if (score > bestScore) { bestScore = score; best = i }
        }
        if (best < 0 || bestScore < MIN_COLOURFUL * seen) return neutral()
        val w = weight[best]
        val r = (sumR[best] / w).toInt()
        val g = (sumG[best] / w).toInt()
        val b = (sumB[best] / w).toInt()
        val (h, s, _) = hsl(r, g, b)
        return mapOf(
            ThemeSpec.ACCENT to argb(h, (s * 1.4).coerceIn(0.62, 0.92), 0.64),
            ThemeSpec.BACKGROUND to argb(h, 0.35, 0.07),
            ThemeSpec.SURFACE to argb(h, 0.28, 0.14),
            ThemeSpec.TEXT to argb(h, 0.30, 0.95),
            ThemeSpec.MUTED to argb(h, 0.18, 0.70),
        )
    }

    private fun neutral() = mapOf(
        ThemeSpec.BACKGROUND to argb(0.0, 0.0, 0.07),
        ThemeSpec.SURFACE to argb(0.0, 0.0, 0.14),
        ThemeSpec.TEXT to argb(0.0, 0.0, 0.95),
        ThemeSpec.MUTED to argb(0.0, 0.0, 0.70),
    )

    private const val BUCKETS = 36
    private const val MIN_SATURATION = 0.18
    private const val MIN_VALUE = 0.15
    /** Less coloured than this share of the sample: the wallpaper has no colour to follow. */
    private const val MIN_COLOURFUL = 0.02

    private fun hue(r: Int, g: Int, b: Int): Double = hsl(r, g, b).first

    private fun hsl(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        val rf = r / 255.0; val gf = g / 255.0; val bf = b / 255.0
        val mx = max(rf, max(gf, bf)); val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2
        val d = mx - mn
        if (d == 0.0) return Triple(0.0, 0.0, l)
        val s = d / (1 - abs(2 * l - 1))
        val h = when (mx) {
            rf -> ((gf - bf) / d) % 6
            gf -> (bf - rf) / d + 2
            else -> (rf - gf) / d + 4
        } * 60
        return Triple(if (h < 0) h + 360 else h, s.coerceIn(0.0, 1.0), l)
    }

    private fun argb(h: Double, s: Double, l: Double): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when ((h / 60).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        fun ch(v: Double) = ((v + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
