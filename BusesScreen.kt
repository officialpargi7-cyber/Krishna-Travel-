package com.example.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bus
import com.example.data.model.PassengerBooking
import com.example.data.model.TripExpense
import com.example.data.model.UserRole
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.ui.viewmodel.UiState
import com.example.util.MediaAndLocationHelper

@Composable
fun BusesScreen(
    busesState: UiState<List<Bus>>,
    allExpenses: List<TripExpense>,
    allBookings: List<PassengerBooking>,
    role: UserRole,
    isBusy: Boolean,
    onAddBus: (String, String, Int, String, () -> Unit) -> Unit,
    onDeleteBus: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var showAddBusDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (busesState) {
            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                busesState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
            is UiState.Success -> {
                val buses = busesState.data
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (role == UserRole.OWNER) {
                        item {
                            OwnerExpenseAnalyticsCard(allExpenses = allExpenses, allBookings = allBookings)
                        }
                    }

                    if (buses.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DirectionsBus,
                                        contentDescription = null,
                                        tint = KrishnaCrimson,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(strings.noBusesAdded, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = if (role == UserRole.OWNER) strings.noBusesOwnerDesc else strings.noBusesManagerDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (role == UserRole.OWNER) {
                                        Button(
                                            onClick = { showAddBusDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
                                        ) {
                                            Text(strings.addDefaultBusBtn)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(buses, key = { it.id }) { bus ->
                            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = KrishnaCrimson.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.DirectionsBus,
                                                contentDescription = null,
                                                tint = KrishnaCrimson
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            bus.busNumber,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${bus.coachName} • ${strings.cabinsRatio(bus.totalCabins.toInt(), 30)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (bus.defaultRoute.isNotBlank()) {
                                            Text(
                                                strings.routePrefix(bus.defaultRoute),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (role == UserRole.OWNER) {
                                        IconButton(onClick = { onDeleteBus(bus.id) }) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = strings.delete,
                                                tint = MaterialTheme.colorScheme.error
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

        if (role == UserRole.OWNER) {
            FloatingActionButton(
                onClick = { showAddBusDialog = true },
                containerColor = KrishnaCrimson,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("add_bus_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.registerSleeperBus)
            }
        }
    }

    if (showAddBusDialog) {
        AddBusDialog(
            isBusy = isBusy,
            onDismiss = { showAddBusDialog = false },
            onSave = { number, name, cabins, route ->
                onAddBus(number, name, cabins, route) { showAddBusDialog = false }
            }
        )
    }
}

@Composable
private fun OwnerExpenseAnalyticsCard(
    allExpenses: List<TripExpense>,
    allBookings: List<PassengerBooking>
) {
    val strings = LocalAppStrings.current
    val diesel = allExpenses.filter { it.category == "Diesel" }.sumOf { it.amount }
    val toll = allExpenses.filter { it.category == "Toll" }.sumOf { it.amount }
    val tax = allExpenses.filter { it.category == "Tax" }.sumOf { it.amount }
    val other = allExpenses.filter { it.category !in listOf("Diesel", "Toll", "Tax") }.sumOf { it.amount }
    val pendingDues = allBookings.sumOf { (it.fareAmount - it.paidAmount).coerceAtLeast(0.0) }

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalGasStation, contentDescription = null, tint = HighwayAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.ownerExpenseBreakdown, style = MaterialTheme.typography.titleMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(strings.diesel, MediaAndLocationHelper.formatInr(diesel), KrishnaCrimson)
                MetricCol(strings.toll, MediaAndLocationHelper.formatInr(toll), HighwayAmber)
                MetricCol(strings.taxPermit, MediaAndLocationHelper.formatInr(tax), MaterialTheme.colorScheme.primary)
                MetricCol(strings.other, MediaAndLocationHelper.formatInr(other), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = VerifiedTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.passengerPendingDues, style = MaterialTheme.typography.bodySmall)
                    Text(
                        MediaAndLocationHelper.formatInr(pendingDues),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = VerifiedTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddBusDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var busNumber by remember { mutableStateOf("GJ01DZ8325") }
    var coachName by remember { mutableStateOf("Krishna 2+1 AC Sleeper") }
    var totalCabins by remember { mutableStateOf("30") }
    var defaultRoute by remember { mutableStateOf("Ahmedabad - Udaipur") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.registerSleeperBus) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it.uppercase() },
                    label = { Text(strings.busNumberPlateReq) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coachName,
                    onValueChange = { coachName = it },
                    label = { Text(strings.coachModelTypeReq) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalCabins,
                    onValueChange = { totalCabins = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.sleeperCabinsCount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultRoute,
                    onValueChange = { defaultRoute = it },
                    label = { Text(strings.primaryRoute) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cabins = totalCabins.toIntOrNull()?.coerceIn(1, 60) ?: 30
                    onSave(busNumber, coachName, cabins, defaultRoute)
                },
                enabled = !isBusy && busNumber.trim().length >= 2 && coachName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveBus)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
