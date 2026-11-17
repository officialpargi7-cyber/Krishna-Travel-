package com.example.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Bus
import com.example.data.model.PassengerBooking
import com.example.data.model.Trip
import com.example.data.model.TripExpense
import com.example.data.model.UserRole
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsphaltSlate
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.ui.viewmodel.UiState
import com.example.util.MediaAndLocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripsScreen(
    tripsState: UiState<List<Trip>>,
    busesState: UiState<List<Bus>>,
    allBookings: List<PassengerBooking>,
    allExpenses: List<TripExpense>,
    role: UserRole,
    isBusy: Boolean,
    onSelectTrip: (String) -> Unit,
    onCreateTrip: (Bus, String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onNavigateToBuses: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    val buses = (busesState as? UiState.Success)?.data ?: emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        when (tripsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(
                    text = tripsState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
            is UiState.Success -> {
                val trips = tripsState.data
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (role == UserRole.OWNER) {
                        item {
                            val totalCollected = allBookings.sumOf { it.paidAmount }
                            val totalExp = allExpenses.sumOf { it.amount }
                            val net = totalCollected - totalExp
                            OwnerFleetSummaryCard(
                                tripCount = trips.size,
                                busCount = buses.size,
                                totalRevenue = totalCollected,
                                totalExpenses = totalExp,
                                netProfit = net
                            )
                        }
                    } else {
                        item {
                            Surface(
                                color = VerifiedTeal.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = VerifiedTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = strings.managerDashboardNotice,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    if (trips.isEmpty()) {
                        item {
                            EmptyTripsCard(
                                hasBuses = buses.isNotEmpty(),
                                isOwner = role == UserRole.OWNER,
                                onAddBus = onNavigateToBuses,
                                onStartTrip = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(trips, key = { it.id }) { trip ->
                            val tripRevenue = allBookings.filter { it.tripId == trip.id }.sumOf { it.paidAmount }
                            val tripExp = allExpenses.filter { it.tripId == trip.id }.sumOf { it.amount }
                            val tripBookedCabins = allBookings.filter { it.tripId == trip.id }
                                .flatMap { it.cabinNumbers }.toSet().size
                            TripCard(
                                trip = trip,
                                bookedCabins = tripBookedCabins,
                                totalRevenue = tripRevenue,
                                totalExpenses = tripExp,
                                isOwner = role == UserRole.OWNER,
                                onClick = { onSelectTrip(trip.id) }
                            )
                        }
                    }
                }
            }
        }

        if (buses.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = KrishnaCrimson,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("add_trip_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.startNewBusTrip)
            }
        }
    }

    if (showCreateDialog && buses.isNotEmpty()) {
        CreateTripDialog(
            buses = buses,
            isBusy = isBusy,
            role = role,
            onDismiss = { showCreateDialog = false },
            onCreate = { bus, origin, dest, date, dep, driver, conductor, notes ->
                onCreateTrip(bus, origin, dest, date, dep, driver, conductor, notes) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun OwnerFleetSummaryCard(
    tripCount: Int,
    busCount: Int,
    totalRevenue: Double,
    totalExpenses: Double,
    netProfit: Double
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AsphaltSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AsphaltSlate, Color(0xFF311018))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = HighwayAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.ownerLiveFleetLedger,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = strings.busesTripsCount(busCount, tripCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.collected, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            MediaAndLocationHelper.formatInr(totalRevenue),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF5EEAD4),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(strings.expenses, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            MediaAndLocationHelper.formatInr(totalExpenses),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(strings.netBalance, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            MediaAndLocationHelper.formatInr(netProfit),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = HighwayAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    bookedCabins: Int,
    totalRevenue: Double,
    totalExpenses: Double,
    isOwner: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val statusColor = when (trip.status) {
        "completed" -> VerifiedTeal
        "scheduled" -> HighwayAmber
        else -> KrishnaCrimson
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("trip_card_${trip.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = KrishnaCrimson.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.DirectionsBus,
                                contentDescription = null,
                                tint = KrishnaCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                trip.busNumber,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = KrishnaCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.cabinsRatio(bookedCabins, 30),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = strings.tripStatusLabel(trip.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${trip.origin} → ${trip.destination}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${trip.tripDate} • ${trip.departureTime} • ${strings.driverLabel(trip.driverName)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.tripBillsLabel(MediaAndLocationHelper.formatInr(totalRevenue)),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = VerifiedTeal
                )
                if (isOwner) {
                    Text(
                        text = strings.expensesLabel(MediaAndLocationHelper.formatInr(totalExpenses)),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = KrishnaCrimson
                    )
                    Text(
                        text = strings.netLabel(MediaAndLocationHelper.formatInr(totalRevenue - totalExpenses)),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = HighwayAmber,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = strings.managerLabel(trip.createdByName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTripsCard(
    hasBuses: Boolean,
    isOwner: Boolean,
    onAddBus: () -> Unit,
    onStartTrip: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsBus,
                contentDescription = null,
                tint = KrishnaCrimson,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (!hasBuses) strings.addBusFirstTitle else strings.noTripsTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (!hasBuses) {
                    if (isOwner) strings.addBusFirstOwnerBody else strings.addBusFirstManagerBody
                } else {
                    strings.startTripBody
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasBuses && isOwner) {
                Button(
                    onClick = onAddBus,
                    colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
                ) {
                    Text(strings.addBusNow)
                }
            } else if (hasBuses) {
                Button(
                    onClick = onStartTrip,
                    colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
                ) {
                    Text(strings.startFirstTrip)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripDialog(
    buses: List<Bus>,
    isBusy: Boolean,
    role: UserRole,
    onDismiss: () -> Unit,
    onCreate: (Bus, String, String, String, String, String, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedBus by remember { mutableStateOf(buses.first()) }
    var busDropdownExpanded by remember { mutableStateOf(false) }
    val defaultParts = selectedBus.defaultRoute.split("-", "->").map { it.trim() }
    var origin by remember { mutableStateOf(defaultParts.getOrNull(0)?.ifBlank { "Ahmedabad" } ?: "Ahmedabad") }
    var destination by remember { mutableStateOf(defaultParts.getOrNull(1)?.ifBlank { "Udaipur" } ?: "Udaipur") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var tripDate by remember { mutableStateOf(today) }
    var departureTime by remember { mutableStateOf("21:30") }
    var driverName by remember { mutableStateOf("") }
    var conductorName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.startNewBusTrip) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = busDropdownExpanded,
                    onExpandedChange = { busDropdownExpanded = !busDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedBus.busNumber} (${strings.cabinsRatio(selectedBus.totalCabins.toInt(), 30)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectBusLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = busDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = busDropdownExpanded,
                        onDismissRequest = { busDropdownExpanded = false }
                    ) {
                        buses.forEach { bus ->
                            DropdownMenuItem(
                                text = { Text("${bus.busNumber} • ${bus.coachName}") },
                                onClick = {
                                    selectedBus = bus
                                    busDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text(strings.fromLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text(strings.toLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tripDate,
                        onValueChange = { tripDate = it },
                        label = { Text(strings.dateLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = departureTime,
                        onValueChange = { departureTime = it },
                        label = { Text(strings.depTimeLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text(strings.driverNameReq) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conductorName,
                    onValueChange = { conductorName = it },
                    label = { Text(strings.conductorHelperLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.tripNotesLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (role == UserRole.MANAGER) {
                    Text(
                        text = strings.managerTripLockWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = HighwayAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(selectedBus, origin, destination, tripDate, departureTime, driverName, conductorName, notes)
                },
                enabled = !isBusy && origin.isNotBlank() && destination.isNotBlank() && driverName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KrishnaCrimson)
            ) {
                Text(strings.saveAndLockTrip)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
