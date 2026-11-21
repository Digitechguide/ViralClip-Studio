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
import com.mrpallanti.viralvideo.domain.model.EditorialStyle
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
        val style = config.editorialStyle

        val textColor = when (style) {
            EditorialStyle.STREET -> Color.parseColor("#0A0A0A")
            EditorialStyle.CINEMA -> Color.parseColor("#FDE047")
            EditorialStyle.EDITORIAL -> Color.parseColor("#F8FAFC")
            EditorialStyle.NEWS -> Color.WHITE
            EditorialStyle.CLASSIC -> try { Color.parseColor(config.textColorHex) } catch (e: Exception) { Color.WHITE }
        }

        val typeface = when (style) {
            EditorialStyle.EDITORIAL -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            EditorialStyle.STREET -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = if (style == EditorialStyle.STREET) 54f else 50f
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            if (style != EditorialStyle.STREET) {
                setShadowLayer(10f, 3f, 3f, Color.BLACK)
            }
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

        val paddingH = if (style == EditorialStyle.STREET) 42f else 36f
        val paddingV = if (style == EditorialStyle.NEWS) 32f else 24f
        val totalBoxW = (layoutWidth + paddingH * 2).coerceAtMost(width - 80f)
        val totalBoxH = layoutHeight + paddingV * 2

        val boxX = (width - totalBoxW) / 2f
        val boxY = when (config.textPosition) {
            TextOverlayPosition.TOP -> 260f
            TextOverlayPosition.CENTER -> (height - totalBoxH) / 2f
            TextOverlayPosition.BOTTOM -> height - totalBoxH - 320f
        }

        // 1. Optional CINEMA Letterbox bars
        if (style == EditorialStyle.CINEMA) {
            val letterboxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 120f, letterboxPaint)
            canvas.drawRect(0f, height - 120f, width.toFloat(), height.toFloat(), letterboxPaint)
        }

        // 2. Background pill styling
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (style) {
                EditorialStyle.STREET -> Color.parseColor("#FFE600")
                EditorialStyle.EDITORIAL -> Color.argb(225, 15, 23, 42)
                EditorialStyle.NEWS -> Color.argb(235, 11, 15, 25)
                EditorialStyle.CINEMA -> Color.argb(190, 0, 0, 0)
                EditorialStyle.CLASSIC -> try {
                    Color.parseColor(config.textBgColorHex)
                } catch (e: Exception) {
                    Color.argb(200, 10, 10, 15)
                }
            }
        }

        val cornerRadius = when (style) {
            EditorialStyle.STREET -> 8f
            EditorialStyle.EDITORIAL -> 16f
            EditorialStyle.NEWS -> 12f
            else -> 28f
        }

        val boxRect = RectF(boxX, boxY, boxX + totalBoxW, boxY + totalBoxH)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, bgPaint)

        // 3. Border styling
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            when (style) {
                EditorialStyle.STREET -> {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                EditorialStyle.EDITORIAL -> {
                    color = Color.argb(160, 241, 245, 249)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                EditorialStyle.NEWS -> {
                    color = Color.parseColor("#EF4444")
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                EditorialStyle.CINEMA -> {
                    color = Color.argb(100, 253, 224, 71)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                EditorialStyle.CLASSIC -> {
                    color = Color.argb(120, 168, 85, 247)
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
            }
        }
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)

        // 4. Special NEWS badge header ("🚨 BREAKING NEWS")
        if (style == EditorialStyle.NEWS) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DC2626")
            }
            val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val badgeW = 240f
            val badgeH = 42f
            val badgeX = boxX + 24f
            val badgeY = boxY - (badgeH / 2f)
            val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
            canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)
            canvas.drawText("🚨 BREAKING", badgeX + (badgeW / 2f), badgeY + 28f, badgeTextPaint)
        }

        // Draw static layout centered
        canvas.save()
        canvas.translate(width / 2f, boxY + paddingV)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}
