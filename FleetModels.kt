package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val wireValue: String, val label: String) {
    OWNER("owner", "Owner"),
    MANAGER("manager", "Manager");

    companion object {
        fun fromWire(value: String?): UserRole =
            if (value.equals("owner", ignoreCase = true)) OWNER else MANAGER
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeFleetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserMembership(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val fleetName: String = "",
    val userId: String = "",
    val role: String = "manager",
    val slotUsername: String = "",
    val slotAuthDigest: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val userRole: UserRole get() = UserRole.fromWire(role)
}

@IgnoreExtraProperties
data class FleetAccessRegistry(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val fleetName: String = "",
    val initializedBy: String = "",
    val ownerUids: List<String> = emptyList(),
    val slotUsername: String = "",
    val slotAuthDigest: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Fleet(
    @DocumentId val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val ownerUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FleetMember(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "manager",
    val slotUsername: String = "",
    val slotAuthDigest: String = "",
    val ownerUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val userRole: UserRole get() = UserRole.fromWire(role)
}

@IgnoreExtraProperties
data class Bus(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val busNumber: String = "",
    val coachName: String = "",
    val totalCabins: Long = 30L,
    val defaultRoute: String = "",
    val active: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Trip(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val busId: String = "",
    val busNumber: String = "",
    val routeName: String = "",
    val origin: String = "",
    val destination: String = "",
    val tripDate: String = "",
    val departureTime: String = "",
    val driverName: String = "",
    val conductorName: String = "",
    val status: String = "in_progress",
    val notes: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StaffAttendance(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val tripId: String = "",
    val staffName: String = "",
    val staffRole: String = "Driver",
    val status: String = "Present",
    val photoBase64: String = "",
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val capturedLocationLabel: String = "",
    val notes: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TripExpense(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val tripId: String = "",
    val category: String = "Diesel",
    val amount: Double = 0.0,
    val liters: Double = 0.0,
    val ratePerLiter: Double = 0.0,
    val odometerKm: Double = 0.0,
    val vendorOrPlaza: String = "",
    val paymentMode: String = "Cash",
    val notes: String = "",
    val receiptPhotoBase64: String = "",
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val capturedLocationLabel: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PassengerBooking(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val tripId: String = "",
    val invoiceNumber: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",
    val cabinNumbers: List<Long> = emptyList(),
    val berthNumbers: String = "",
    val seatCount: Long = 1L,
    val boardingPoint: String = "",
    val droppingPoint: String = "",
    val fareAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "Paid",
    val paymentMode: String = "Cash",
    val notes: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompartmentPhoto(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val tripId: String = "",
    val cabinNumber: Long = 1L,
    val berthLabel: String = "",
    val passengerName: String = "",
    val conditionNote: String = "",
    val photoBase64: String = "",
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val capturedLocationLabel: String = "",
    val verifiedByOwner: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NoFareRecord(
    @DocumentId val id: String = "",
    val fleetId: String = "",
    val tripId: String = "",
    val personName: String = "",
    val mobileNumber: String = "",
    val cabinNumber: Long = 0L,
    val expectedFare: Double = 0.0,
    val reasonOrNote: String = "",
    val photoBase64: String = "",
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val capturedLocationLabel: String = "",
    val resolvedByOwner: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdByRole: String = "manager",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class TripMetrics(
    val totalBookings: Int = 0,
    val totalBookedCabins: Int = 0,
    val totalFare: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val dieselExpense: Double = 0.0,
    val dieselLiters: Double = 0.0,
    val tollExpense: Double = 0.0,
    val taxExpense: Double = 0.0,
    val otherExpense: Double = 0.0,
    val attendanceCount: Int = 0,
    val cabinPhotosCount: Int = 0,
    val noFareCount: Int = 0,
    val netProfit: Double = 0.0
)

fun computeTripMetrics(
    attendance: List<StaffAttendance>,
    expenses: List<TripExpense>,
    bookings: List<PassengerBooking>,
    photos: List<CompartmentPhoto>,
    noFareRecords: List<NoFareRecord> = emptyList()
): TripMetrics {
    val totalFare = bookings.sumOf { it.fareAmount }
    val totalCollected = bookings.sumOf { it.paidAmount }
    val totalPending = (totalFare - totalCollected).coerceAtLeast(0.0)
    val bookedCabins = bookings.flatMap { it.cabinNumbers }.toSet().size
    val diesel = expenses.filter { it.category == "Diesel" }
    val toll = expenses.filter { it.category == "Toll" }
    val tax = expenses.filter { it.category == "Tax" }
    val other = expenses.filter { it.category !in listOf("Diesel", "Toll", "Tax") }
    val totalExp = expenses.sumOf { it.amount }
    return TripMetrics(
        totalBookings = bookings.size,
        totalBookedCabins = bookedCabins,
        totalFare = totalFare,
        totalCollected = totalCollected,
        totalPending = totalPending,
        totalExpenses = totalExp,
        dieselExpense = diesel.sumOf { it.amount },
        dieselLiters = diesel.sumOf { it.liters },
        tollExpense = toll.sumOf { it.amount },
        taxExpense = tax.sumOf { it.amount },
        otherExpense = other.sumOf { it.amount },
        attendanceCount = attendance.size,
        cabinPhotosCount = photos.map { it.cabinNumber }.toSet().size,
        noFareCount = noFareRecords.size,
        netProfit = totalCollected - totalExp
    )
}
