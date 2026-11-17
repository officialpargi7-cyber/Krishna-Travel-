package com.example.ui.trip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CompartmentPhoto
import com.example.data.model.NoFareRecord
import com.example.data.model.PassengerBooking
import com.example.data.model.StaffAttendance
import com.example.data.model.Trip
import com.example.data.model.TripExpense
import com.example.data.model.TripMetrics
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadgePill
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.LocalOnLanguageSelected
import com.example.ui.i18n.TopBarLanguageButton
import com.example.ui.theme.AsphaltSlate
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.VerifiedTeal
import com.example.util.MediaAndLocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip?,
    attendance: List<StaffAttendance>,
    expenses: List<TripExpense>,
    bookings: List<PassengerBooking>,
    photos: List<CompartmentPhoto>,
    noFareRecords: List<NoFareRecord>,
    metrics: TripMetrics,
    role: UserRole,
    isBusy: Boolean,
    onBack: () -> Unit,
    onUpdateTripStatus: (Trip, String) -> Unit,
    onDeleteTrip: (String, () -> Unit) -> Unit,
    onAddAttendance: (String, String, String, String, Double?, Double?, String, String, () -> Unit) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onAddExpense: (String, Double, Double, Double, Double, String, String, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddBooking: (String, String, String, List<Int>, String, String, Double, Double, String, String, String, () -> Unit) -> Unit,
    onMarkBookingPaid: (PassengerBooking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onUploadCabinPhoto: (Int, String, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onToggleCabinPhotoVerified: (CompartmentPhoto) -> Unit,
    onDeleteCabinPhoto: (String) -> Unit,
    onAddNoFareRecord: (String, String, Int, Double, String, String, Double?, Double?, String, () -> Unit) -> Unit,
    onToggleNoFareResolved: (NoFareRecord) -> Unit,
    onDeleteNoFareRecord: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val onLanguageSelected = LocalOnLanguageSelected.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteTripConfirm by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    if (trip == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isOwner = role == UserRole.OWNER
    val tabTitles = listOf(
        strings.tabTripBills(bookings.size),
        strings.tabCabins(metrics.cabinPhotosCount),
        strings.tabNoFare(noFareRecords.size),
        strings.tabDieselExp(expenses.size),
        strings.tabStaff(attendance.size)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        // Dark Header Card
        Surface(
            color = AsphaltSlate,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("trip_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.navTrips,
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${trip.origin} → ${trip.destination}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${trip.busNumber} • ${trip.tripDate} ${trip.departureTime}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    TopBarLanguageButton(
                        selected = currentLanguage,
                        onSelect = onLanguageSelected
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadgePill(role = role)
                    if (isOwner) {
                        IconButton(
                            onClick = {
                                MediaAndLocationHelper.shareTripReport(
                                    context = context,
                                    trip = trip,
                                    attendance = attendance,
                                    expenses = expenses,
                                    bookings = bookings,
                                    photos = photos,
                                    noFareRecords = noFareRecords
                                )
                            },
                            modifier = Modifier.testTag("share_trip_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = strings.shareTripReportDesc,
                                tint = HighwayAmber
                            )
                        }
                        IconButton(onClick = { showDeleteTripConfirm = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = strings.deleteTripTitle,
                                tint = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                // KPI Summary Strip
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.tripBillsKpi, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                MediaAndLocationHelper.formatInr(metrics.totalCollected),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF5EEAD4),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isOwner) {
                            Column {
                                Text(strings.expenses, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    MediaAndLocationHelper.formatInr(metrics.totalExpenses),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(strings.netProfitKpi, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    MediaAndLocationHelper.formatInr(metrics.netProfit),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HighwayAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Column {
                                Text(strings.pendingDueKpi, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    MediaAndLocationHelper.formatInr(metrics.totalPending),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HighwayAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(strings.tripManagerKpi, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    trip.createdByName.ifBlank { strings.roleManager },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.cabinsKpi, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                "${metrics.cabinPhotosCount}/30",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isOwner) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("scheduled", "in_progress", "completed").forEach { wire ->
                            FilterChip(
                                selected = trip.status == wire,
                                onClick = { onUpdateTripStatus(trip, wire) },
                                label = { Text(strings.tripStatusLabel(wire), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = HighwayAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.managerTripBanner,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("trip_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTabIndex) {
                0 -> TripBookingsTab(
                    trip = trip,
                    bookings = bookings,
                    metrics = metrics,
                    role = role,
                    isBusy = isBusy,
                    onAddBooking = { name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes, onSaved ->
                        onAddBooking(trip.id, name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes, onSaved)
                    },
                    onMarkPaid = onMarkBookingPaid,
                    onDeleteBooking = onDeleteBooking
                )
                1 -> TripCabinsTab(
                    trip = trip,
                    photos = photos,
                    bookings = bookings,
                    role = role,
                    isBusy = isBusy,
                    onUploadCabinPhoto = onUploadCabinPhoto,
                    onToggleVerified = onToggleCabinPhotoVerified,
                    onDeletePhoto = onDeleteCabinPhoto
                )
                2 -> TripNoFareTab(
                    records = noFareRecords,
                    role = role,
                    isBusy = isBusy,
                    onAddRecord = onAddNoFareRecord,
                    onToggleResolved = onToggleNoFareResolved,
                    onDeleteRecord = onDeleteNoFareRecord
                )
                3 -> TripExpensesTab(
                    expenses = expenses,
                    metrics = metrics,
                    role = role,
                    isBusy = isBusy,
                    onAddExpense = { cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc, onSaved ->
                        onAddExpense(cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc, onSaved)
                    },
                    onDeleteExpense = onDeleteExpense
                )
                4 -> TripAttendanceTab(
                    attendance = attendance,
                    role = role,
                    isBusy = isBusy,
                    onAddAttendance = onAddAttendance,
                    onDeleteAttendance = onDeleteAttendance
                )
            }
        }
    }

    if (showDeleteTripConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteTripConfirm = false },
            title = { Text(strings.deleteTripTitle) },
            text = { Text(strings.deleteTripBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTripConfirm = false
                        onDeleteTrip(trip.id, onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTripConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}
