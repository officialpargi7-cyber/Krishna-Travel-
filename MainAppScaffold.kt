package com.example.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.RoleBadgePill
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.LocalOnLanguageSelected
import com.example.ui.i18n.TopBarLanguageButton
import com.example.ui.onboarding.FleetOnboardingScreen
import com.example.ui.theme.AsphaltSlate
import com.example.ui.trip.TripDetailScreen
import com.example.ui.viewmodel.FleetViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: FleetViewModel,
    onSignOut: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val onLanguageSelected = LocalOnLanguageSelected.current

    val membershipsState by viewModel.membershipsState.collectAsStateWithLifecycle()
    val activeMembership by viewModel.activeMembership.collectAsStateWithLifecycle()
    val activeFleet by viewModel.activeFleet.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val busesState by viewModel.busesState.collectAsStateWithLifecycle()
    val tripsState by viewModel.tripsState.collectAsStateWithLifecycle()
    val allBookings by viewModel.allFleetBookings.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allFleetExpenses.collectAsStateWithLifecycle()
    val membersState by viewModel.fleetMembersState.collectAsStateWithLifecycle()

    val selectedTripId by viewModel.selectedTripId.collectAsStateWithLifecycle()
    val selectedTrip by viewModel.selectedTrip.collectAsStateWithLifecycle()
    val tripAttendance by viewModel.tripAttendance.collectAsStateWithLifecycle()
    val tripExpenses by viewModel.tripExpenses.collectAsStateWithLifecycle()
    val tripBookings by viewModel.tripBookings.collectAsStateWithLifecycle()
    val tripPhotos by viewModel.tripCompartmentPhotos.collectAsStateWithLifecycle()
    val tripNoFareRecords by viewModel.tripNoFareRecords.collectAsStateWithLifecycle()
    val tripMetrics by viewModel.tripMetrics.collectAsStateWithLifecycle()

    val actionFeedback by viewModel.actionFeedback.collectAsStateWithLifecycle()
    val actionMessage = remember(actionFeedback, strings) { actionFeedback?.resolve(strings) }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedNavTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    when (val memState = membershipsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(memState.message, color = MaterialTheme.colorScheme.error)
            }
            return
        }
        is UiState.Success -> {
            if (memState.data.isEmpty()) {
                FleetOnboardingScreen(
                    displayName = viewModel.displayName,
                    email = viewModel.email,
                    isBusy = isBusy,
                    actionMessage = actionMessage,
                    onUnlockWithCredentials = { username, password ->
                        viewModel.signInWithStaffSlot(username, password)
                    },
                    onSignOut = onSignOut,
                    onDismissMessage = { viewModel.clearMessage() }
                )
                return
            }
        }
    }

    if (selectedTripId != null) {
        TripDetailScreen(
            trip = selectedTrip,
            attendance = tripAttendance,
            expenses = tripExpenses,
            bookings = tripBookings,
            photos = tripPhotos,
            noFareRecords = tripNoFareRecords,
            metrics = tripMetrics,
            role = currentRole,
            isBusy = isBusy,
            onBack = { viewModel.selectTrip(null) },
            onUpdateTripStatus = viewModel::updateTripStatus,
            onDeleteTrip = viewModel::deleteTrip,
            onAddAttendance = { name, staffRole, status, photoBase64, lat, lng, loc, notes, onSaved ->
                val tId = selectedTripId ?: return@TripDetailScreen
                viewModel.addAttendance(tId, name, staffRole, status, photoBase64, lat, lng, loc, notes, onSaved)
            },
            onDeleteAttendance = viewModel::deleteAttendance,
            onAddExpense = { cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc, onSaved ->
                val tId = selectedTripId ?: return@TripDetailScreen
                viewModel.addExpense(tId, cat, amt, liters, rate, odo, vendor, mode, notes, photo, lat, lng, loc, onSaved)
            },
            onDeleteExpense = viewModel::deleteExpense,
            onAddBooking = { tId, name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes, onSaved ->
                viewModel.addBooking(tId, name, phone, cabins, boarding, dropping, fare, paid, status, mode, notes, onSaved)
            },
            onMarkBookingPaid = { booking ->
                viewModel.updateBooking(booking.copy(paidAmount = booking.fareAmount, paymentStatus = "Paid"))
            },
            onDeleteBooking = viewModel::deleteBooking,
            onUploadCabinPhoto = { cabinNum, passengerName, note, base64, lat, lng, locLabel, onSaved ->
                val tId = selectedTripId ?: return@TripDetailScreen
                viewModel.addCompartmentPhoto(tId, cabinNum, passengerName, note, base64, lat, lng, locLabel, onSaved)
            },
            onToggleCabinPhotoVerified = viewModel::toggleCabinPhotoVerified,
            onDeleteCabinPhoto = viewModel::deleteCompartmentPhoto,
            onAddNoFareRecord = { name, mobile, cabin, fare, reason, photo, lat, lng, loc, onSaved ->
                val tId = selectedTripId ?: return@TripDetailScreen
                viewModel.addNoFareRecord(tId, name, mobile, cabin, fare, reason, photo, lat, lng, loc, onSaved)
            },
            onToggleNoFareResolved = viewModel::toggleNoFareResolved,
            onDeleteNoFareRecord = viewModel::deleteNoFareRecord
        )
        return
    }

    val navItems = listOf(
        Triple(strings.navTrips, Icons.Rounded.Route, "nav_trips"),
        Triple(strings.navBuses, Icons.Rounded.DirectionsBus, "nav_buses"),
        Triple(strings.navTeam, Icons.Rounded.Group, "nav_team")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AsphaltSlate,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.krishna_icon_fg),
                                contentDescription = strings.appTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = activeFleet?.name ?: activeMembership?.fleetName ?: strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeMembership?.slotUsername?.ifBlank { null }
                                        ?: viewModel.displayName.ifBlank { viewModel.email },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        TopBarLanguageButton(
                            selected = currentLanguage,
                            onSelect = onLanguageSelected
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RoleBadgePill(role = currentRole)
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = strings.signOut
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEachIndexed { index, (label, icon, tag) ->
                            NavigationBarItem(
                                selected = selectedNavTab == index,
                                onClick = { selectedNavTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { index, (label, icon, tag) ->
                            NavigationRailItem(
                                selected = selectedNavTab == index,
                                onClick = { selectedNavTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedNavTab) {
                        0 -> TripsScreen(
                            tripsState = tripsState,
                            busesState = busesState,
                            allBookings = allBookings,
                            allExpenses = allExpenses,
                            role = currentRole,
                            isBusy = isBusy,
                            onSelectTrip = { id -> viewModel.selectTrip(id) },
                            onCreateTrip = { bus, origin, dest, date, dep, driver, conductor, notes, onDone ->
                                viewModel.createTrip(bus, origin, dest, date, dep, driver, conductor, notes) {
                                    onDone()
                                }
                            },
                            onNavigateToBuses = { selectedNavTab = 1 }
                        )
                        1 -> BusesScreen(
                            busesState = busesState,
                            allExpenses = allExpenses,
                            allBookings = allBookings,
                            role = currentRole,
                            isBusy = isBusy,
                            onAddBus = viewModel::addBus,
                            onDeleteBus = viewModel::deleteBus
                        )
                        else -> TeamScreen(
                            fleet = activeFleet,
                            currentUserId = viewModel.currentUserId,
                            currentRole = currentRole,
                            activeSlotUsername = activeMembership?.slotUsername.orEmpty(),
                            membersState = membersState,
                            onUpdateMemberRole = viewModel::updateMemberRole,
                            onSyncAccess = viewModel::syncAccessIfOwner
                        )
                    }
                }
            }
        }
    }
}
