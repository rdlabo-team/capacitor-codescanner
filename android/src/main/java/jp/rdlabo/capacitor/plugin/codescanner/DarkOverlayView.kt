package jp.rdlabo.capacitor.plugin.codescanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class DarkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = "#40000000".toColorInt() // より透明な黒
        isAntiAlias = true
    }

    private val transparentPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var detectionRect: RectF? = null
    private var cornerRadius = 16f

    fun setDetectionArea(left: Int, top: Int, right: Int, bottom: Int) {
        // Safe-areaを考慮せず、そのままの座標を使用
        detectionRect = RectF(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat()
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 全体を暗く塗る
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // 検出エリア部分を透明にする
        detectionRect?.let { rect ->
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, transparentPaint)
        }
    }
}
