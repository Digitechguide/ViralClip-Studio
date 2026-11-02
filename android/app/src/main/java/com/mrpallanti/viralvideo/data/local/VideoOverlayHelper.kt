package com.mrpallanti.viralvideo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mrpallanti.viralvideo.domain.model.OverlayConfig
import com.mrpallanti.viralvideo.domain.model.TextOverlayPosition
import com.mrpallanti.viralvideo.domain.model.WatermarkPosition

object VideoOverlayHelper {

    fun createOverlayBitmap(
        context: Context,
        width: Int = 1080,
        height: Int = 1920,
        hookText: String,
        overlayConfig: OverlayConfig
    ): Bitmap? {
        val effectiveText = overlayConfig.customText.ifBlank { hookText }.trim()
        val hasLogo = !overlayConfig.logoUri.isNullOrBlank()
        val hasWatermark = overlayConfig.watermarkText.isNotBlank()

        if (effectiveText.isBlank() && !hasLogo && !hasWatermark) {
            return null
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Watermark (Logo Image or Channel Watermark Text)
        if (hasLogo && overlayConfig.logoUri != null) {
            drawLogoWatermark(context, canvas, width, height, overlayConfig.logoUri, overlayConfig.watermarkPosition, overlayConfig.watermarkOpacity)
        } else if (hasWatermark) {
            drawTextWatermark(canvas, width, height, overlayConfig.watermarkText, overlayConfig.watermarkPosition, overlayConfig.watermarkOpacity)
        }

        // 2. Draw Hook Caption / Overlay Text
        if (effectiveText.isNotBlank()) {
            drawStyledCaption(canvas, width, height, effectiveText, overlayConfig)
        }

        return bitmap
    }

    private fun drawLogoWatermark(
        context: Context,
        canvas: Canvas,
        width: Int,
        height: Int,
        logoUriStr: String,
        position: WatermarkPosition,
        opacity: Float
    ) {
        try {
            val uri = Uri.parse(logoUriStr)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return

            val maxLogoWidth = 180f
            val scale = maxLogoWidth / original.width.toFloat()
            val logoW = maxLogoWidth
            val logoH = original.height * scale

            val marginHorizontal = 48f
            val marginVertical = 120f // Below/above typical platform UI overlays

            val x = when (position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> marginHorizontal
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> width - logoW - marginHorizontal
            }

            val y = when (position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> marginVertical
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - logoH - marginVertical
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (opacity.coerceIn(0.1f, 1.0f) * 255).toInt()
            }

            val dstRect = RectF(x, y, x + logoW, y + logoH)
            canvas.drawBitmap(original, null, dstRect, paint)
            original.recycle()
        } catch (ignored: Exception) {}
    }

    private fun drawTextWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        position: WatermarkPosition,
        opacity: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            alpha = (opacity.coerceIn(0.1f, 1.0f) * 255).toInt()
            setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0))
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((opacity.coerceIn(0.1f, 1.0f) * 160).toInt(), 15, 23, 42)
        }

        val textW = paint.measureText(text)
        val pillPaddingH = 24f
        val pillPaddingV = 14f
        val pillW = textW + (pillPaddingH * 2)
        val pillH = 34f + (pillPaddingV * 2)

        val marginH = 48f
        val marginV = 120f

        val x = when (position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> marginH
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> width - pillW - marginH
        }

        val y = when (position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> marginV
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - pillH - marginV
        }

        // Draw pill background
        val pillRect = RectF(x, y, x + pillW, y + pillH)
        canvas.drawRoundRect(pillRect, 20f, 20f, bgPaint)

        // Draw text
        canvas.drawText(text, x + pillPaddingH, y + pillH - pillPaddingV - 4f, paint)
    }

    private fun drawStyledCaption(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: OverlayConfig
    ) {
        val maxTextWidth = 900
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try { Color.parseColor(config.textColorHex) } catch (e: Exception) { Color.WHITE }
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 3f, 3f, Color.BLACK)
        }

        // StaticLayout for multi-line centered text wrapping
        @Suppress("DEPRECATION")
        val staticLayout = StaticLayout(
            text,
            textPaint,
            maxTextWidth,
            Layout.Alignment.ALIGN_CENTER,
            1.15f,
            0f,
            false
        )

        val layoutWidth = staticLayout.width
        val layoutHeight = staticLayout.height

        val paddingH = 36f
        val paddingV = 24f
        val totalBoxW = (layoutWidth + paddingH * 2).coerceAtMost(width - 80f)
        val totalBoxH = layoutHeight + paddingV * 2

        val boxX = (width - totalBoxW) / 2f
        val boxY = when (config.textPosition) {
            TextOverlayPosition.TOP -> 260f
            TextOverlayPosition.CENTER -> (height - totalBoxH) / 2f
            TextOverlayPosition.BOTTOM -> height - totalBoxH - 320f
        }

        // Background pill
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                Color.parseColor(config.textBgColorHex)
            } catch (e: Exception) {
                Color.argb(200, 10, 10, 15)
            }
        }

        val boxRect = RectF(boxX, boxY, boxX + totalBoxW, boxY + totalBoxH)
        canvas.drawRoundRect(boxRect, 28f, 28f, bgPaint)

        // Draw border accent (neon purple tint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 168, 85, 247)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(boxRect, 28f, 28f, borderPaint)

        // Draw static layout centered
        canvas.save()
        canvas.translate(width / 2f, boxY + paddingV)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}
