package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioRecorderManager
import com.example.data.Category
import com.example.data.Note
import com.example.security.BiometricHelper
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FragmentActivity() {
    private lateinit var audioRecorderManager: AudioRecorderManager

    // List of customizable note background colors in hex code paired with a title
    private val noteColors = listOf(
        Pair("#FAF9F6", "Krem"),
        Pair("#FFF5F0", "Şeftali"),
        Pair("#EBFDF5", "Nane Taze"),
        Pair("#ECF8FF", "Bulut Mavi"),
        Pair("#F7F0FF", "Lavanta"),
        Pair("#FFFDF0", "Bal Sarısı"),
        Pair("#EBEBEB", "Metalik Gri")
    )

    // Icons available for dynamic category creation
    private val categoryIcons = mapOf(
        "Person" to Icons.Default.Person,
        "BusinessCenter" to Icons.Default.BusinessCenter,
        "Lightbulb" to Icons.Default.Lightbulb,
        "PriorityHigh" to Icons.Default.PriorityHigh,
        "Favorite" to Icons.Default.Favorite,
        "Book" to Icons.Default.Book,
        "Star" to Icons.Default.Star,
        "MusicNote" to Icons.Default.MusicNote,
        "Home" to Icons.Default.Home
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioRecorderManager = AudioRecorderManager(this)

        setContent {
            MyApplicationTheme {
                val viewModel: NoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                
                val isRegistered by viewModel.isRegistered.collectAsStateWithLifecycle()
                val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
                val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

                // Request microphone permission when starting recordings
                val context = LocalContext.current
                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val requestMicPermission = {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        200
                    )
                    hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = Pair(isRegistered, isAuthenticated),
                        label = "AuthNavigator"
                    ) { (registered, authenticated) ->
                        when {
                            !registered -> {
                                // Pin & Biometric Onboarding Setup View
                                OnboardingSetupView(
                                    onRegister = { pin, enableBio ->
                                        viewModel.registerPin(pin, enableBio)
                                        Toast.makeText(context, "Giriş Şifresi Başarıyla Oluşturuldu!", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                            !authenticated -> {
                                // Locked verification screen (uses premium dark palette)
                                SecurityLockView(
                                    isBiometricsEnabled = isBiometricsEnabled,
                                    onVerifyPin = { pin, callback ->
                                        viewModel.authenticatePin(pin) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Hatalı Giriş Kodu", Toast.LENGTH_SHORT).show()
                                            }
                                            callback(success)
                                        }
                                    },
                                    onTriggerBiometric = {
                                        if (BiometricHelper.isBiometricAvailable(context)) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = this@MainActivity,
                                                onSuccess = {
                                                    viewModel.verifyBiometricSuccess()
                                                    Toast.makeText(context, "Biyometrik Giriş Başarılı", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Hata: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Cihazda biyometrik veri algılanmadı. Lütfen PIN girin.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                            else -> {
                                // Authenticated secure notepad dashboard
                                SecureDashboard(
                                    viewModel = viewModel,
                                    audioRecorderManager = audioRecorderManager,
                                    noteColors = noteColors,
                                    categoryIcons = categoryIcons,
                                    hasMicPermission = hasMicPermission,
                                    onRequestMicPermission = { requestMicPermission() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioRecorderManager.stopPlayback()
    }
}

// 1. SETUP / REGISTRATION VIEW
@Composable
fun OnboardingSetupView(
    onRegister: (String, Boolean) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var enableBiometrics by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val systemHasBiometrics = remember { BiometricHelper.isBiometricAvailable(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Güvenlik Kurulumu",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF334155), CircleShape)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Kişisel Güvenli Kasa",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Notlarınıza yalnızca sizin erişebilmeniz için şifreli bir giriş kodu tanımlayın.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8), // slate 400
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Keypad or simple custom Secure Input Field
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
            label = { Text("Yeni Giriş Kodu (4-6 hane)", color = Color.White.copy(alpha = 0.8f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_pin_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pinConfirm,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pinConfirm = it },
            label = { Text("Giriş Kodunu Doğrula", color = Color.White.copy(alpha = 0.8f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_pin_confirm"),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Biometrics Switch If Available on device
        if (systemHasBiometrics) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334155).copy(alpha = 0.4f))
                    .clickable { enableBiometrics = !enableBiometrics }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Yüz Tanıma",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Yüz Tanıma / Parmak İzi",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Biyometrik doğrulamayı aktif et",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = enableBiometrics,
                    onCheckedChange = { enableBiometrics = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (pin.length < 4) {
                    errorMessage = "Giriş kodu en az 4 haneli olmalıdır."
                } else if (pin != pinConfirm) {
                    errorMessage = "Giriş kodları uyuşmuyor!"
                } else {
                    errorMessage = ""
                    onRegister(pin, enableBiometrics)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_pin_button")
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kayıt Ol & Kilit Aç", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}


// 2. SECURITY LOCK SCREEN VIEW
@Composable
fun SecurityLockView(
    isBiometricsEnabled: Boolean,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var shakes by remember { mutableStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    // Auto-trigger biometrics upon loading the screen if enabled
    LaunchedEffect(isBiometricsEnabled) {
        if (isBiometricsEnabled) {
            delay(400)
            onTriggerBiometric()
        }
    }

    // PIN shake animation for negative feedback
    LaunchedEffect(shakes) {
        if (shakes > 0) {
            shakeOffset.animateTo(
                targetValue = 15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(
                targetValue = -15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // dark cosmic slate representation
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top lock visual header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "🔒",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Güvenli Kasa Kilitli",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Giriş şifrenizi veya yüz tanımanızı kullanın.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Active PIN Bullets Visualizer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = shakeOffset.value.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF334155)
                                }
                            )
                            .border(1.dp, Color(0xFF475569), CircleShape)
                    )
                }
            }
        }

        // Numeric Keypad + Biometrics Button layout Grid Style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        if (key == "BIO" && !isBiometricsEnabled) {
                            // Empty spacer if biometrics are disabled
                            Box(modifier = Modifier.size(64.dp))
                        } else {
                            IconButtonCodeUnit(
                                symbol = key,
                                onClick = {
                                    when (key) {
                                        "DEL" -> {
                                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        }
                                        "BIO" -> {
                                            onTriggerBiometric()
                                        }
                                        else -> {
                                            if (enteredPin.length < 6) {
                                                enteredPin += key
                                                // auto-verify if length meets 4 or more on trigger (using dynamic checking)
                                                if (enteredPin.length >= 4) {
                                                    scope.launch {
                                                        delay(150)
                                                        onVerifyPin(enteredPin) { correct ->
                                                            if (!correct) {
                                                                enteredPin = ""
                                                                shakes++
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonCodeUnit(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (symbol == "DEL" || symbol == "BIO") Color.Transparent else Color(0xFF1E293B)
            )
            .clickable(onClick = onClick)
            .border(
                if (symbol == "DEL" || symbol == "BIO") BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, Color(0xFF334155)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (symbol) {
            "DEL" -> {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Sil",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            "BIO" -> {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biyometrik",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            else -> {
                Text(
                    text = symbol,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


// 3. SECURE AUTHENTICATED DASHBOARD VIEW
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SecureDashboard(
    viewModel: NoteViewModel,
    audioRecorderManager: AudioRecorderManager,
    noteColors: List<Pair<String, String>>,
    categoryIcons: Map<String, ImageVector>,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var showCategoryCreator by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }

    // Active instant audionote record parameters
    var activeAudioPlaybackPath by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    // Coroutine to monitor audio playback progress in real-time
    LaunchedEffect(isAudioPlaying, activeAudioPlaybackPath) {
        if (isAudioPlaying && activeAudioPlaybackPath != null) {
            while (audioRecorderManager.isPlaying()) {
                val current = audioRecorderManager.getCurrentPlaybackPosition().toFloat()
                val total = audioRecorderManager.getPlaybackDuration().toFloat()
                if (total > 0) {
                    playbackProgress = current / total
                }
                delay(100)
            }
            isAudioPlaying = false
            playbackProgress = 0f
        }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "Güvenli Notlarım",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        IconButton(onClick = { showSecuritySettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Güvenlik Ayarları",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Güvenli Çıkış",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Modern Search Field with embedded trailing reset
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text("Notlarda arayın...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("notes_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal Flow of Category Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category list wrapper
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("Tümü", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        items(categories) { category ->
                            val isSelected = selectedCategoryId == category.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCategory(category.id) },
                                label = { Text(category.name, fontSize = 13.sp) },
                                trailingIcon = {
                                    // Let users delete custom categories (keep default prepopulated ones stable)
                                    if (category.id > 4) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Sil",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.deleteCategory(category) }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcons[category.iconName] ?: Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(android.graphics.Color.parseColor(category.colorHex)),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Plus button to add dynamic Category tags
                    IconButton(
                        onClick = { showCategoryCreator = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Kategori Ekle",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.setEditingNote(null) // Fresh new note setup
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Not")
            }
        }
    ) { innerPadding ->
        
        // Main Notes Display Area (Staggered or Linear Card list layout)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredNotes.isEmpty()) {
                // Friendly Empty State Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SpeakerNotesOff,
                        contentDescription = "Boş liste",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Sonuç Bulunamadı" else "Not Defteriniz Boş",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Lütfen arama kelimenizi kontrol edin." else "Sağ aşağıdaki düğmeye tıklayarak ilk güvenli notunuzu oluşturun!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val noteCategory = categories.find { it.id == note.categoryId }
                        val customColor = remember(note.backgroundColorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(note.backgroundColorHex))
                            } catch (e: Exception) {
                                Color(0xFFFAF9F6) // recovery cream fallback
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setEditingNote(note)
                                    showEditor = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = customColor),
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Title, Priority Icons, Pinned Indicator row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (note.isPinned) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = "Pinnned",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = note.title.ifBlank { "Başlıksız Not" },
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Category Badge inside note card
                                        if (noteCategory != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = categoryIcons[noteCategory.iconName] ?: Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = Color(android.graphics.Color.parseColor(noteCategory.colorHex)),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = noteCategory.name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black.copy(alpha = 0.7f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        // Quick pin toggle
                                        IconButton(
                                            onClick = { viewModel.togglePinNote(note) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (note.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Sabitle",
                                                tint = if (note.isPinned) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.4f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Description content snippet
                                Text(
                                    text = note.content.ifBlank { "İçerik yok..." },
                                    fontSize = 14.sp,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )

                                // Voicenote Player Attachment card section (IF exists)
                                if (!note.audioFilePath.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val isThisPlaying = isAudioPlaying && activeAudioPlaybackPath == note.audioFilePath

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (isThisPlaying) {
                                                    audioRecorderManager.stopPlayback()
                                                    isAudioPlaying = false
                                                    activeAudioPlaybackPath = null
                                                } else {
                                                    activeAudioPlaybackPath = note.audioFilePath
                                                    isAudioPlaying = true
                                                    audioRecorderManager.startPlayback(
                                                        filePath = note.audioFilePath,
                                                        onCompletion = {
                                                            isAudioPlaying = false
                                                            activeAudioPlaybackPath = null
                                                        },
                                                        onError = {
                                                            isAudioPlaying = false
                                                            activeAudioPlaybackPath = null
                                                            Toast.makeText(context, "Ses dosyası çalınamadı.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "Oynat",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Sesli Not Kaydı",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = formatDuration(note.audioDurationMs),
                                                    fontSize = 11.sp,
                                                    color = Color.Black.copy(alpha = 0.5f)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Mini custom playback progress slider indicator
                                            LinearProgressIndicator(
                                                progress = { if (isThisPlaying) playbackProgress else 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = Color.Black.copy(alpha = 0.1f),
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Date indicator & Delete Trigger row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dateStr = remember(note.createdAt) {
                                        val format = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault())
                                        format.format(Date(note.createdAt))
                                    }

                                    Text(
                                        text = dateStr,
                                        fontSize = 11.sp,
                                        color = Color.Black.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Medium
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteNote(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Notu Sil",
                                            tint = Color.Red.copy(alpha = 0.7f),
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
    }

    // A. CUSTOM CATEGORY CREATOR DIALOG
    if (showCategoryCreator) {
        var newCatName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#3B82F6") } // Blue default
        var selectedIconKey by remember { mutableStateOf("Favorite") }

        val palette = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#06B6D4")

        Dialog(onDismissRequest = { showCategoryCreator = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Yeni Kategori Ekle", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Kategori Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Renk Seçimi", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Color palette selectors Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        palette.forEach { colorStr ->
                            val c = Color(android.graphics.Color.parseColor(colorStr))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColorHex == colorStr) 3.dp else 0.dp,
                                        color = if (selectedColorHex == colorStr) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = colorStr }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Simge Seçimi", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Icons Choice Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryIcons.keys.forEach { key ->
                            val vector = categoryIcons[key]!!
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedIconKey == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIconKey = key }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = null,
                                    tint = if (selectedIconKey == key) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCategoryCreator = false }) {
                            Text("Vazgeç")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    viewModel.addCategory(newCatName, selectedColorHex, selectedIconKey)
                                    showCategoryCreator = false
                                } else {
                                    Toast.makeText(context, "Lütfen bir ad yazın", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }

    // B. SECURITY SETTINGS SCREEN DIALOG
    if (showSecuritySettings) {
        Dialog(onDismissRequest = { showSecuritySettings = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Güvenlik & Giriş Ayarları", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Yüz Tanıma / Biometrik Giriş", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Uygulama açılışında biyometrik tarama yap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isBiometricsEnabled,
                            onCheckedChange = { viewModel.setBiometricsEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Master custom reset password setup trigger action
                    var showResetForm by remember { mutableStateOf(false) }
                    var newCodeSetting by remember { mutableStateOf("") }

                    if (!showResetForm) {
                        OutlinedButton(
                            onClick = { showResetForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.LockReset, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Giriş Şifresini Değiştir")
                        }
                    } else {
                        Column {
                            OutlinedTextField(
                                value = newCodeSetting,
                                onValueChange = { if (it.all { char -> char.isDigit() }) newCodeSetting = it },
                                label = { Text("Yeni Giriş PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showResetForm = false; newCodeSetting = "" }) {
                                    Text("İptal")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newCodeSetting.length >= 4) {
                                            viewModel.registerPin(newCodeSetting, isBiometricsEnabled)
                                            showResetForm = false
                                            newCodeSetting = ""
                                            Toast.makeText(context, "PIN Değiştirildi", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "PIN en az 4 hane olmalıdır", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Uygula")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showSecuritySettings = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tamam")
                    }
                }
            }
        }
    }

    // C. RICH FULL-SCREEN NOTE EDITOR DIALOG (CREATOR / EDITOR MODEL)
    if (showEditor) {
        // Prepare temporary editor state values holding modifications
        var tempTitle by remember { mutableStateOf(editingNote?.title ?: "") }
        var tempContent by remember { mutableStateOf(editingNote?.content ?: "") }
        var tempCategoryId by remember { mutableStateOf(editingNote?.categoryId) }
        var tempBgColorHex by remember { mutableStateOf(editingNote?.backgroundColorHex ?: "#FAF9F6") }
        var tempAudioPath by remember { mutableStateOf(editingNote?.audioFilePath) }
        var tempAudioDuration by remember { mutableLongStateOf(editingNote?.audioDurationMs ?: 0L) }
        var tempIsPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }

        // Live Recording Parameters
        var isRecording by remember { mutableStateOf(false) }
        var recordTimeElapsed by remember { mutableLongStateOf(0L) }
        var animatedPulsingRadius by remember { mutableStateOf(1f) }

        // Local dynamic record progress simulator timer
        LaunchedEffect(isRecording) {
            if (isRecording) {
                recordTimeElapsed = 0L
                while (isRecording) {
                    delay(250)
                    recordTimeElapsed += 250
                    animatedPulsingRadius = if (animatedPulsingRadius == 1f) 1.25f else 1f
                }
            }
        }

        // Auto release recorder and stop playback on editor exit
        DisposableEffect(Unit) {
            onDispose {
                if (isRecording) {
                    audioRecorderManager.stopRecording()
                }
            }
        }

        Dialog(onDismissRequest = { showEditor = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(20.dp),
                color = remember(tempBgColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(tempBgColorHex))
                    } catch (e: Exception) {
                        Color(0xFFFAF9F6)
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header controls (Back to Dashboard, Star Pin, Save Click)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEditor = false }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kapat", tint = Color.Black.copy(alpha = 0.6f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Star Pin toggle inside edit window
                            IconButton(onClick = { tempIsPinned = !tempIsPinned }) {
                                Icon(
                                    imageVector = if (tempIsPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Sabitle",
                                    tint = if (tempIsPinned) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    viewModel.saveNote(
                                        id = editingNote?.id ?: 0,
                                        title = tempTitle,
                                        content = tempContent,
                                        categoryId = tempCategoryId,
                                        bgColorHex = tempBgColorHex,
                                        audioPath = tempAudioPath,
                                        audioDuration = tempAudioDuration,
                                        isPinned = tempIsPinned
                                    )
                                    showEditor = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Kaydet")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Input with high contrast
                    TextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        placeholder = { Text("Başlık", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.4f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black.copy(alpha = 0.85f),
                            unfocusedTextColor = Color.Black.copy(alpha = 0.85f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )

                    Divider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 8.dp))

                    // Category Pill selector flow for tag labeling
                    Text(
                        text = "Kategori Etiketi:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isActive = tempCategoryId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.04f))
                                    .clickable { tempCategoryId = null }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Etiketsiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.7f))
                            }
                        }

                        items(categories) { cat ->
                            val isActive = tempCategoryId == cat.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.04f))
                                    .clickable { tempCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIcons[cat.iconName] ?: Icons.Default.Category,
                                    contentDescription = null,
                                    tint = Color(android.graphics.Color.parseColor(cat.colorHex)),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multi-line rich content notes text editor body representation
                    TextField(
                        value = tempContent,
                        onValueChange = { tempContent = it },
                        placeholder = { Text("Notunuzu yazın...", fontSize = 15.sp, color = Color.Black.copy(alpha = 0.4f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black.copy(alpha = 0.75f),
                            unfocusedTextColor = Color.Black.copy(alpha = 0.75f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp)
                    )

                    // Color picker selection row inside individual notes (personalization)
                    Text(
                        text = "Not Rengi Özelleştirme:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(noteColors) { (code, title) ->
                            val c = Color(android.graphics.Color.parseColor(code))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (tempBgColorHex == code) 2.dp else 1.dp,
                                        color = if (tempBgColorHex == code) Color.Black else Color.Black.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .clickable { tempBgColorHex = code }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DYNAMIC VOICE RECORDER PREFERRED MODULE FOR ADDING VOICE NOTES
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.04f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (tempAudioPath == null) {
                                // Default mic setup view
                                if (!isRecording) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Bu nota ses kaydı ekleyebilirsiniz.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black.copy(alpha = 0.6f)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (hasMicPermission) {
                                                    val path = audioRecorderManager.startRecording()
                                                    if (path != null) {
                                                        tempAudioPath = path
                                                        isRecording = true
                                                    } else {
                                                        Toast.makeText(context, "Kayıt başlatılamadı. Lütfen mikrofonu kontrol edin.", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    onRequestMicPermission()
                                                }
                                            },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Kaydet",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                } else {
                                    // Live Active Audio Recorder Visualizer Interface
                                    val scale by animateFloatAsState(
                                        targetValue = animatedPulsingRadius,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ), label = "visualScaler"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Red)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Ses Kaydediliyor... [${formatDuration(recordTimeElapsed)}]",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Red
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val duration = audioRecorderManager.stopRecording()
                                                tempAudioDuration = duration
                                                isRecording = false
                                                if (duration <= 0) {
                                                    tempAudioPath = null
                                                }
                                            },
                                            modifier = Modifier
                                                .background(Color.Red, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "Stop",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Exists voicenote audio playback preview layout inside editor
                                val playerActive = isAudioPlaying && activeAudioPlaybackPath == tempAudioPath

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (playerActive) {
                                                audioRecorderManager.stopPlayback()
                                                isAudioPlaying = false
                                            } else {
                                                activeAudioPlaybackPath = tempAudioPath
                                                isAudioPlaying = true
                                                audioRecorderManager.startPlayback(
                                                    filePath = tempAudioPath!!,
                                                    onCompletion = {
                                                        isAudioPlaying = false
                                                    },
                                                    onError = {
                                                        isAudioPlaying = false
                                                        Toast.makeText(context, "Oynatma tamamlanamadı.", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (playerActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Stop",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sesli Not Kaydı Oluşturuldu",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = formatDuration(tempAudioDuration),
                                                fontSize = 11.sp,
                                                color = Color.Black.copy(alpha = 0.5f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        // Progress bar
                                        LinearProgressIndicator(
                                            progress = { if (playerActive) playbackProgress else 0f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = Color.Black.copy(alpha = 0.08f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Let users delete sound and capture again
                                    IconButton(
                                        onClick = {
                                            audioRecorderManager.stopPlayback()
                                            isAudioPlaying = false
                                            // Delete physical audio file from cache for storage cleanup
                                            try {
                                                File(tempAudioPath!!).delete()
                                            } catch (e: Exception) { /* ignore */ }
                                            tempAudioPath = null
                                            tempAudioDuration = 0L
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Ses Kaydını Sil",
                                            tint = Color.Red.copy(alpha = 0.7f),
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
    }
}

// Global millisecond duration parser to visual clock representation e.g. 02:45
fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
