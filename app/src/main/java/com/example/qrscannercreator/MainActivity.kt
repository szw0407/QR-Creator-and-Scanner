package com.example.qrscannercreator

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import java.io.OutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
import java.text.SimpleDateFormat
import java.util.Locale

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
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var errorCorrectionLevel by remember { mutableStateOf(ErrorCorrectionLevel.M) }
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
    
    // Logo图片选择启动器
    val logoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedLogoUri = uri
            Toast.makeText(context, "Logo已选择", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 保存文件启动器
    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null && qrBitmap != null) {
            saveQRCodeToUri(context, qrBitmap!!, uri)
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
        
        // 错误纠正级别选择
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "错误纠正级别",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf(
                        ErrorCorrectionLevel.L to "L (7%)",
                        ErrorCorrectionLevel.M to "M (15%)",
                        ErrorCorrectionLevel.Q to "Q (25%)",
                        ErrorCorrectionLevel.H to "H (30%)"
                    )
                    
                    levels.forEach { (level, label) ->
                        FilterChip(
                            onClick = { errorCorrectionLevel = level },
                            label = { Text(label) },
                            selected = errorCorrectionLevel == level,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "级别越高，二维码越复杂但容错能力越强",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Logo选择区域
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "二维码Logo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (selectedLogoUri != null) {
                        IconButton(
                            onClick = { selectedLogoUri = null }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "清除Logo")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                  if (selectedLogoUri != null) {
                    // 显示选中的Logo预览
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp, 
                                MaterialTheme.colorScheme.outline, 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { logoPickerLauncher.launch("image/*") }
                    ) {
                        // 使用AsyncImage来显示实际的Logo图片
                        val logoBitmap = remember(selectedLogoUri) {
                            loadBitmapFromUri(context, selectedLogoUri!!)
                        }
                        
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "选中的Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "加载中...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    // Logo选择按钮
                    OutlinedButton(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加Logo")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择Logo图片 (可选)")
                    }
                }
                  if (selectedLogoUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Logo将保持原始形状并居中放置，支持PNG透明背景",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
          // 生成和保存按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val logobitmap = selectedLogoUri?.let { uri ->
                        loadBitmapFromUri(context, uri)
                    }
                    qrBitmap = generateQRCodeWithLogo(inputText, logobitmap, errorCorrectionLevel)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("生成二维码")
            }
              Button(
                onClick = {
                    if (qrBitmap != null) {
                        val fileName = "QRCode_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
                        saveFileLauncher.launch(fileName)
                    } else {
                        Toast.makeText(context, "请先生成二维码", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = qrBitmap != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }
        
        // 快速保存到相册按钮
        if (qrBitmap != null) {
            Button(
                onClick = {
                    saveQRCodeToMediaStore(context, qrBitmap!!)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("快速保存到相册")
            }
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
 * 生成二维码的函数（支持Logo和错误纠正级别）
 */
fun generateQRCodeWithLogo(
    text: String, 
    logo: Bitmap? = null, 
    errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M
): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.CHARACTER_SET to "utf-8",
            EncodeHintType.MARGIN to 2
        )
        
        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400, hints)
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
        
        val qrBitmap = createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
        
        // 如果有Logo，添加到二维码中心
        if (logo != null) {
            addLogoToQRCode(qrBitmap, logo)
        } else {
            qrBitmap
        }
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

/**
 * 原有的生成二维码函数（保持兼容性）
 */
fun generateQRCode(text: String): Bitmap? {
    return generateQRCodeWithLogo(text)
}

/**
 * 将Logo添加到二维码中心
 */
fun addLogoToQRCode(qrBitmap: Bitmap, logo: Bitmap): Bitmap {
    val qrWidth = qrBitmap.width
    val qrHeight = qrBitmap.height
    
    // Logo大小约为二维码的1/5
    val logoSize = minOf(qrWidth, qrHeight) / 5
    
    // 调整Logo大小，保持原始纵横比
    val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
    
    // 创建新的Bitmap来组合二维码和Logo
    val combinedBitmap = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(combinedBitmap)
    
    // 计算Logo位置（居中）
    val logoX = (qrWidth - logoSize) / 2f
    val logoY = (qrHeight - logoSize) / 2f
    
    // 直接绘制Logo，支持PNG透明度
    canvas.drawBitmap(scaledLogo, logoX, logoY, null)
    
    return combinedBitmap
}

/**
 * 从Uri加载Bitmap
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 保存二维码到指定Uri
 */
fun saveQRCodeToUri(context: Context, bitmap: Bitmap, uri: Uri) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Toast.makeText(context, "二维码已保存", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 快速保存二维码到媒体库
 */
fun saveQRCodeToMediaStore(context: Context, bitmap: Bitmap) {
    try {
        val fileName = "QRCode_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCodes")
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let { imageUri ->
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Toast.makeText(context, "二维码已保存到相册/QRCodes文件夹", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
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