package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.firebase.AiSearchResult
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.i18n.StringsRepository
import com.example.ui.components.ProductVisualArtwork
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PriceTextStyle
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiChatUiState
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max

@Composable
fun AiAssistantScreen(
    language: AppLanguage,
    strings: LocalizedStrings,
    visualSearchState: UiState<AiSearchResult>?,
    voiceIntentState: UiState<AiSearchResult>?,
    chatMessages: List<AiChatMessage>,
    chatState: AiChatUiState,
    onAnalyzeImage: (Bitmap) -> Unit,
    onAnalyzeVoiceIntent: (String) -> Unit,
    onAnalyzeAudio: (ByteArray) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onQuickAddProduct: (Product) -> Unit,
    onAddBundleToCart: (List<Product>) -> Unit,
    onProductClick: (Product) -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var voiceNoticeMessage by remember { mutableStateOf<String?>(null) }
    var voiceQueryInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var peakAmplitude by remember { mutableIntStateOf(0) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeAudioFile by remember { mutableStateOf<File?>(null) }

    fun releaseRecorderSafely() {
        try {
            activeRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            activeRecorder?.release()
        } catch (_: Exception) {
        }
        activeRecorder = null
        isRecordingAudio = false
    }

    fun stopAndAnalyzeRecording() {
        val file = activeAudioFile
        val recordedPeak = peakAmplitude
        releaseRecorderSafely()
        if (file == null || !file.exists() || file.length() < 800L || recordedPeak < 80) {
            voiceNoticeMessage = strings.aiMicNoAudioMsg
            try {
                file?.delete()
            } catch (_: Exception) {
            }
            return
        }
        val bytes = try {
            file.readBytes()
        } catch (_: Exception) {
            ByteArray(0)
        }
        try {
            file.delete()
        } catch (_: Exception) {
        }
        if (bytes.size < 800) {
            voiceNoticeMessage = strings.aiMicNoAudioMsg
        } else {
            voiceNoticeMessage = null
            onAnalyzeAudio(bytes)
        }
    }

    fun startInAppRecording() {
        voiceNoticeMessage = null
        permissionMessage = null
        val outFile = File(context.cacheDir, "tezbazar_voice_${System.currentTimeMillis()}.m4a")
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(16_000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            activeAudioFile = outFile
            activeRecorder = recorder
            peakAmplitude = 0
            recordingSeconds = 0
            isRecordingAudio = true
        } catch (_: Exception) {
            try {
                outFile.delete()
            } catch (_: Exception) {
            }
            releaseRecorderSafely()
            voiceNoticeMessage = strings.aiMicNoAudioMsg
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseRecorderSafely()
        }
    }

    // Track recording duration & peak microphone amplitude while recording
    LaunchedEffect(isRecordingAudio) {
        var ticks = 0
        while (isRecordingAudio) {
            delay(200L)
            ticks++
            recordingSeconds = ticks / 5
            val amp = try {
                activeRecorder?.maxAmplitude ?: 0
            } catch (_: Exception) {
                0
            }
            peakAmplitude = max(peakAmplitude, amp)
            if (ticks >= 60) { // Auto-stop at 12s
                stopAndAnalyzeRecording()
            }
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            permissionMessage = null
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                previewBitmap = bmp
                onAnalyzeImage(bmp)
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            permissionMessage = null
            previewBitmap = bmp
            onAnalyzeImage(bmp)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            permissionMessage = strings.cameraPermissionDenied
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = null
            startInAppRecording()
        } else {
            permissionMessage = strings.micPermissionDenied
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BukharaGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = BukharaGold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.aiHubTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = strings.aiHubSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (permissionMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = permissionMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 1. Visual Image Search Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ai_chef_bundle),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.aiVisualCardTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.aiVisualCardDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_pick_photo_btn")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiPickPhotoBtn)
                        }

                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    takePictureLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_take_photo_btn")
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiTakePhotoBtn)
                        }
                    }

                    previewBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    if (visualSearchState != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AiSearchResultBox(
                            state = visualSearchState,
                            language = language,
                            strings = strings,
                            onAddBundle = onAddBundleToCart,
                            onQuickAddProduct = onQuickAddProduct,
                            onProductClick = onProductClick,
                            onRetry = previewBitmap?.let { bmp -> { onAnalyzeImage(bmp) } }
                        )
                    }
                }
            }
        }

        // 2. Voice & Natural Intent Smart Basket Card (In-app Gemini Audio Recorder + Text Analyze)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.aiVoiceCardTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = strings.aiVoiceCardDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = voiceQueryInput,
                            onValueChange = {
                                voiceQueryInput = it
                                if (voiceNoticeMessage != null) voiceNoticeMessage = null
                            },
                            placeholder = { Text(strings.aiQuickPrompts.first(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (voiceQueryInput.isNotBlank()) {
                                        voiceNoticeMessage = null
                                        onAnalyzeVoiceIntent(voiceQueryInput)
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_voice_text_input")
                        )
                        Button(
                            onClick = {
                                if (isRecordingAudio) {
                                    stopAndAnalyzeRecording()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        startInAppRecording()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingAudio) Color(0xFFDC2626) else BukharaGold
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("ai_voice_mic_btn")
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = if (isRecordingAudio) strings.aiRecordingStopBtn else strings.aiVoiceBtn,
                                tint = Color.White
                            )
                        }
                    }

                    if (isRecordingAudio) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color(0xFFDC2626))
                                    Text(
                                        text = "${strings.aiListeningLabel} (0:${String.format("%02d", recordingSeconds)})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Button(
                                    onClick = { stopAndAnalyzeRecording() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("ai_stop_recording_btn")
                                ) {
                                    Text(strings.aiRecordingStopBtn, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }

                    if (voiceNoticeMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = voiceNoticeMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            voiceNoticeMessage = null
                            val target = voiceQueryInput.ifBlank { strings.aiQuickPrompts.first() }
                            if (voiceQueryInput.isBlank()) voiceQueryInput = target
                            onAnalyzeVoiceIntent(target)
                        },
                        enabled = voiceIntentState !is UiState.Loading && !isRecordingAudio,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_analyze_intent_btn")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.aiAnalyzeBtn, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(strings.aiQuickPrompts) { prompt ->
                            SuggestionChip(
                                onClick = {
                                    voiceNoticeMessage = null
                                    voiceQueryInput = prompt
                                    onAnalyzeVoiceIntent(prompt)
                                },
                                label = { Text(prompt, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    if (voiceIntentState != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AiSearchResultBox(
                            state = voiceIntentState,
                            language = language,
                            strings = strings,
                            onAddBundle = onAddBundleToCart,
                            onQuickAddProduct = onQuickAddProduct,
                            onProductClick = onProductClick,
                            onRetry = {
                                val target = voiceQueryInput.ifBlank { strings.aiQuickPrompts.first() }
                                onAnalyzeVoiceIntent(target)
                            }
                        )
                    }
                }
            }
        }

        // 3. Multi-turn Streaming AI Shopping Concierge with Product Action Cards
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.aiChatTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (chatMessages.isNotEmpty() && chatState !is AiChatUiState.Streaming) {
                            TextButton(
                                onClick = onClearChat,
                                modifier = Modifier.testTag("ai_clear_chat_btn")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.aiClearChatBtn)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (chatMessages.isEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(strings.aiQuickPrompts) { starter ->
                                SuggestionChip(
                                    onClick = { onSendChatMessage(starter) },
                                    label = { Text(starter, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    chatMessages.forEach { msg ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (msg.isUser) EmeraldDeep else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(0.88f)
                                ) {
                                    Text(
                                        text = msg.text.ifBlank { "..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            if (!msg.isUser && msg.suggestedProducts.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                ) {
                                    items(msg.suggestedProducts, key = { "${msg.id}_${it.id}" }) { p ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .width(156.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onProductClick(p) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ProductVisualArtwork(
                                                    imageKey = p.imageKey,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    iconSizeDp = 20
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = p.localizedTitle(language),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = StringsRepository.formatUzs(p.priceUzs, language),
                                                        style = PriceTextStyle.copy(fontSize = 11.sp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onQuickAddProduct(p) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Add,
                                                        contentDescription = strings.addBtn,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (chatState is AiChatUiState.Error) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chatState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text(strings.aiChatPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (chatInput.isNotBlank() && chatState !is AiChatUiState.Streaming) {
                                        onSendChatMessage(chatInput)
                                        chatInput = ""
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input")
                        )
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendChatMessage(chatInput)
                                    chatInput = ""
                                }
                            },
                            enabled = chatState !is AiChatUiState.Streaming,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("ai_chat_send_btn")
                        ) {
                            if (chatState is AiChatUiState.Streaming) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = strings.aiSendBtn,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSearchResultBox(
    state: UiState<AiSearchResult>,
    language: AppLanguage,
    strings: LocalizedStrings,
    onAddBundle: (List<Product>) -> Unit,
    onQuickAddProduct: (Product) -> Unit,
    onProductClick: (Product) -> Unit,
    onRetry: (() -> Unit)? = null
) {
    when (state) {
        is UiState.Loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(strings.aiAnalyzingLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
        is UiState.Error -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (onRetry != null) {
                        TextButton(onClick = onRetry) {
                            Text(strings.aiRetryBtn)
                        }
                    }
                }
            }
        }
        is UiState.Success -> {
            val result = state.data
            Column {
                Text(
                    text = result.detectedConcept,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (result.explanation.isNotBlank()) {
                    Text(
                        text = result.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.products.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(result.products, key = { "res_${it.id}" }) { p ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { onProductClick(p) },
                                modifier = Modifier.width(136.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box {
                                        ProductVisualArtwork(
                                            imageKey = p.imageKey,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            iconSizeDp = 24
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .clickable { onQuickAddProduct(p) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Add, contentDescription = strings.addBtn, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = p.localizedTitle(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = StringsRepository.formatUzs(p.priceUzs, language),
                                        style = PriceTextStyle.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onAddBundle(result.products) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.aiAddBundleBtn)
                    }
                }
            }
        }
    }
}
