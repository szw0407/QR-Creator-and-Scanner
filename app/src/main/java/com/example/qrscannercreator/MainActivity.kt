package com.example.qrscannercreator

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.core.net.toUri

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



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeApp() {
    var inputText by remember { mutableStateOf("Hello QR Code!") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var decodedText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
      val context = LocalContext.current
    
    // 存储待连接的WiFi信息
    var pendingWifiInfo by remember { mutableStateOf<String?>(null) }
    
    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "需要相机权限来扫描二维码", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 位置权限请求 (用于WiFi连接)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "位置权限已授予，正在连接WiFi...", Toast.LENGTH_SHORT).show()
            // 权限授予后，连接待连接的WiFi
            pendingWifiInfo?.let { wifiQrCode ->
                connectToWifi(context, wifiQrCode)
                pendingWifiInfo = null // 清除待连接信息
            }
        } else {
            Toast.makeText(context, "位置权限被拒绝，无法自动连接WiFi", Toast.LENGTH_LONG).show()
            pendingWifiInfo = null // 清除待连接信息
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
                    
                    // 可点击和长按的解码结果文本
                    Text(
                        text = decodedText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    handleScanResultAction(context, decodedText)
                                },
                                onLongClick = {
                                    copyToClipboard(context, decodedText)
                                }
                            )
                            .padding(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 提示信息
                    Text(
                        text = "点击执行操作，长按复制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 根据内容类型显示特定按钮
                    when {                        decodedText.startsWith("WIFI:") -> {
                            Button(
                                onClick = {
                                    connectToWifiWithPermissionCheck(
                                        context, 
                                        decodedText, 
                                        locationPermissionLauncher
                                    ) { wifiQrCode ->
                                        pendingWifiInfo = wifiQrCode
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("连接到WiFi")
                            }
                        }
                        isUrl(decodedText) -> {
                            Button(
                                onClick = {
                                    openUrl(context, decodedText)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("打开链接")
                            }
                        }
                        isDomainLike(decodedText) -> {
                            Button(
                                onClick = {
                                    openUrl(context, "https://$decodedText")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("作为网址打开")
                            }
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

/**
 * 处理扫码结果的点击操作
 */
fun handleScanResultAction(context: Context, text: String) {
    when {
        text.startsWith("WIFI:") -> {
            connectToWifi(context, text)
        }
        isUrl(text) -> {
            openUrl(context, text)
        }
        isDomainLike(text) -> {
            openUrl(context, "https://$text")
        }
        else -> {
            // 普通文本，复制到剪贴板
            copyToClipboard(context, text)
        }
    }
}

/**
 * 判断是否为URL
 */
fun isUrl(text: String): Boolean {
    val urlPatterns = listOf(
        "^https?://.*",
        "^ftp://.*",
        "^mailto:.*",
        "^tel:.*",
        "^sms:.*",
        "^geo:.*",
        "^market://.*",
        "^intent://.*"
    )
    return urlPatterns.any { pattern ->
        text.matches(Regex(pattern, RegexOption.IGNORE_CASE))
    }
}

/**
 * 判断是否看起来像域名
 */
fun isDomainLike(text: String): Boolean {
    // 简单的域名模式检测
    val domainPattern = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}(/.*)?$"
    return text.matches(Regex(domainPattern, RegexOption.IGNORE_CASE)) && 
           !text.contains(" ") && 
           text.length < 200
}

/**
 * 打开URL
 */
fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
        Toast.makeText(context, "正在打开: $url", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "无法打开链接: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 复制到剪贴板
 */
fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("扫码结果", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 带权限检查的WiFi连接函数
 */
fun connectToWifiWithPermissionCheck(
    context: Context, 
    wifiQrCode: String,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    setPendingWifi: (String) -> Unit
) {
    try {
        // 解析WiFi二维码格式
        val wifiInfo = parseWifiQrCode(wifiQrCode)
        if (wifiInfo == null) {
            Toast.makeText(context, "WiFi二维码格式错误", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Android 10+需要位置权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasLocationPermission) {
                // 储存待连接的WiFi信息
                setPendingWifi(wifiQrCode)
                
                // 请求位置权限
                Toast.makeText(
                    context, 
                    "连接WiFi需要位置权限，正在请求权限...", 
                    Toast.LENGTH_SHORT
                ).show()
                
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        }
        
        // 有权限或Android 9-，直接连接
        connectToWifi(context, wifiQrCode)
        
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "WiFi连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 连接到WiFi
 */
fun connectToWifi(context: Context, wifiQrCode: String) {
    try {
        // 解析WiFi二维码格式: WIFI:T:WPA;S:networkname;P:password;H:false;
        val wifiInfo = parseWifiQrCode(wifiQrCode)
        if (wifiInfo != null) {
            // Android 10+ 使用新的WiFi连接方式
            requestNetworkConnection(
                context,
                wifiInfo
            );
        } else {
            Toast.makeText(context, "WiFi二维码格式错误", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "WiFi连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 解析WiFi二维码
 */
data class WifiInfo(
    val ssid: String,
    val password: String,
    val security: String,
    val hidden: Boolean = false
)

fun parseWifiQrCode(qrCode: String): WifiInfo? {
    try {
        if (!qrCode.startsWith("WIFI:")) return null
        
        val parts = qrCode.substring(5).split(";")
        var ssid = ""
        var password = ""
        var security = "WPA"
        var hidden = false
        
        for (part in parts) {
            when {
                part.startsWith("S:") -> ssid = part.substring(2)
                part.startsWith("P:") -> password = part.substring(2)
                part.startsWith("T:") -> security = part.substring(2)
                part.startsWith("H:") -> hidden = part.substring(2).equals("true", ignoreCase = true)
            }
        }
        
        return if (ssid.isNotEmpty()) {
            WifiInfo(ssid, password, security, hidden)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
/**
 * Android 11+ 使用NetworkRequest请求网络连接
 */
fun requestNetworkConnection(context: Context, wifiInfo: WifiInfo) {
    try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val networkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(wifiInfo.ssid)
            .apply {
                when (wifiInfo.security.uppercase()) {
                    "WPA", "WPA2", "WPA3" -> {
                        setWpa2Passphrase(wifiInfo.password)
                    }
                    "WEP" -> {
                        // WEP在新API中不推荐，降级为WPA2
                        setWpa2Passphrase(wifiInfo.password)
                    }
                    "NONE" -> {
                        // 开放网络
                    }
                }
                
                if (wifiInfo.hidden) {
                    setIsHiddenSsid(true)
                }
            }
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                // 网络连接成功
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "已连接到WiFi: ${wifiInfo.ssid}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                // 连接失败
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "连接WiFi失败，请检查密码或手动连接", Toast.LENGTH_LONG).show()
                }
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, 10000) // 10秒超时
        Toast.makeText(context, "正在尝试连接到: ${wifiInfo.ssid}...", Toast.LENGTH_SHORT).show()
        
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "网络请求失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
@Preview(showBackground = true)
@Composable
fun QRCodeAppPreview() {
    QRScannerCreatorTheme {
        QRCodeApp()
    }
}