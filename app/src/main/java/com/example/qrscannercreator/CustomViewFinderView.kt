package com.example.qrscannercreator

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

/**
 * 自定义扫描覆盖层
 * 根据屏幕方向显示不同形状的扫描区域
 */
class CustomViewFinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val backgroundPaint = Paint().apply {
        color = "#60000000".toColorInt()
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width
        val height = height
        val orientation = resources.configuration.orientation
        
        // 绘制半透明背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // 横向：绘制长条形扫描区域（适合条码）
                drawBarcodeFrame(canvas, width, height)
            }
            else -> {
                // 纵向：绘制方形扫描区域（适合QR码）
                drawQRCodeFrame(canvas, width, height)
            }
        }
    }
    
    private fun drawBarcodeFrame(canvas: Canvas, width: Int, height: Int) {
        // 条码扫描区域：宽度较大，高度较小
        val frameWidth = (width * 0.8f).toInt()
        val frameHeight = (height * 0.3f).toInt()
        
        val left = (width - frameWidth) / 2f
        val top = (height - frameHeight) / 2f
        val right = left + frameWidth
        val bottom = top + frameHeight
        
        // 清除扫描区域的背景
        canvas.drawRect(left, top, right, bottom, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
        
        // 绘制扫描框
        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, paint)
        
        // 绘制四个角的装饰
        drawCorners(canvas, left, top, right, bottom, 30f)
        
        // 绘制提示文字
        val text = "将条码放在扫描框内"
        canvas.drawText(text, width / 2f, bottom + 80f, textPaint)
    }
    
    private fun drawQRCodeFrame(canvas: Canvas, width: Int, height: Int) {
        // QR码扫描区域：正方形
        val frameSize = (minOf(width, height) * 0.7f).toInt()
        
        val left = (width - frameSize) / 2f
        val top = (height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize
        
        // 清除扫描区域的背景
        canvas.drawRect(left, top, right, bottom, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
        
        // 绘制扫描框
        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, paint)
        
        // 绘制四个角的装饰
        drawCorners(canvas, left, top, right, bottom, 40f)
        
        // 绘制提示文字
        val text = "将二维码放在扫描框内"
        canvas.drawText(text, width / 2f, bottom + 80f, textPaint)
    }
    
    private fun drawCorners(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, cornerLength: Float) {
        val cornerPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        
        // 左上角
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint)
        canvas.drawLine(left, top, left, top + cornerLength, cornerPaint)
        
        // 右上角
        canvas.drawLine(right - cornerLength, top, right, top, cornerPaint)
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint)
        
        // 左下角
        canvas.drawLine(left, bottom - cornerLength, left, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint)
        
        // 右下角
        canvas.drawLine(right - cornerLength, bottom, right, bottom, cornerPaint)
        canvas.drawLine(right, bottom - cornerLength, right, bottom, cornerPaint)
    }
}
