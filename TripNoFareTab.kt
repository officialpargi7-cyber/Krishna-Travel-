package com.example.ui.trip

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.NoFareRecord
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
fun TripNoFareTab(
    records: List<NoFareRecord>,
    role: UserRole,
    isBusy: Boolean,
    onAddRecord: (String, String, Int, Double, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onToggleResolved: (NoFareRecord) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectRecord by remember { mutableStateOf<NoFareRecord?>(null) }
    val isOwner = role == UserRole.OWNER

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = KrishnaCrimson)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            strings.noFareHeaderTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isOwner) strings.noFareHeaderOwner else strings.noFareHeaderManager,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_no_fare_button")
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addNoFareBtn)
            }
        }

        if (records.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        strings.emptyNoFareMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(records, key = { it.id }) { rec ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (rec.photoBase64.isNotBlank()) {
                                if (isOwner) {
                                    Base64PhotoPreview(
                                        base64 = rec.photoBase64,
                                        contentDescription = "${rec.personName} photo",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { inspectRecord = rec }
                                    )
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Rounded.Lock,
                                                    contentDescription = "Photo sealed for Owner",
                                                    tint = HighwayAmber,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    strings.sealedBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        rec.personName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (rec.resolvedByOwner) VerifiedTeal.copy(alpha = 0.14f) else KrishnaCrimson.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            if (rec.resolvedByOwner) strings.resolvedBadge else strings.noFareBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rec.resolvedByOwner) VerifiedTeal else KrishnaCrimson,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    strings.mobilePrefix(rec.mobileNumber),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val cabinInfo = if (rec.cabinNumber > 0) "${strings.cabinsKpi} C${rec.cabinNumber.toString().padStart(2, '0')}" else strings.generalStanding
                                val dueInfo = if (rec.expectedFare > 0) " • ${strings.duePrefix(MediaAndLocationHelper.formatInr(rec.expectedFare))}" else ""
                                Text(
                                    "$cabinInfo$dueInfo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (rec.reasonOrNote.isNotBlank()) {
                                    Text(rec.reasonOrNote, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    strings.managerTimeLine(rec.createdByName, MediaAndLocationHelper.formatTimestamp(rec.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(onClick = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${rec.mobileNumber}"))
                                    context.startActivity(dial)
                                }) {
                                    Icon(Icons.Rounded.Call, contentDescription = "Call ${rec.personName}", tint = VerifiedTeal)
                                }
                                if (isOwner) {
                                    IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Record", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        if (isOwner) {
                            if (rec.capturedLocationLabel.isNotBlank()) {
                                LocationStampRow(
                                    locationLabel = rec.capturedLocationLabel,
                                    latitude = rec.capturedLatitude,
                                    longitude = rec.capturedLongitude,
                                    timestampText = MediaAndLocationHelper.formatTimestamp(rec.createdAt)
                                )
                            }
                            OutlinedButton(
                                onClick = { onToggleResolved(rec) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (rec.resolvedByOwner) strings.markedResolvedByOwner else strings.markFareCollectedResolved)
                            }
                        } else if (rec.photoBase64.isNotBlank()) {
                            Surface(
                                color = HighwayAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.noFarePhotoSealedNotice,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        LockedRecordBadge(role = role)
                    }
                }
            }
        }
    }

    if (isOwner) {
        inspectRecord?.let { rec ->
            AlertDialog(
                onDismissRequest = { inspectRecord = null },
                title = { Text("${rec.personName} • ${rec.mobileNumber}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Base64PhotoPreview(
                            base64 = rec.photoBase64,
                            contentDescription = "${rec.personName} photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(strings.managerLabel(rec.createdByName), style = MaterialTheme.typography.bodySmall)
                        if (rec.capturedLocationLabel.isNotBlank()) {
                            LocationStampRow(
                                locationLabel = rec.capturedLocationLabel,
                                latitude = rec.capturedLatitude,
                                longitude = rec.capturedLongitude,
                                timestampText = MediaAndLocationHelper.formatTimestamp(rec.createdAt)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { inspectRecord = null }) { Text(strings.close) }
                }
            )
        }
    }

    if (showAddDialog) {
        AddNoFareDialog(
            role = role,
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { name, mobile, cabin, fare, reason, photo, lat, lng, loc ->
                onAddRecord(name, mobile, cabin, fare, reason, photo, lat, lng, loc) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun AddNoFareDialog(
    role: UserRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double, String, String, Double?, Double?, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var personName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var cabinStr by remember { mutableStateOf("") }
    var expectedFareStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(strings.reasonDidntPay) }
    var photoBase64 by remember { mutableStateOf("") }
    var locationCapture by remember { mutableStateOf(LocationCapture(label = strings.capturingGps)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addNoFareDialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(strings.personNameReq) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("no_fare_name_input")
                )
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text(strings.mobileNumberReq) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("no_fare_mobile_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cabinStr,
                        onValueChange = { cabinStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.cabin1To30Label) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expectedFareStr,
                        onValueChange = { expectedFareStr = it },
                        label = { Text(strings.dueFareLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        strings.reasonDidntPay,
                        strings.reasonPayOnArrival,
                        strings.reasonStaffKnown,
                        strings.reasonHalfPaid
                    ).forEach { tag ->
                        FilterChip(
                            selected = reason == tag,
                            onClick = { reason = tag },
                            label = { Text(tag) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(strings.reasonHistoryNoteLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                PhotoAndLocationCaptureSection(
                    title = strings.personIdPhotoTitle,
                    required = false,
                    photoBase64 = photoBase64,
                    locationCapture = locationCapture,
                    onPhotoCaptured = { photoBase64 = it },
                    onLocationCaptured = { locationCapture = it }
                )

                if (role == UserRole.MANAGER) {
                    Text(
                        text = strings.noFareLockWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = HighwayAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cabin = (cabinStr.toIntOrNull() ?: 0).coerceIn(0, 30)
                    val due = (expectedFareStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onSave(
                        personName,
                        mobileNumber,
                        cabin,
                        due,
                        reason,
                        photoBase64,
                        locationCapture.latitude,
                        locationCapture.longitude,
                        locationCapture.label
                    )
                },
                enabled = !isBusy && personName.isNotBlank() && mobileNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveAndLockHistory)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
