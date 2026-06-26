package com.bibleread.bread.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.bibleread.bread.R
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.ui.theme.BackgroundDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom contract to open SAF at the Images category
class OpenImagesSAF : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            val uri = Uri.parse("content://com.android.providers.media.documents/root/images_root")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(isLoggedIn: Boolean = false) {
    if (!isLoggedIn) { ProfileSignInPrompt(); return }
    ProfileContent()
}

@Composable
private fun ProfileSignInPrompt() {
    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Sign in to unlock\ncommunity features",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Join discussions, save highlights, and connect with other readers.",
                color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Account", color = Color.White, fontWeight = FontWeight.Medium,
                    fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { profileImageUri = uri; showSheet = false } }

    val fileLauncher = rememberLauncherForActivityResult(OpenImagesSAF()) { uri ->
        if (uri != null) { profileImageUri = uri; showSheet = false }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) { profileImageUri = tempCameraUri; showSheet = false } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showSheet = true }

    val photosPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms -> if (perms.values.all { it }) showSheet = true }

    fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun createImageFile(): Uri {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${stamp}_", ".jpg", dir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun handleUploadClick() {
        val photosPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        when {
            !hasPermission(Manifest.permission.CAMERA) ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            photosPermissions.any { !hasPermission(it) } ->
                photosPermissionLauncher.launch(photosPermissions)
            else -> showSheet = true
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, top = 16.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SelectionOptionItem("Camera", R.drawable.ic_camera, Color.White, Color.Black) {
                    val uri = createImageFile(); tempCameraUri = uri; cameraLauncher.launch(uri)
                }
                SelectionOptionItem("Photos", R.drawable.ic_photo_library, Color(0xFFD0E2FF), Color(0xFF002D69)) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SelectionOptionItem("Files", R.drawable.ic_description, Color(0xFFE0E0E0), Color.Black) {
                    fileLauncher.launch(arrayOf("image/*"))
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROFILE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp)
        )

        Box(
            modifier = Modifier.padding(horizontal = 16.dp).size(150.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.DarkGray)) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val btnColor by animateColorAsState(
                targetValue = if (isPressed) Color(0xFFD0D0D0) else Color.White,
                label = "btnColor"
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(btnColor)
                    .clickable(interactionSource = interactionSource, indication = ripple()) {
                        handleUploadClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Upload photo",
                    tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("toaderrr", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(32.dp))

        var verseCount by remember { mutableStateOf(-1) }
        var downloadsExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                verseCount = BibleDatabase.getInstance(context).verseDao().getTotalVerseCount()
            }
        }

        SettingItem("Edit Profile")
        SettingItem("Notification Settings")
        SettingItem("Language")
        SettingItem("Theme")

        // Downloads expandable
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { downloadsExpanded = !downloadsExpanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Downloads", color = Color.White, fontSize = 16.sp)
                Icon(
                    painter = painterResource(if (downloadsExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = null, tint = Color.Gray
                )
            }

            if (downloadsExpanded) {
                val isDownloaded = verseCount > 0
                val statusText = when {
                    verseCount < 0 -> "Checking..."
                    isDownloaded   -> "Downloaded"
                    else           -> "Not Downloaded"
                }
                val statusColor = when {
                    verseCount < 0 -> Color.Gray
                    isDownloaded   -> Color(0xFF4CAF50)
                    else           -> Color(0xFFFF5252)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MBBTAG05", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("Magandang Balita Biblia (2005)", color = Color.Gray, fontSize = 12.sp)
                        if (isDownloaded) Text("$verseCount verses", color = Color.Gray, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            painter = painterResource(if (isDownloaded) R.drawable.ic_check_circle else R.drawable.ic_download),
                            contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp)
                        )
                        Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        SettingItem("Log Out", isLast = true)
    }
}

@Composable
fun SelectionOptionItem(
    label: String,
    icon: Int,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null,
                tint = iconColor, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp,
            textAlign = TextAlign.Center, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingItem(label: String, isLast: Boolean = false, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (label == "Log Out") Color.Red else Color.White,
                fontSize = 16.sp
            )
            Icon(painterResource(R.drawable.ic_arrow_right),
                contentDescription = null, tint = Color.Gray)
        }
        if (!isLast) {
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
