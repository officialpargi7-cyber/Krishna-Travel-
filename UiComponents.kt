package com.example.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.util.LocationCapture
import com.example.util.MediaAndLocationHelper
import kotlinx.coroutines.launch

@Composable
fun RoleBadgePill(role: UserRole, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    val bgColor = if (role == UserRole.OWNER) HighwayAmber.copy(alpha = 0.16f) else VerifiedTeal.copy(alpha = 0.16f)
    val contentColor = if (role == UserRole.OWNER) HighwayAmber else VerifiedTeal
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (role == UserRole.OWNER) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (role == UserRole.OWNER) strings.roleOwnerUpper else strings.roleManagerUpper,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LockedRecordBadge(role: UserRole, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = strings.lockedOwnerOnlyEdit,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (role == UserRole.MANAGER) strings.lockedOwnerOnlyEdit else strings.ownerEditable,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LocationStampRow(
    locationLabel: String,
    latitude: Double?,
    longitude: Double?,
    timestampText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (latitude != null && longitude != null) {
                    Modifier.clickable {
                        MediaAndLocationHelper.openLocationInMaps(
                            context,
                            latitude,
                            longitude,
                            locationLabel.ifBlank { strings.openInMaps }
                        )
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = strings.openInMaps,
                tint = KrishnaCrimson,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = locationLabel.ifBlank { strings.locationNotRecorded },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timestampText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (latitude != null && longitude != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = strings.openInMaps,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun Base64PhotoPreview(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember(base64) { MediaAndLocationHelper.decodeBase64ToImageBitmap(base64) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PhotoAndLocationCaptureSection(
    title: String,
    required: Boolean,
    photoBase64: String,
    locationCapture: LocationCapture,
    onPhotoCaptured: (String) -> Unit,
    onLocationCaptured: (LocationCapture) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun refreshLocation() {
        scope.launch {
            isFetchingGps = true
            val cap = MediaAndLocationHelper.captureCurrentLocation(context)
            onLocationCaptured(cap)
            isFetchingGps = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            refreshLocation()
        } else {
            onLocationCaptured(LocationCapture(label = strings.locationPermissionDenied))
        }
    }

    LaunchedEffect(Unit) {
        if (MediaAndLocationHelper.hasLocationPermission(context)) {
            refreshLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                isProcessingPhoto = true
                val b64 = MediaAndLocationHelper.compressUriToBase64(context, uri)
                if (b64 != null) onPhotoCaptured(b64)
                refreshLocation()
                isProcessingPhoto = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingPhoto = true
                val b64 = MediaAndLocationHelper.compressUriToBase64(context, uri)
                if (b64 != null) onPhotoCaptured(b64)
                refreshLocation()
                isProcessingPhoto = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$title (${if (required) strings.requiredSuffix else strings.optionalSuffix})",
            style = MaterialTheme.typography.titleSmall
        )

        if (photoBase64.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, VerifiedTeal, RoundedCornerShape(14.dp))
            ) {
                Base64PhotoPreview(
                    base64 = photoBase64,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = VerifiedTeal,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.photoReady, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val uri = MediaAndLocationHelper.createCameraImageUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                enabled = !isProcessingPhoto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.camera)
            }

            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isProcessingPhoto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.gallery)
            }
        }

        if (isProcessingPhoto) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.compressingAndTaggingGps, style = MaterialTheme.typography.bodySmall)
            }
        }

        // GPS Capture Indicator
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (MediaAndLocationHelper.hasLocationPermission(context)) {
                        refreshLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (locationCapture.latitude != null) VerifiedTeal else HighwayAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFetchingGps) strings.fetchingGps else locationCapture.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.tapToRefreshGps,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InlineCalculatorPanel(
    onApplyResult: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    var expr by remember { mutableStateOf("") }

    val evaluated: Double? = remember(expr) { evaluateSimpleExpression(expr) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (expanded) strings.hideCalculator else strings.openCalculator)
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expr.ifBlank { "0" },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = evaluated?.let { "= ${MediaAndLocationHelper.formatInr(it)}" } ?: "",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = VerifiedTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val rows = listOf(
                        listOf("7", "8", "9", "÷"),
                        listOf("4", "5", "6", "×"),
                        listOf("1", "2", "3", "-"),
                        listOf("C", "0", ".", "+")
                    )
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { key ->
                                FilledTonalButton(
                                    onClick = {
                                        when (key) {
                                            "C" -> expr = ""
                                            else -> expr += key
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Text(key, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (expr.isNotEmpty()) expr = expr.dropLast(1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.delKey)
                        }
                        Button(
                            onClick = {
                                evaluated?.let {
                                    if (it > 0) {
                                        onApplyResult(it)
                                        expanded = false
                                    }
                                }
                            },
                            enabled = (evaluated ?: 0.0) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedTeal),
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(strings.useAmount)
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateSimpleExpression(raw: String): Double? {
    val clean = raw.replace("×", "*").replace("÷", "/").replace(" ", "")
    if (clean.isBlank()) return null
    return try {
        val tokens = mutableListOf<String>()
        var num = StringBuilder()
        for (ch in clean) {
            if (ch.isDigit() || ch == '.') {
                num.append(ch)
            } else if (ch in listOf('+', '-', '*', '/')) {
                if (num.isNotEmpty()) {
                    tokens.add(num.toString())
                    num = StringBuilder()
                }
                tokens.add(ch.toString())
            }
        }
        if (num.isNotEmpty()) tokens.add(num.toString())
        if (tokens.isEmpty()) return null

        // First pass: * and /
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if ((t == "*" || t == "/") && pass1.isNotEmpty() && i + 1 < tokens.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: return null
                val right = tokens[i + 1].toDoubleOrNull() ?: return null
                val res = if (t == "*") left * right else if (right != 0.0) left / right else return null
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(t)
                i++
            }
        }

        // Second pass: + and -
        var total = pass1.firstOrNull()?.toDoubleOrNull() ?: return null
        var j = 1
        while (j + 1 < pass1.size) {
            val op = pass1[j]
            val rhs = pass1[j + 1].toDoubleOrNull() ?: return null
            total = if (op == "+") total + rhs else if (op == "-") total - rhs else return null
            j += 2
        }
        total
    } catch (_: Exception) {
        null
    }
}
