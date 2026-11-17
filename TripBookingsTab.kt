package com.example.ui.trip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PassengerBooking
import com.example.data.model.Trip
import com.example.data.model.TripMetrics
import com.example.data.model.UserRole
import com.example.ui.components.InlineCalculatorPanel
import com.example.ui.components.LockedRecordBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.util.MediaAndLocationHelper
import java.util.Locale

@Composable
fun TripBookingsTab(
    trip: Trip,
    bookings: List<PassengerBooking>,
    metrics: TripMetrics,
    role: UserRole,
    isBusy: Boolean,
    onAddBooking: (String, String, List<Int>, String, String, Double, Double, String, String, String, () -> Unit) -> Unit,
    onMarkPaid: (PassengerBooking) -> Unit,
    onDeleteBooking: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var billFilter by remember { mutableStateOf("ALL") } // ALL, UNPAID, PAID

    val alreadyBookedCabins = remember(bookings) {
        bookings.flatMap { it.cabinNumbers }.map { it.toInt() }.toSet()
    }
    val visibleBookings = remember(bookings, billFilter) {
        when (billFilter) {
            "UNPAID" -> bookings.filter { it.paymentStatus != "Paid" || it.paidAmount <= 0.0 }
            "PAID" -> bookings.filter { it.paymentStatus == "Paid" && it.paidAmount > 0.0 }
            else -> bookings
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.bookedKpi, style = MaterialTheme.typography.labelSmall)
                        Text(
                            strings.cabinsRatio(metrics.totalBookedCabins, 30),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(strings.collected, style = MaterialTheme.typography.labelSmall)
                        Text(
                            MediaAndLocationHelper.formatInr(metrics.totalCollected),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VerifiedTeal
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(strings.pendingDueKpi, style = MaterialTheme.typography.labelSmall)
                        Text(
                            MediaAndLocationHelper.formatInr(metrics.totalPending),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = HighwayAmber
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
                    .testTag("add_booking_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.newPassengerBookingBtn)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = billFilter == "ALL",
                    onClick = { billFilter = "ALL" },
                    label = { Text(strings.filterAllBills(bookings.size)) }
                )
                FilterChip(
                    selected = billFilter == "UNPAID",
                    onClick = { billFilter = "UNPAID" },
                    label = { Text(strings.filterUnpaidNoFare) }
                )
                FilterChip(
                    selected = billFilter == "PAID",
                    onClick = { billFilter = "PAID" },
                    label = { Text(strings.filterPaid) }
                )
            }
        }

        if (visibleBookings.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (bookings.isEmpty()) {
                            strings.emptyBookingsMsg
                        } else {
                            strings.emptyFilteredBillsMsg
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(visibleBookings, key = { it.id }) { booking ->
                val statusColor = when (booking.paymentStatus) {
                    "Paid" -> VerifiedTeal
                    "Partial" -> HighwayAmber
                    else -> KrishnaCrimson
                }
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                booking.invoiceNumber,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = KrishnaCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = statusColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    "${strings.paymentStatusLabel(booking.paymentStatus)} • ${strings.paymentModeLabel(booking.paymentMode)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            "${booking.passengerName} ${if (booking.passengerPhone.isNotBlank()) "• ${booking.passengerPhone}" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${strings.cabinsKpi}: ${booking.berthNumbers} | ${booking.boardingPoint} → ${booking.droppingPoint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    strings.paidOutOf(
                                        MediaAndLocationHelper.formatInr(booking.paidAmount),
                                        MediaAndLocationHelper.formatInr(booking.fareAmount)
                                    ),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    strings.managerTimeLine(
                                        booking.createdByName,
                                        MediaAndLocationHelper.formatTimestamp(booking.createdAt)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    MediaAndLocationHelper.shareBookingInvoice(context, trip, booking)
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share Bill", tint = VerifiedTeal)
                                }
                                if (role == UserRole.OWNER) {
                                    if (booking.paymentStatus != "Paid") {
                                        IconButton(onClick = { onMarkPaid(booking) }) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Mark Paid", tint = VerifiedTeal)
                                        }
                                    }
                                    IconButton(onClick = { onDeleteBooking(booking.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Booking", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        LockedRecordBadge(role = role)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBookingDialog(
            trip = trip,
            alreadyBookedCabins = alreadyBookedCabins,
            role = role,
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes ->
                onAddBooking(name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes) {
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBookingDialog(
    trip: Trip,
    alreadyBookedCabins: Set<Int>,
    role: UserRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, List<Int>, String, String, Double, Double, String, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var passengerName by remember { mutableStateOf("") }
    var passengerPhone by remember { mutableStateOf("") }
    var selectedCabins by remember { mutableStateOf(setOf<Int>()) }
    var boarding by remember { mutableStateOf(trip.origin) }
    var dropping by remember { mutableStateOf(trip.destination) }
    var farePerCabinStr by remember { mutableStateOf("800") }
    var totalFareStr by remember { mutableStateOf("") }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    var didntGiveFare by remember { mutableStateOf(false) }

    // Auto-calculate total fare whenever cabin count or per-cabin fare changes
    LaunchedEffect(selectedCabins.size, farePerCabinStr, didntGiveFare) {
        val perCabin = farePerCabinStr.toDoubleOrNull()
        if (selectedCabins.isNotEmpty() && perCabin != null && perCabin > 0) {
            val total = selectedCabins.size * perCabin
            totalFareStr = String.format(Locale.US, "%.0f", total)
            if (didntGiveFare) {
                paidAmountStr = "0"
            } else if (paidAmountStr.isBlank()) {
                paidAmountStr = totalFareStr
            }
        } else if (didntGiveFare) {
            paidAmountStr = "0"
        }
    }

    val totalFare = totalFareStr.toDoubleOrNull() ?: 0.0
    val paidAmount = paidAmountStr.toDoubleOrNull() ?: 0.0
    val balanceDue = (totalFare - paidAmount).coerceAtLeast(0.0)
    val autoPaymentStatus = when {
        paidAmount >= totalFare && totalFare > 0.0 -> "Paid"
        paidAmount > 0.0 -> "Partial"
        else -> "Pending"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.passengerBookingDialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = passengerName,
                    onValueChange = { passengerName = it },
                    label = { Text(strings.passengerNameReq) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passengerPhone,
                    onValueChange = { passengerPhone = it },
                    label = { Text(strings.phoneNumberLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(strings.selectSleeperCabinsReq, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..30).forEach { c ->
                        val isBooked = c in alreadyBookedCabins
                        val isSelected = c in selectedCabins
                        Surface(
                            color = when {
                                isSelected -> KrishnaCrimson
                                isBooked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !isBooked) {
                                selectedCabins = if (isSelected) selectedCabins - c else selectedCabins + c
                            }
                        ) {
                            Text(
                                text = "C${c.toString().padStart(2, '0')}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isSelected -> androidx.compose.ui.graphics.Color.White
                                    isBooked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = boarding,
                        onValueChange = { boarding = it },
                        label = { Text(strings.boardingLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dropping,
                        onValueChange = { dropping = it },
                        label = { Text(strings.droppingLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = farePerCabinStr,
                        onValueChange = { farePerCabinStr = it },
                        label = { Text(strings.farePerCabinLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalFareStr,
                        onValueChange = { totalFareStr = it },
                        label = { Text(strings.totalFareLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = paidAmountStr,
                    onValueChange = {
                        paidAmountStr = it
                        didntGiveFare = (it.toDoubleOrNull() ?: 0.0) <= 0.0
                    },
                    label = { Text(strings.paidAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FilterChip(
                    selected = didntGiveFare,
                    onClick = {
                        didntGiveFare = !didntGiveFare
                        if (didntGiveFare) {
                            paidAmountStr = "0"
                            if (notes.isBlank()) notes = strings.didntGiveFareNoteDefault
                        } else if (totalFareStr.isNotBlank()) {
                            paidAmountStr = totalFareStr
                            if (notes == strings.didntGiveFareNoteDefault || notes == "Didn't give fare") notes = ""
                        }
                    },
                    label = { Text(strings.didntGiveFareChip) }
                )

                InlineCalculatorPanel(
                    onApplyResult = { res ->
                        paidAmountStr = String.format(Locale.US, "%.0f", res)
                    }
                )

                Surface(
                    color = VerifiedTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            strings.statusPrefix(strings.paymentStatusLabel(autoPaymentStatus)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            strings.balanceDuePrefix(MediaAndLocationHelper.formatInr(balanceDue)),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (balanceDue > 0) HighwayAmber else VerifiedTeal
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "UPI", "Online").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(strings.paymentModeLabel(mode)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (role == UserRole.MANAGER) {
                    Text(
                        text = strings.managerBookingLockWarning,
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
                        passengerPhone,
                        selectedCabins.toList(),
                        boarding,
                        dropping,
                        totalFare,
                        paidAmount,
                        autoPaymentStatus,
                        paymentMode,
                        notes
                    )
                },
                enabled = !isBusy && passengerName.isNotBlank() && selectedCabins.isNotEmpty() && totalFare >= 0,
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveAndLockBill)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
