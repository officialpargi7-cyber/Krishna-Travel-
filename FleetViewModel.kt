package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Bus
import com.example.data.model.CompartmentPhoto
import com.example.data.model.Fleet
import com.example.data.model.FleetMember
import com.example.data.model.NoFareRecord
import com.example.data.model.PassengerBooking
import com.example.data.model.StaffAttendance
import com.example.data.model.Trip
import com.example.data.model.TripExpense
import com.example.data.model.TripMetrics
import com.example.data.model.UserMembership
import com.example.data.model.UserRole
import com.example.data.model.computeTripMetrics
import com.example.data.repository.FleetRepository
import com.example.ui.i18n.AppStrings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ActionFeedback {
    data class Text(val raw: String) : ActionFeedback
    data class JoinedSlot(val slotUsername: String, val role: UserRole) : ActionFeedback
    data object InvalidCredentials : ActionFeedback
    data object OwnerMustInitFirst : ActionFeedback
    data class RoleUpdated(val name: String, val role: UserRole) : ActionFeedback
    data class BusAdded(val busNumber: String) : ActionFeedback
    data object BusRemoved : ActionFeedback
    data object TripCreatedLocked : ActionFeedback
    data class TripStatusUpdated(val wireStatus: String) : ActionFeedback
    data object TripDeleted : ActionFeedback
    data object AttendanceSavedLocked : ActionFeedback
    data object AttendanceDeleted : ActionFeedback
    data class ExpenseSavedLocked(val category: String) : ActionFeedback
    data object ExpenseUpdated : ActionFeedback
    data object ExpenseDeleted : ActionFeedback
    data object BookingSavedLocked : ActionFeedback
    data object BookingUpdated : ActionFeedback
    data object BookingDeleted : ActionFeedback
    data class CabinPhotoSavedLocked(val cabinLabel: String) : ActionFeedback
    data object CabinVerified : ActionFeedback
    data object CabinVerificationRemoved : ActionFeedback
    data object CabinPhotoDeleted : ActionFeedback
    data object NoFareSavedLocked : ActionFeedback
    data object MarkedResolved : ActionFeedback
    data object MarkedPending : ActionFeedback
    data object RecordDeleted : ActionFeedback
    data object OwnerOnlyError : ActionFeedback

    fun resolve(s: AppStrings): String = when (this) {
        is Text -> raw
        is JoinedSlot -> s.msgJoinedFleet(slotUsername, s.roleLabel(role.wireValue))
        InvalidCredentials -> s.invalidStaffCredentials
        OwnerMustInitFirst -> s.ownerMustInitFirst
        is RoleUpdated -> s.msgRoleUpdated(name, s.roleLabel(role.wireValue))
        is BusAdded -> s.msgBusAdded(busNumber)
        BusRemoved -> s.msgBusRemoved
        TripCreatedLocked -> s.msgTripCreatedLocked
        is TripStatusUpdated -> s.msgTripStatusUpdated(s.tripStatusLabel(wireStatus))
        TripDeleted -> s.msgTripDeleted
        AttendanceSavedLocked -> s.msgAttendanceSavedLocked
        AttendanceDeleted -> s.msgAttendanceDeleted
        is ExpenseSavedLocked -> s.msgExpenseSavedLocked(s.expenseCategoryLabel(category))
        ExpenseUpdated -> s.msgExpenseUpdated
        ExpenseDeleted -> s.msgExpenseDeleted
        BookingSavedLocked -> s.msgBookingSavedLocked
        BookingUpdated -> s.msgBookingUpdated
        BookingDeleted -> s.msgBookingDeleted
        is CabinPhotoSavedLocked -> s.msgCabinPhotoSavedLocked(cabinLabel)
        CabinVerified -> s.msgCabinVerified
        CabinVerificationRemoved -> s.msgVerificationRemoved
        CabinPhotoDeleted -> s.msgCabinPhotoDeleted
        NoFareSavedLocked -> s.msgNoFareSavedLocked
        MarkedResolved -> s.msgMarkedResolved
        MarkedPending -> s.msgMarkedPending
        RecordDeleted -> s.msgRecordDeleted
        OwnerOnlyError -> s.msgOwnerOnlyError
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FleetViewModel(
    private val repository: FleetRepository,
    val currentUserId: String,
    val displayName: String,
    val email: String
) : ViewModel() {

    private val _selectedFleetId = MutableStateFlow<String?>(null)
    val selectedFleetId: StateFlow<String?> = _selectedFleetId.asStateFlow()

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    private val _actionFeedback = MutableStateFlow<ActionFeedback?>(null)
    val actionFeedback: StateFlow<ActionFeedback?> = _actionFeedback.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(displayName, email).onSuccess { profile ->
                if (_selectedFleetId.value == null && profile.activeFleetId.isNotBlank()) {
                    _selectedFleetId.value = profile.activeFleetId
                }
            }
        }
    }

    private fun <T> Flow<T>.toUiState(tagLabel: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $tagLabel"))
            }

    val membershipsState: StateFlow<UiState<List<UserMembership>>> =
        repository.observeMemberships(currentUserId)
            .toUiState("memberships")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activeMembership: StateFlow<UserMembership?> =
        combine(membershipsState, _selectedFleetId) { state, selectedId ->
            val list = (state as? UiState.Success)?.data ?: emptyList()
            list.firstOrNull { it.fleetId == selectedId } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val currentRole: StateFlow<UserRole> =
        activeMembership.map { it?.userRole ?: UserRole.MANAGER }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserRole.MANAGER)

    val activeFleet: StateFlow<Fleet?> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null) flowOf(null)
            else repository.observeFleet(membership.fleetId)
                .catch { emit(null) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        // Automatically propagate new member UIDs onto historical records whenever Owner's fleet updates
        viewModelScope.launch {
            combine(activeFleet, currentRole) { fleet, role -> fleet to role }
                .collect { (fleet, role) ->
                    if (fleet != null && role == UserRole.OWNER && fleet.memberUids.isNotEmpty()) {
                        repository.syncMemberUidsAcrossFleet(fleet.id, fleet.memberUids, fleet.ownerUids)
                    }
                }
        }
    }

    val busesState: StateFlow<UiState<List<Bus>>> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null) flowOf(UiState.Success(emptyList()))
            else repository.observeBuses(membership.fleetId, currentUserId).toUiState("buses")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val tripsState: StateFlow<UiState<List<Trip>>> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null) flowOf(UiState.Success(emptyList()))
            else repository.observeTrips(membership.fleetId, currentUserId).toUiState("trips")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allFleetBookings: StateFlow<List<PassengerBooking>> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null) flowOf(emptyList())
            else repository.observeAllFleetBookings(membership.fleetId, currentUserId).catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allFleetExpenses: StateFlow<List<TripExpense>> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null) flowOf(emptyList())
            else repository.observeAllFleetExpenses(membership.fleetId, currentUserId).catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val fleetMembersState: StateFlow<UiState<List<FleetMember>>> =
        activeMembership.flatMapLatest { membership ->
            if (membership == null || membership.userRole != UserRole.OWNER) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeFleetMembersForOwner(membership.fleetId).toUiState("team members")
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    // Selected Trip Streams
    val selectedTrip: StateFlow<Trip?> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(null)
                else repository.observeTrip(membership.fleetId, tripId).catch { emit(null) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val tripAttendance: StateFlow<List<StaffAttendance>> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeAttendance(membership.fleetId, tripId, currentUserId).catch { emit(emptyList()) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripExpenses: StateFlow<List<TripExpense>> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeExpenses(membership.fleetId, tripId, currentUserId).catch { emit(emptyList()) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripBookings: StateFlow<List<PassengerBooking>> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeBookings(membership.fleetId, tripId, currentUserId).catch { emit(emptyList()) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripCompartmentPhotos: StateFlow<List<CompartmentPhoto>> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeCompartmentPhotos(membership.fleetId, tripId, currentUserId).catch { emit(emptyList()) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripNoFareRecords: StateFlow<List<NoFareRecord>> =
        combine(activeMembership, _selectedTripId) { membership, tripId -> membership to tripId }
            .flatMapLatest { (membership, tripId) ->
                if (membership == null || tripId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeNoFareRecords(membership.fleetId, tripId, currentUserId).catch { emit(emptyList()) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripMetrics: StateFlow<TripMetrics> =
        combine(
            tripAttendance,
            tripExpenses,
            tripBookings,
            tripCompartmentPhotos,
            tripNoFareRecords
        ) { att, exp, book, photos, noFare ->
            computeTripMetrics(att, exp, book, photos, noFare)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TripMetrics())

    fun clearMessage() {
        _actionFeedback.value = null
    }

    fun selectFleet(fleetId: String) {
        _selectedFleetId.value = fleetId
        _selectedTripId.value = null
        viewModelScope.launch {
            repository.setActiveFleetId(fleetId)
        }
    }

    fun selectTrip(tripId: String?) {
        _selectedTripId.value = tripId
    }

    fun syncAccessIfOwner() {
        val fleet = activeFleet.value ?: return
        if (currentRole.value != UserRole.OWNER) return
        viewModelScope.launch {
            repository.syncMemberUidsAcrossFleet(fleet.id, fleet.memberUids, fleet.ownerUids)
        }
    }

    fun signInWithStaffSlot(username: String, password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.signInWithStaffSlot(username, password, displayName, email)
                .onSuccess { membership ->
                    _selectedFleetId.value = membership.fleetId
                    _actionFeedback.value = ActionFeedback.JoinedSlot(membership.slotUsername, membership.userRole)
                    onSuccess()
                }
                .onFailure { err ->
                    _actionFeedback.value = when (err) {
                        is FleetRepository.InvalidStaffCredentialsException -> ActionFeedback.InvalidCredentials
                        is FleetRepository.OwnerMustInitializeException -> ActionFeedback.OwnerMustInitFirst
                        else -> ActionFeedback.Text(err.localizedMessage ?: "Access denied")
                    }
                }
            _isBusy.value = false
        }
    }

    fun updateMemberRole(member: FleetMember, newRole: UserRole) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateMemberRole(fleet, member, newRole)
                .onSuccess { _actionFeedback.value = ActionFeedback.RoleUpdated(member.displayName, newRole) }
                .onFailure { _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to update role") }
            _isBusy.value = false
        }
    }

    fun addBus(busNumber: String, coachName: String, totalCabins: Int, defaultRoute: String, onSuccess: () -> Unit = {}) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.addBus(fleet, busNumber, coachName, totalCabins, defaultRoute)
                .onSuccess {
                    _actionFeedback.value = ActionFeedback.BusAdded(busNumber)
                    onSuccess()
                }
                .onFailure { _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to add bus") }
            _isBusy.value = false
        }
    }

    fun deleteBus(busId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteBus(fleet.id, busId)
                .onSuccess { _actionFeedback.value = ActionFeedback.BusRemoved }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun createTrip(
        bus: Bus,
        origin: String,
        destination: String,
        tripDate: String,
        departureTime: String,
        driverName: String,
        conductorName: String,
        notes: String,
        onCreated: (String) -> Unit = {}
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = activeMembership.value?.slotUsername?.ifBlank { null }
            ?: displayName.ifBlank { email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.createTrip(
                fleet = fleet,
                bus = bus,
                origin = origin,
                destination = destination,
                tripDate = tripDate,
                departureTime = departureTime,
                driverName = driverName,
                conductorName = conductorName,
                notes = notes,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess { tripId ->
                _actionFeedback.value = ActionFeedback.TripCreatedLocked
                onCreated(tripId)
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to create trip")
            }
            _isBusy.value = false
        }
    }

    fun updateTripStatus(trip: Trip, newStatus: String) {
        viewModelScope.launch {
            repository.updateTrip(trip.copy(status = newStatus))
                .onSuccess { _actionFeedback.value = ActionFeedback.TripStatusUpdated(newStatus) }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun deleteTrip(tripId: String, onDeleted: () -> Unit) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteTrip(fleet.id, tripId)
                .onSuccess {
                    _selectedTripId.value = null
                    _actionFeedback.value = ActionFeedback.TripDeleted
                    onDeleted()
                }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun addAttendance(
        tripId: String,
        staffName: String,
        staffRole: String,
        status: String,
        photoBase64: String,
        lat: Double?,
        lng: Double?,
        locationLabel: String,
        notes: String,
        onSaved: () -> Unit
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = displayName.ifBlank { activeMembership.value?.slotUsername ?: email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.addAttendance(
                fleet = fleet,
                tripId = tripId,
                staffName = staffName,
                staffRole = staffRole,
                status = status,
                photoBase64 = photoBase64,
                capturedLatitude = lat,
                capturedLongitude = lng,
                capturedLocationLabel = locationLabel,
                notes = notes,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess {
                _actionFeedback.value = ActionFeedback.AttendanceSavedLocked
                onSaved()
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to save attendance")
            }
            _isBusy.value = false
        }
    }

    fun deleteAttendance(attendanceId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteAttendance(fleet.id, attendanceId)
                .onSuccess { _actionFeedback.value = ActionFeedback.AttendanceDeleted }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun addExpense(
        tripId: String,
        category: String,
        amount: Double,
        liters: Double,
        ratePerLiter: Double,
        odometerKm: Double,
        vendorOrPlaza: String,
        paymentMode: String,
        notes: String,
        receiptPhotoBase64: String,
        lat: Double?,
        lng: Double?,
        locationLabel: String,
        onSaved: () -> Unit
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = displayName.ifBlank { activeMembership.value?.slotUsername ?: email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.addExpense(
                fleet = fleet,
                tripId = tripId,
                category = category,
                amount = amount,
                liters = liters,
                ratePerLiter = ratePerLiter,
                odometerKm = odometerKm,
                vendorOrPlaza = vendorOrPlaza,
                paymentMode = paymentMode,
                notes = notes,
                receiptPhotoBase64 = receiptPhotoBase64,
                capturedLatitude = lat,
                capturedLongitude = lng,
                capturedLocationLabel = locationLabel,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess {
                _actionFeedback.value = ActionFeedback.ExpenseSavedLocked(category)
                onSaved()
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to save expense")
            }
            _isBusy.value = false
        }
    }

    fun updateExpense(expense: TripExpense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
                .onSuccess { _actionFeedback.value = ActionFeedback.ExpenseUpdated }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun deleteExpense(expenseId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteExpense(fleet.id, expenseId)
                .onSuccess { _actionFeedback.value = ActionFeedback.ExpenseDeleted }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun addBooking(
        tripId: String,
        passengerName: String,
        passengerPhone: String,
        cabinNumbers: List<Int>,
        boardingPoint: String,
        droppingPoint: String,
        fareAmount: Double,
        paidAmount: Double,
        paymentStatus: String,
        paymentMode: String,
        notes: String,
        onSaved: () -> Unit
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = displayName.ifBlank { activeMembership.value?.slotUsername ?: email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.addBooking(
                fleet = fleet,
                tripId = tripId,
                passengerName = passengerName,
                passengerPhone = passengerPhone,
                cabinNumbers = cabinNumbers,
                boardingPoint = boardingPoint,
                droppingPoint = droppingPoint,
                fareAmount = fareAmount,
                paidAmount = paidAmount,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                notes = notes,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess {
                _actionFeedback.value = ActionFeedback.BookingSavedLocked
                onSaved()
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to save booking")
            }
            _isBusy.value = false
        }
    }

    fun updateBooking(booking: PassengerBooking) {
        viewModelScope.launch {
            repository.updateBooking(booking)
                .onSuccess { _actionFeedback.value = ActionFeedback.BookingUpdated }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun deleteBooking(bookingId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteBooking(fleet.id, bookingId)
                .onSuccess { _actionFeedback.value = ActionFeedback.BookingDeleted }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun addCompartmentPhoto(
        tripId: String,
        cabinNumber: Int,
        passengerName: String,
        conditionNote: String,
        photoBase64: String,
        lat: Double?,
        lng: Double?,
        locationLabel: String,
        onSaved: () -> Unit
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = displayName.ifBlank { activeMembership.value?.slotUsername ?: email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.addCompartmentPhoto(
                fleet = fleet,
                tripId = tripId,
                cabinNumber = cabinNumber,
                passengerName = passengerName,
                conditionNote = conditionNote,
                photoBase64 = photoBase64,
                capturedLatitude = lat,
                capturedLongitude = lng,
                capturedLocationLabel = locationLabel,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess {
                _actionFeedback.value = ActionFeedback.CabinPhotoSavedLocked("C${cabinNumber.toString().padStart(2, '0')}")
                onSaved()
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to upload cabin photo")
            }
            _isBusy.value = false
        }
    }

    fun toggleCabinPhotoVerified(photo: CompartmentPhoto) {
        viewModelScope.launch {
            repository.updateCompartmentPhoto(photo.copy(verifiedByOwner = !photo.verifiedByOwner))
                .onSuccess {
                    _actionFeedback.value = if (!photo.verifiedByOwner) ActionFeedback.CabinVerified else ActionFeedback.CabinVerificationRemoved
                }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun deleteCompartmentPhoto(photoId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteCompartmentPhoto(fleet.id, photoId)
                .onSuccess { _actionFeedback.value = ActionFeedback.CabinPhotoDeleted }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun addNoFareRecord(
        tripId: String,
        personName: String,
        mobileNumber: String,
        cabinNumber: Int,
        expectedFare: Double,
        reasonOrNote: String,
        photoBase64: String,
        lat: Double?,
        lng: Double?,
        locationLabel: String,
        onSaved: () -> Unit
    ) {
        val fleet = activeFleet.value ?: return
        val creatorName = displayName.ifBlank { activeMembership.value?.slotUsername ?: email }
        viewModelScope.launch {
            _isBusy.value = true
            repository.addNoFareRecord(
                fleet = fleet,
                tripId = tripId,
                personName = personName,
                mobileNumber = mobileNumber,
                cabinNumber = cabinNumber,
                expectedFare = expectedFare,
                reasonOrNote = reasonOrNote,
                photoBase64 = photoBase64,
                capturedLatitude = lat,
                capturedLongitude = lng,
                capturedLocationLabel = locationLabel,
                createdByName = creatorName,
                createdByRole = currentRole.value
            ).onSuccess {
                _actionFeedback.value = ActionFeedback.NoFareSavedLocked
                onSaved()
            }.onFailure {
                _actionFeedback.value = ActionFeedback.Text(it.localizedMessage ?: "Failed to save record")
            }
            _isBusy.value = false
        }
    }

    fun toggleNoFareResolved(record: NoFareRecord) {
        viewModelScope.launch {
            repository.updateNoFareRecord(record.copy(resolvedByOwner = !record.resolvedByOwner))
                .onSuccess {
                    _actionFeedback.value = if (!record.resolvedByOwner) ActionFeedback.MarkedResolved else ActionFeedback.MarkedPending
                }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    fun deleteNoFareRecord(recordId: String) {
        val fleet = activeFleet.value ?: return
        viewModelScope.launch {
            repository.deleteNoFareRecord(fleet.id, recordId)
                .onSuccess { _actionFeedback.value = ActionFeedback.RecordDeleted }
                .onFailure { _actionFeedback.value = ActionFeedback.OwnerOnlyError }
        }
    }

    private companion object {
        const val TAG = "FleetVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
