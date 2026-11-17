package com.example.ui.trip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Toll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.TripExpense
import com.example.data.model.TripMetrics
import com.example.data.model.UserRole
import com.example.ui.components.Base64PhotoPreview
import com.example.ui.components.InlineCalculatorPanel
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
import java.util.Locale

@Composable
fun TripExpensesTab(
    expenses: List<TripExpense>,
    metrics: TripMetrics,
    role: UserRole,
    isBusy: Boolean,
    onAddExpense: (String, Double, Double, Double, Double, String, String, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectExpense by remember { mutableStateOf<TripExpense?>(null) }

    val filtered = remember(expenses, selectedFilter) {
        if (selectedFilter == "All") expenses else expenses.filter { it.category == selectedFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                strings.dieselLitersLabel(String.format(Locale.US, "%.1f", metrics.dieselLiters)),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                MediaAndLocationHelper.formatInr(metrics.dieselExpense),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = KrishnaCrimson
                            )
                        }
                        Column {
                            Text(strings.toll, style = MaterialTheme.typography.labelSmall)
                            Text(
                                MediaAndLocationHelper.formatInr(metrics.tollExpense),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = HighwayAmber
                            )
                        }
                        Column {
                            Text(strings.taxPermit, style = MaterialTheme.typography.labelSmall)
                            Text(
                                MediaAndLocationHelper.formatInr(metrics.taxExpense),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.totalLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                MediaAndLocationHelper.formatInr(metrics.totalExpenses),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = KrishnaCrimson
                            )
                        }
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
                    .testTag("add_expense_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addExpenseBtn)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Diesel", "Toll", "Tax", "Maintenance", "Allowance", "Other").forEach { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(strings.expenseCategoryLabel(cat)) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        strings.emptyExpensesMsg(strings.expenseCategoryLabel(selectedFilter)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { exp ->
                ExpenseCard(
                    expense = exp,
                    role = role,
                    onInspectPhoto = { inspectExpense = exp },
                    onDelete = { onDeleteExpense(exp.id) }
                )
            }
        }
    }

    if (role == UserRole.OWNER) {
        inspectExpense?.let { exp ->
            AlertDialog(
                onDismissRequest = { inspectExpense = null },
                title = { Text("${strings.expenseCategoryLabel(exp.category)} • ${MediaAndLocationHelper.formatInr(exp.amount)}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Base64PhotoPreview(
                            base64 = exp.receiptPhotoBase64,
                            contentDescription = "${exp.category} receipt photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(
                            strings.managerLabel(exp.createdByName),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        LocationStampRow(
                            locationLabel = exp.capturedLocationLabel,
                            latitude = exp.capturedLatitude,
                            longitude = exp.capturedLongitude,
                            timestampText = MediaAndLocationHelper.formatTimestamp(exp.createdAt)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { inspectExpense = null }) { Text(strings.close) }
                }
            )
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            currentTripTotal = metrics.totalExpenses,
            role = role,
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc ->
                onAddExpense(cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: TripExpense,
    role: UserRole,
    onInspectPhoto: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isOwner = role == UserRole.OWNER
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = KrishnaCrimson.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (expense.category) {
                            "Diesel" -> Icons.Rounded.LocalGasStation
                            "Toll" -> Icons.Rounded.Toll
                            else -> Icons.AutoMirrored.Rounded.ReceiptLong
                        },
                        contentDescription = null,
                        tint = KrishnaCrimson,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${strings.expenseCategoryLabel(expense.category)} • ${strings.paymentModeLabel(expense.paymentMode)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (expense.vendorOrPlaza.isNotBlank()) {
                        Text(expense.vendorOrPlaza, style = MaterialTheme.typography.bodySmall)
                    }
                    if (expense.category == "Diesel" && expense.liters > 0) {
                        Text(
                            "${String.format(Locale.US, "%.1f", expense.liters)} L @ ₹${String.format(Locale.US, "%.2f", expense.ratePerLiter)}/L",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        strings.managerTimeLine(expense.createdByName, MediaAndLocationHelper.formatTimestamp(expense.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = MediaAndLocationHelper.formatInr(expense.amount),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = KrishnaCrimson,
                    fontWeight = FontWeight.Bold
                )
                if (isOwner) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (expense.receiptPhotoBase64.isNotBlank()) {
                if (isOwner) {
                    Base64PhotoPreview(
                        base64 = expense.receiptPhotoBase64,
                        contentDescription = "${expense.category} receipt photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onInspectPhoto)
                    )
                } else {
                    Surface(
                        color = HighwayAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = HighwayAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.receiptPhotoSealedNotice,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            if (isOwner && expense.capturedLocationLabel.isNotBlank()) {
                LocationStampRow(
                    locationLabel = expense.capturedLocationLabel,
                    latitude = expense.capturedLatitude,
                    longitude = expense.capturedLongitude,
                    timestampText = MediaAndLocationHelper.formatTimestamp(expense.createdAt)
                )
            }
            LockedRecordBadge(role = role)
        }
    }
}

@Composable
private fun AddExpenseDialog(
    currentTripTotal: Double,
    role: UserRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, String, String, String, String, Double?, Double?, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var category by remember { mutableStateOf("Diesel") }
    var litersStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    var vendorOrPlaza by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    var receiptPhotoBase64 by remember { mutableStateOf("") }
    var locationCapture by remember { mutableStateOf(LocationCapture(label = strings.capturingGps)) }
    var confirmLockChecked by remember { mutableStateOf(false) }

    // Automatic calculator for Diesel (Liters * Rate = Amount)
    LaunchedEffect(category, litersStr, rateStr) {
        if (category == "Diesel") {
            val l = litersStr.toDoubleOrNull()
            val r = rateStr.toDoubleOrNull()
            if (l != null && r != null && l > 0 && r > 0) {
                amountStr = String.format(Locale.US, "%.2f", l * r)
            }
        }
    }

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val projectedTripTotal = currentTripTotal + enteredAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addExpenseDialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.categoryLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Diesel", "Toll", "Tax", "Maintenance", "Allowance", "Other").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.expenseCategoryLabel(cat)) }
                        )
                    }
                }

                if (category == "Diesel") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = litersStr,
                            onValueChange = { litersStr = it },
                            label = { Text(strings.litersLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = { rateStr = it },
                            label = { Text(strings.ratePerLiterLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(strings.totalAmountReq) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Built-in Quick Calculator Pad for Manager
                InlineCalculatorPanel(
                    onApplyResult = { result ->
                        amountStr = String.format(Locale.US, "%.2f", result)
                    }
                )

                // Live Trip Total Preview
                Surface(
                    color = VerifiedTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.projectedTripExpenses, style = MaterialTheme.typography.bodySmall)
                        Text(
                            MediaAndLocationHelper.formatInr(projectedTripTotal),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = VerifiedTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = vendorOrPlaza,
                    onValueChange = { vendorOrPlaza = it },
                    label = {
                        Text(
                            when (category) {
                                "Diesel" -> strings.fuelPumpName
                                "Toll" -> strings.tollPlazaName
                                "Tax" -> strings.borderStateTaxName
                                else -> strings.vendorDescName
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (category == "Diesel") {
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it },
                        label = { Text(strings.odometerLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(strings.paymentModeLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "FASTag", "UPI", "Card").forEach { m ->
                        FilterChip(
                            selected = paymentMode == m,
                            onClick = { paymentMode = m },
                            label = { Text(strings.paymentModeLabel(m)) }
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

                PhotoAndLocationCaptureSection(
                    title = strings.receiptPhotoTitle,
                    required = false,
                    photoBase64 = receiptPhotoBase64,
                    locationCapture = locationCapture,
                    onPhotoCaptured = { receiptPhotoBase64 = it },
                    onLocationCaptured = { locationCapture = it }
                )

                if (role == UserRole.MANAGER) {
                    Surface(
                        color = HighwayAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.expenseAntiTamperWarning,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (role == UserRole.MANAGER && !confirmLockChecked) {
                        confirmLockChecked = true
                    } else {
                        onSave(
                            category,
                            enteredAmount,
                            litersStr.toDoubleOrNull() ?: 0.0,
                            rateStr.toDoubleOrNull() ?: 0.0,
                            odometerStr.toDoubleOrNull() ?: 0.0,
                            vendorOrPlaza,
                            paymentMode,
                            notes,
                            receiptPhotoBase64,
                            locationCapture.latitude,
                            locationCapture.longitude,
                            locationCapture.label
                        )
                    }
                },
                enabled = !isBusy && enteredAmount > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(
                    if (role == UserRole.MANAGER && !confirmLockChecked) strings.saveAndLockBtn
                    else if (role == UserRole.MANAGER) strings.confirmLockAndSaveBtn
                    else strings.saveExpenseBtn
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
