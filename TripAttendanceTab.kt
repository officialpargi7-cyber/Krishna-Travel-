package com.example.ui.trip

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StaffAttendance
import com.example.data.model.UserRole
import com.example.ui.components.Base64PhotoPreview
import com.example.ui.components.LocationStampRow
import com.example.ui.components.LockedRecordBadge
import com.example.ui.components.PhotoAndLocationCaptureSection
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.util.LocationCapture
import com.example.util.MediaAndLocationHelper

@Composable
fun TripAttendanceTab(
    attendance: List<StaffAttendance>,
    role: UserRole,
    isBusy: Boolean,
    onAddAttendance: (String, String, String, String, Double?, Double?, String, String, () -> Unit) -> Unit,
    onDeleteAttendance: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectItem by remember { mutableStateOf<StaffAttendance?>(null) }
    val isOwner = role == UserRole.OWNER

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addAttendanceBtn)
            }
        }

        if (attendance.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.emptyAttendanceMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(attendance, key = { it.id }) { item ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOwner) {
                                Base64PhotoPreview(
                                    base64 = item.photoBase64,
                                    contentDescription = "${item.staffName} photo",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { inspectItem = item }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        item.staffName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = VerifiedTeal.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            strings.attendanceStatusLabel(item.status),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VerifiedTeal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "${strings.staffRoleLabel(item.staffRole)} • ${strings.managerLabel(item.createdByName)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    strings.dateTimePrefix(MediaAndLocationHelper.formatTimestamp(item.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.notes.isNotBlank()) {
                                    Text(item.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isOwner) {
                                IconButton(onClick = { onDeleteAttendance(item.id) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Attendance",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (isOwner) {
                            LocationStampRow(
                                locationLabel = item.capturedLocationLabel,
                                latitude = item.capturedLatitude,
                                longitude = item.capturedLongitude,
                                timestampText = MediaAndLocationHelper.formatTimestamp(item.createdAt)
                            )
                        } else {
                            Surface(
                                color = HighwayAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.attendancePhotoSealedNotice,
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
        inspectItem?.let { item ->
            AlertDialog(
                onDismissRequest = { inspectItem = null },
                title = { Text("${item.staffName} (${strings.staffRoleLabel(item.staffRole)})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Base64PhotoPreview(
                            base64 = item.photoBase64,
                            contentDescription = "${item.staffName} attendance photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(strings.uploadedByManager(item.createdByName), style = MaterialTheme.typography.bodySmall)
                        LocationStampRow(
                            locationLabel = item.capturedLocationLabel,
                            latitude = item.capturedLatitude,
                            longitude = item.capturedLongitude,
                            timestampText = MediaAndLocationHelper.formatTimestamp(item.createdAt)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { inspectItem = null }) { Text(strings.close) }
                }
            )
        }
    }

    if (showAddDialog) {
        AddAttendanceDialog(
            role = role,
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { name, staffRole, status, photoBase64, lat, lng, locLabel, notes ->
                onAddAttendance(name, staffRole, status, photoBase64, lat, lng, locLabel, notes) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun AddAttendanceDialog(
    role: UserRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double?, Double?, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var staffName by remember { mutableStateOf("") }
    var staffRole by remember { mutableStateOf("Driver") }
    var status by remember { mutableStateOf("Present") }
    var notes by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    var locationCapture by remember { mutableStateOf(LocationCapture(label = strings.capturingGps)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.attendanceDialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = { staffName = it },
                    label = { Text(strings.staffMemberNameReq) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.staffRoleLabel, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Driver", "Conductor", "Helper", "Cleaner").forEach { r ->
                        FilterChip(
                            selected = staffRole == r,
                            onClick = { staffRole = r },
                            label = { Text(strings.staffRoleLabel(r)) }
                        )
                    }
                }
                Text(strings.statusLabel, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Present", "Late", "Half-Day").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(strings.attendanceStatusLabel(s)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesOptionalLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PhotoAndLocationCaptureSection(
                    title = strings.staffPhotoTitle,
                    required = true,
                    photoBase64 = photoBase64,
                    locationCapture = locationCapture,
                    onPhotoCaptured = { photoBase64 = it },
                    onLocationCaptured = { locationCapture = it }
                )
                if (role == UserRole.MANAGER) {
                    Text(
                        text = strings.managerAttendanceLockWarning,
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
                        staffName,
                        staffRole,
                        status,
                        photoBase64,
                        locationCapture.latitude,
                        locationCapture.longitude,
                        locationCapture.label,
                        notes
                    )
                },
                enabled = !isBusy && staffName.isNotBlank() && photoBase64.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveAndLockAttendance)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
