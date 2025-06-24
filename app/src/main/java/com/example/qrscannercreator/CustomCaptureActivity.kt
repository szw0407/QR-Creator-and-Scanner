package com.example.qrscannercreator

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/**
 * 自定义扫描Activity
 * 横向时扫描条码，纵向时扫描QR码
 */
class CustomCaptureActivity : CaptureActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var statusView: TextView
    private lateinit var flashlightButton: Button
    private var isFlashlightOn = false

    // 扫码结果回调
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            // 处理扫码结果
            handleScanResult(result)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
            // 可选：处理可能的结果点（用于UI反馈）
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_capture)
        
        try {
            barcodeView = findViewById(R.id.zxing_barcode_scanner)
            statusView = findViewById(R.id.zxing_status_view)
            flashlightButton = findViewById(R.id.flashlight_button)
            
            // 设置闪光灯按钮点击事件
            setupFlashlightButton()
            
            // 根据当前方向设置扫描格式
            updateScanFormats()
            
            // 设置扫码回调
            barcodeView.decodeContinuous(callback)
            
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果初始化失败，关闭Activity
            finish()
        }
    }
      override fun onResume() {
        super.onResume()
        // 确保摄像头正确启动和扫码回调设置
        try {
            barcodeView.resume()
            // 重新设置扫码回调（确保在resume后正确工作）
            barcodeView.decodeContinuous(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停时关闭闪光灯和摄像头
        try {
            if (isFlashlightOn) {
                barcodeView.setTorchOff()
                isFlashlightOn = false
                updateFlashlightButtonUI()
            }
            barcodeView.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // 屏幕方向改变时更新扫描格式
        try {
            updateScanFormats()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }    private fun updateScanFormats() {
        val currentOrientation = resources.configuration.orientation
        
        // 所有支持的条码格式
        val allFormats = listOf(
            // 一维条码
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODABAR,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.ITF,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            // 二维码
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.AZTEC,
            BarcodeFormat.PDF_417,
            BarcodeFormat.MAXICODE
        )
        
        val (formats, statusText) = when (currentOrientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // 横向：优先条码，但支持所有格式
                Pair(allFormats, "横向扫描 - 全格式支持 (优化条码)")
            }
            else -> {
                // 纵向：优先二维码，但支持所有格式
                Pair(allFormats, "纵向扫描 - 全格式支持 (优化二维码)")
            }
        }
          // 更新状态文字
        statusView.text = statusText
        
        // 创建解码器工厂
        val decoderFactory = DefaultDecoderFactory(formats)
        
        // 安全地设置解码器工厂
        try {
            barcodeView.barcodeView.decoderFactory = decoderFactory
            // 不在这里调用 resume，由生命周期管理
        } catch (e: Exception) {
            // 如果设置失败，使用默认设置
            e.printStackTrace()
        }
    }
    
    private fun setupFlashlightButton() {
        flashlightButton.setOnClickListener {
            toggleFlashlight()
        }
        
        // 初始化按钮状态
        updateFlashlightButtonUI()
    }
    
    private fun toggleFlashlight() {
        try {
            if (isFlashlightOn) {
                barcodeView.setTorchOff()
                isFlashlightOn = false
            } else {
                barcodeView.setTorchOn()
                isFlashlightOn = true
            }
            updateFlashlightButtonUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
      private fun updateFlashlightButtonUI() {
        if (isFlashlightOn) {
            flashlightButton.text = "🔦" // 开启状态
            flashlightButton.alpha = 1.0f
        } else {
            flashlightButton.text = "💡" // 关闭状态
            flashlightButton.alpha = 0.7f
        }
    }    /**
     * 处理扫码结果
     */
    private fun handleScanResult(result: BarcodeResult) {
        try {
            val scannedText = result.text
            val format = result.barcodeFormat.toString()
            
            // 延迟一点时间让用户看到反馈，然后返回结果
            Handler(Looper.getMainLooper()).postDelayed({
                // 创建返回的Intent
                val returnIntent = Intent().apply {
                    putExtra("SCAN_RESULT", scannedText)
                    putExtra("SCAN_RESULT_FORMAT", format)
                }
                
                // 设置结果并关闭Activity
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }, 500) // 延迟500毫秒
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "扫码处理失败", Toast.LENGTH_SHORT).show()
            // 如果处理失败，继续扫描
            resumeScanning()
        }
    }

    /**
     * 播放提示音
     */
    private fun playBeepSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            
            // 延迟释放资源
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, 300)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果播放提示音失败，不影响扫码功能
        }
    }

    /**
     * 恢复扫描（用于错误恢复）
     */
    private fun resumeScanning() {
        try {
            barcodeView.resume()
            statusView.text = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> "横向扫描 - 全格式支持 (优化条码)"
                else -> "纵向扫描 - 全格式支持 (优化二维码)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
