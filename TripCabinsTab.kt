package com.example.ui.trip

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirlineSeatIndividualSuite
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CompartmentPhoto
import com.example.data.model.PassengerBooking
import com.example.data.model.Trip
import com.example.data.model.UserRole
import com.example.ui.components.Base64PhotoPreview
import com.example.ui.components.LocationStampRow
import com.example.ui.components.LockedRecordBadge
import com.example.ui.components.PhotoAndLocationCaptureSection
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.util.LocationCapture
import com.example.util.MediaAndLocationHelper

@Composable
fun TripCabinsTab(
    trip: Trip,
    photos: List<CompartmentPhoto>,
    bookings: List<PassengerBooking>,
    role: UserRole,
    isBusy: Boolean,
    onUploadCabinPhoto: (Int, String, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onToggleVerified: (CompartmentPhoto) -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var deckFilter by remember { mutableStateOf("ALL") } // ALL, LOWER, UPPER
    var uploadCabinTarget by remember { mutableStateOf<Int?>(null) }
    var inspectPhoto by remember { mutableStateOf<CompartmentPhoto?>(null) }
    var sealedCabinNotice by remember { mutableStateOf<Int?>(null) }

    val photosByCabin = remember(photos) {
        photos.associateBy { it.cabinNumber.toInt() }
    }
    val passengerByCabin = remember(bookings) {
        buildMap {
            bookings.forEach { booking ->
                booking.cabinNumbers.forEach { cabin ->
                    put(cabin.toInt(), booking.passengerName)
                }
            }
        }
    }

    val visibleCabins = remember(deckFilter) {
        when (deckFilter) {
            "LOWER" -> (1..15).toList()
            "UPPER" -> (16..30).toList()
            else -> (1..30).toList()
        }
    }
    val uploadedCount = photosByCabin.keys.count { it in 1..30 }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_sleeper_cabin),
                            contentDescription = "Sleeper Compartment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.78f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    strings.cabinsHeroTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (role == UserRole.OWNER) {
                                        strings.cabinsHeroOwnerSub(uploadedCount)
                                    } else {
                                        strings.cabinsHeroManagerSub(uploadedCount)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                "$uploadedCount/30",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                color = HighwayAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { uploadedCount / 30f },
                        color = VerifiedTeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = deckFilter == "ALL",
                    onClick = { deckFilter = "ALL" },
                    label = { Text(strings.filterAll30) }
                )
                FilterChip(
                    selected = deckFilter == "LOWER",
                    onClick = { deckFilter = "LOWER" },
                    label = { Text(strings.filterLower15) }
                )
                FilterChip(
                    selected = deckFilter == "UPPER",
                    onClick = { deckFilter = "UPPER" },
                    label = { Text(strings.filterUpper30) }
                )
            }
        }

        val rows = visibleCabins.chunked(3)
        items(rows.size) { rowIndex ->
            val rowCabins = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCabins.forEach { cabinNum ->
                    val photo = photosByCabin[cabinNum]
                    val passenger = passengerByCabin[cabinNum] ?: photo?.passengerName.orEmpty()
                    CabinTile(
                        cabinNumber = cabinNum,
                        passengerName = passenger,
                        photo = photo,
                        isOwner = role == UserRole.OWNER,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cabin_tile_$cabinNum"),
                        onClick = {
                            if (photo != null) {
                                if (role == UserRole.OWNER) {
                                    inspectPhoto = photo
                                } else {
                                    sealedCabinNotice = cabinNum
                                }
                            } else {
                                uploadCabinTarget = cabinNum
                            }
                        }
                    )
                }
                repeat(3 - rowCabins.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    sealedCabinNotice?.let { lockedCabin ->
        AlertDialog(
            onDismissRequest = { sealedCabinNotice = null },
            title = { Text(strings.sealedCabinTitle(lockedCabin.toString().padStart(2, '0'))) },
            text = {
                Text(
                    strings.sealedCabinBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { sealedCabinNotice = null }) { Text(strings.ok) }
            }
        )
    }

    uploadCabinTarget?.let { targetCabin ->
        UploadCabinPhotoDialog(
            cabinNumber = targetCabin,
            prefilledPassenger = passengerByCabin[targetCabin].orEmpty(),
            role = role,
            isBusy = isBusy,
            onDismiss = { uploadCabinTarget = null },
            onSave = { passengerName, note, base64, lat, lng, locLabel ->
                onUploadCabinPhoto(targetCabin, passengerName, note, base64, lat, lng, locLabel) {
                    uploadCabinTarget = null
                }
            }
        )
    }

    inspectPhoto?.let { photo ->
        // Keep fresh copy from state if updated (e.g. verifiedByOwner toggled)
        val freshPhoto = photos.firstOrNull { it.id == photo.id } ?: photo
        CabinPhotoInspectorDialog(
            trip = trip,
            photo = freshPhoto,
            role = role,
            onDismiss = { inspectPhoto = null },
            onShare = {
                val caption = buildString {
                    appendLine("Krishna Travels — ${freshPhoto.berthLabel} Confirmation")
                    appendLine("Bus: ${trip.busNumber} | Route: ${trip.routeName}")
                    if (freshPhoto.passengerName.isNotBlank()) appendLine("Passenger: ${freshPhoto.passengerName}")
                    if (freshPhoto.conditionNote.isNotBlank()) appendLine("Note: ${freshPhoto.conditionNote}")
                    appendLine("Captured: ${MediaAndLocationHelper.formatTimestamp(freshPhoto.createdAt)}")
                    appendLine("Location: ${freshPhoto.capturedLocationLabel}")
                    appendLine("Uploaded by: ${freshPhoto.createdByName}")
                }
                MediaAndLocationHelper.sharePhotoWithCaption(
                    context = context,
                    photoBase64 = freshPhoto.photoBase64,
                    fileNamePrefix = "cabin_${freshPhoto.cabinNumber}",
                    subject = "${freshPhoto.berthLabel} Photo - Krishna Travels",
                    caption = caption
                )
            },
            onToggleVerified = { onToggleVerified(freshPhoto) },
            onDelete = {
                onDeletePhoto(freshPhoto.id)
                inspectPhoto = null
            }
        )
    }
}

@Composable
private fun CabinTile(
    cabinNumber: Int,
    passengerName: String,
    photo: CompartmentPhoto?,
    isOwner: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isUploaded = photo != null
    val borderColor = when {
        photo?.verifiedByOwner == true -> VerifiedTeal
        isUploaded -> HighwayAmber
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = modifier
            .height(124.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (photo != null && isOwner) {
                Base64PhotoPreview(
                    base64 = photo.photoBase64,
                    contentDescription = "Cabin $cabinNumber photo",
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )
            } else if (photo != null && !isOwner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isUploaded && isOwner) Color.Black.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "C${cabinNumber.toString().padStart(2, '0')}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUploaded && isOwner) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (photo != null) {
                        Icon(
                            imageVector = if (photo.verifiedByOwner) Icons.Rounded.Verified else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (photo.verifiedByOwner) VerifiedTeal else HighwayAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AirlineSeatIndividualSuite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (photo == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Upload cabin photo",
                            tint = KrishnaCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (passengerName.isNotBlank()) passengerName else strings.tapPhoto,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (!isOwner) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Sealed for Owner",
                            tint = HighwayAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.photoSealed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.ownerOnly,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = passengerName.ifBlank { strings.passengerFallback },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (photo.capturedLatitude != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = HighwayAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = strings.gpsTagged,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
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
private fun UploadCabinPhotoDialog(
    cabinNumber: Int,
    prefilledPassenger: String,
    role: UserRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double?, Double?, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var passengerName by remember { mutableStateOf(prefilledPassenger) }
    var conditionNote by remember { mutableStateOf(strings.defaultCabinNote) }
    var photoBase64 by remember { mutableStateOf("") }
    var locationCapture by remember { mutableStateOf(LocationCapture(label = strings.capturingGps)) }
    val deck = if (cabinNumber <= 15) strings.lowerDeck else strings.upperDeck

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.cabinDialogTitle(cabinNumber.toString().padStart(2, '0'), deck))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = passengerName,
                    onValueChange = { passengerName = it },
                    label = { Text(strings.passengerNameOpt) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conditionNote,
                    onValueChange = { conditionNote = it },
                    label = { Text(strings.cabinNoteLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                PhotoAndLocationCaptureSection(
                    title = strings.cabinPhotoSectionTitle,
                    required = true,
                    photoBase64 = photoBase64,
                    locationCapture = locationCapture,
                    onPhotoCaptured = { photoBase64 = it },
                    onLocationCaptured = { locationCapture = it }
                )
                if (role == UserRole.MANAGER) {
                    Text(
                        text = strings.managerCabinLockWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = HighwayAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        passengerName,
                        conditionNote,
                        photoBase64,
                        locationCapture.latitude,
                        locationCapture.longitude,
                        locationCapture.label
                    )
                },
                enabled = !isBusy && photoBase64.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveAndLockPhoto)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun CabinPhotoInspectorDialog(
    trip: Trip,
    photo: CompartmentPhoto,
    role: UserRole,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onToggleVerified: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(photo.berthLabel)
                LockedRecordBadge(role = role)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Base64PhotoPreview(
                    base64 = photo.photoBase64,
                    contentDescription = photo.berthLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                if (photo.passengerName.isNotBlank()) {
                    Text(
                        "${strings.passengerFallback}: ${photo.passengerName}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (photo.conditionNote.isNotBlank()) {
                    Text(photo.conditionNote, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    strings.uploadedByRole(photo.createdByName, strings.roleLabel(photo.createdByRole)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LocationStampRow(
                    locationLabel = photo.capturedLocationLabel,
                    latitude = photo.capturedLatitude,
                    longitude = photo.capturedLongitude,
                    timestampText = MediaAndLocationHelper.formatTimestamp(photo.createdAt)
                )

                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.sharePhotoToPassengerOwner)
                }

                if (role == UserRole.OWNER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleVerified,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (photo.verifiedByOwner) strings.verifiedLabel else strings.verifyLabel)
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.delete)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
