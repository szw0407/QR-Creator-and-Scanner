package com.example.qrscannercreator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.qrscannercreator.ui.theme.QRScannerCreatorTheme
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.*
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRScannerCreatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRCodeApp()
                }
            }
        }
    }
}



@Composable
fun QRCodeApp() {
    var inputText by remember { mutableStateOf("Hello QR Code!") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var decodedText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "需要相机权限来扫描二维码", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 二维码扫描启动器
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            decodedText = result.contents
            inputText = result.contents
            Toast.makeText(context, "扫码成功！内容已识别", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "扫码取消或失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 图片选择启动器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // 新增：从图片解码二维码
            val resolver = context.contentResolver
            try {
                val inputStream = resolver.openInputStream(uri)
                val bitmap = inputStream?.use { android.graphics.BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    val result = decodeQRCode(bitmap)
                    if (result != null) {
                        decodedText = result
                        inputText = result
                        Toast.makeText(context, "二维码识别成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "未识别到二维码", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "解码出错: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "QR 码生成与扫描",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        // 输入文本区域
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("二维码的内容") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLines = 3
        )
        
        // 生成二维码按钮
        Button(
            onClick = {
                qrBitmap = generateQRCode(inputText)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("生成二维码")
        }
        
        // 显示生成的二维码
        qrBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.size(250.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "生成的二维码",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        HorizontalDivider()
        
        // 扫描功能区域
        Text(
            text = "扫描二维码",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            val options = ScanOptions().apply {
                                setPrompt("旋转设备切换扫描模式")
                                setBeepEnabled(true)
                                setOrientationLocked(false) // 允许屏幕自由旋转
                                setCameraId(0) // 使用后置摄像头
                                setCaptureActivity(CustomCaptureActivity::class.java) // 使用自定义扫描Activity
                            }
                            scanLauncher.launch(options)
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("相机扫描")
            }
            
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("从图片")
            }
        }
        
        // 显示解码结果
        if (decodedText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "解码结果:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = decodedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 检查是否是WiFi二维码
                    if (decodedText.startsWith("WIFI:")) {
                        Button(
                            onClick = {
                                // 暂时显示提示，explain函数功能待实现
                                Toast.makeText(context, "WiFi连接功能待实现", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("连接到WiFi (功能待实现)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 生成二维码的函数
 */
fun generateQRCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.CHARACTER_SET to "utf-8",
            EncodeHintType.MARGIN to 2
        )
        
        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 250, 250, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        
        for (i in 0 until width) {
            for (j in 0 until height) {
                pixels[i * width + j] = if (bitMatrix[i, j]) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
            }
        }
        
        createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

/**
 * 从bitmap解码二维码的函数
 */
fun decodeQRCode(bitmap: Bitmap): String? {
    return try {
        val hints = hashMapOf<DecodeHintType, Any>(
            DecodeHintType.CHARACTER_SET to "utf-8"
        )
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(binaryBitmap, hints)
        
        result.text
    } catch (e: NotFoundException) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun QRCodeAppPreview() {
    QRScannerCreatorTheme {
        QRCodeApp()
    }
}