package com.heartbeatmusic.terminal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable

/**
 * Draws a subtle geometric grid overlay for Sync Engine.
 */
class GridOverlayDrawable : Drawable() {
    private val paint = Paint().apply {
        color = Color.argb(25, 0, 255, 255)
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val spacing = 48f

        var x = 0f
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h, paint)
            x += spacing
        }

        var y = 0f
        while (y <= h) {
            canvas.drawLine(0f, y, w, y, paint)
            y += spacing
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
