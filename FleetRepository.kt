package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Bus
import com.example.data.model.CompartmentPhoto
import com.example.data.model.Fleet
import com.example.data.model.FleetMember
import com.example.data.model.NoFareRecord
import com.example.data.model.PassengerBooking
import com.example.data.model.StaffAccountRegistry
import com.example.data.model.StaffAttendance
import com.example.data.model.Trip
import com.example.data.model.TripExpense
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FleetRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleStreamErrors(uid: String, path: String, op: OperationType = OperationType.LIST): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    // --- USER PROFILE & MEMBERSHIPS ---

    suspend fun ensureUserProfile(displayName: String, email: String): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        val snap = try {
            ref.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            throw e
        }

        val cleanName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Fleet User" } }.take(100)
        val cleanEmail = email.take(150)
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to cleanName,
                "email" to cleanEmail,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                ref.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, ref.path)
                throw e
            }
        }
        ref.get().await().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: UserProfile(id = uid, userId = uid, displayName = cleanName, email = cleanEmail)
    }

    suspend fun setActiveFleetId(fleetId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        try {
            ref.update(
                mapOf(
                    "activeFleetId" to fleetId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    fun observeMemberships(userId: String): Flow<List<UserMembership>> = flow {
        val path = "users/$userId/memberships"
        emitAll(
            db.collection("users").document(userId).collection("memberships")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserMembership::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.fleetName.lowercase() }
                }
                .handleStreamErrors(userId, path)
        )
    }

    // --- EXCLUSIVE 10-ACCOUNT ACCESS GATE & MULTI-OWNER / MULTI-MANAGER FLEET ---

    class InvalidStaffCredentialsException :
        IllegalArgumentException("Invalid username or password. Only the 10 authorized accounts can access Krishna Travels.")

    class OwnerMustInitializeException :
        IllegalStateException("An Owner must sign in first to initialize Krishna Travels before Managers can unlock.")

    suspend fun signInWithStaffSlot(
        username: String,
        password: String,
        displayName: String,
        email: String
    ): Result<UserMembership> = runCatching {
        val uid = requireUserId()
        val verified = StaffAccountRegistry.verifyCredentials(username, password)
            ?: throw InvalidStaffCredentialsException()

        val slot = verified.spec
        val digest = verified.digestHex
        val fleetId = StaffAccountRegistry.SHARED_FLEET_ID
        val fleetName = StaffAccountRegistry.SHARED_FLEET_NAME

        val cleanName = displayName.ifBlank {
            email.substringBefore("@").ifBlank { slot.username }
        }.take(100)
        val cleanEmail = email.take(150)

        val registryRef = db.collection("fleet_access_registry")
            .document(StaffAccountRegistry.ACCESS_REGISTRY_DOC)
        val fleetRef = db.collection("fleets").document(fleetId)
        val memberRef = fleetRef.collection("members").document(uid)
        val membershipRef = db.collection("users").document(uid)
            .collection("memberships").document(fleetId)
        val userRef = db.collection("users").document(uid)

        val registrySnap = try {
            registryRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, registryRef.path)
            throw e
        }

        if (!registrySnap.exists()) {
            if (slot.role != UserRole.OWNER) {
                throw OwnerMustInitializeException()
            }
            // First Owner initializes Krishna Travels HQ workspace + registry
            try {
                fleetRef.set(
                    mapOf(
                        "name" to fleetName,
                        "createdBy" to uid,
                        "ownerUids" to listOf(uid),
                        "memberUids" to listOf(uid),
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                registryRef.set(
                    mapOf(
                        "fleetId" to fleetId,
                        "fleetName" to fleetName,
                        "initializedBy" to uid,
                        "ownerUids" to listOf(uid),
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                memberRef.set(
                    mapOf(
                        "fleetId" to fleetId,
                        "userId" to uid,
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "role" to UserRole.OWNER.wireValue,
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "ownerUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                membershipRef.set(
                    mapOf(
                        "fleetId" to fleetId,
                        "fleetName" to fleetName,
                        "userId" to uid,
                        "role" to UserRole.OWNER.wireValue,
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                fleetRef.collection("owner_vault").document("slot_${slot.username}").set(
                    mapOf(
                        "slotUsername" to slot.username,
                        "claimedByUid" to uid,
                        "claimedByName" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                userRef.update(
                    mapOf(
                        "activeFleetId" to fleetId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, fleetRef.path)
                throw e
            }
        } else {
            val existingOwners = (registrySnap.get("ownerUids") as? List<*>)
                ?.filterIsInstance<String>()
                ?.ifEmpty { listOfNotNull(registrySnap.getString("initializedBy")) }
                ?: emptyList()
            val ownerUidsForMember = if (slot.role == UserRole.OWNER) {
                (existingOwners + uid).distinct()
            } else {
                existingOwners.ifEmpty { listOf(uid) }
            }

            try {
                // Step 1: Write authoritative FleetMember (rules validate exact 10-account digest & role)
                memberRef.set(
                    mapOf(
                        "fleetId" to fleetId,
                        "userId" to uid,
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "role" to slot.role.wireValue,
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "ownerUids" to ownerUidsForMember,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Step 2: Write UserMembership index
                membershipRef.set(
                    mapOf(
                        "fleetId" to fleetId,
                        "fleetName" to fleetName,
                        "userId" to uid,
                        "role" to slot.role.wireValue,
                        "slotUsername" to slot.username,
                        "slotAuthDigest" to digest,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Step 3: Add caller UID to shared fleet memberUids (and ownerUids if Owner)
                val fleetUpdate = mutableMapOf<String, Any>(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (slot.role == UserRole.OWNER) {
                    fleetUpdate["ownerUids"] = FieldValue.arrayUnion(uid)
                }
                fleetRef.update(fleetUpdate).await()

                if (slot.role == UserRole.OWNER) {
                    registryRef.update(
                        mapOf(
                            "ownerUids" to FieldValue.arrayUnion(uid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    fleetRef.collection("owner_vault").document("slot_${slot.username}").set(
                        mapOf(
                            "slotUsername" to slot.username,
                            "claimedByUid" to uid,
                            "claimedByName" to cleanName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                // Step 4: Set activeFleetId on user profile
                userRef.update(
                    mapOf(
                        "activeFleetId" to fleetId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, fleetRef.path)
                throw e
            }
        }

        UserMembership(
            id = fleetId,
            fleetId = fleetId,
            fleetName = fleetName,
            userId = uid,
            role = slot.role.wireValue,
            slotUsername = slot.username,
            slotAuthDigest = digest
        )
    }

    suspend fun writeOwnerVaultEntry(fleetId: String, docId: String, payload: Map<String, Any>): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("owner_vault").document(docId)
        try {
            ref.set(payload + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun readOwnerVaultEntry(fleetId: String, docId: String): Result<Map<String, Any>?> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("owner_vault").document(docId)
        try {
            ref.get().await().data
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            throw e
        }
    }

    fun observeFleet(fleetId: String): Flow<Fleet?> = flow {
        val uid = requireUserId()
        val ref = db.collection("fleets").document(fleetId)
        emitAll(
            ref.snapshots()
                .map { it.toObject(Fleet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamErrors(uid, ref.path, OperationType.GET)
        )
    }

    fun observeFleetMembersForOwner(fleetId: String): Flow<List<FleetMember>> = flow {
        val uid = requireUserId()
        val path = "fleets/$fleetId/members"
        emitAll(
            db.collection("fleets").document(fleetId).collection("members")
                .whereArrayContains("ownerUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FleetMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<FleetMember> { it.role != "owner" }.thenBy { it.slotUsername.lowercase() })
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun updateMemberRole(fleet: Fleet, member: FleetMember, newRole: UserRole): Result<Unit> = runCatching {
        requireUserId()
        val updatedOwners = if (newRole == UserRole.OWNER) {
            (fleet.ownerUids + member.userId).distinct()
        } else {
            val remaining = fleet.ownerUids.filter { it != member.userId }
            require(remaining.isNotEmpty()) { "Fleet must have at least one Owner." }
            remaining
        }
        val fleetRef = db.collection("fleets").document(fleet.id)
        val memberRef = fleetRef.collection("members").document(member.userId)
        val membershipRef = db.collection("users").document(member.userId).collection("memberships").document(fleet.id)

        val batch = db.batch()
        batch.update(
            fleetRef,
            mapOf(
                "ownerUids" to updatedOwners,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            memberRef,
            mapOf(
                "role" to newRole.wireValue,
                "ownerUids" to updatedOwners,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            membershipRef,
            mapOf(
                "role" to newRole.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
            throw e
        }
    }

    suspend fun syncMemberUidsAcrossFleet(
        fleetId: String,
        memberUids: List<String>,
        ownerUids: List<String> = emptyList()
    ) {
        if (memberUids.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        val fleetRef = db.collection("fleets").document(fleetId)
        val subcollections = listOf("buses", "trips", "attendance", "expenses", "bookings", "compartment_photos", "no_fare_records")
        try {
            val batch = db.batch()
            var ops = 0
            for (sub in subcollections) {
                val snap = fleetRef.collection(sub)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    val existing = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (existing.toSet() != memberUids.toSet()) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberUids" to memberUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        ops++
                    }
                }
            }
            if (ownerUids.isNotEmpty()) {
                val membersSnap = fleetRef.collection("members")
                    .whereArrayContains("ownerUids", uid)
                    .get()
                    .await()
                for (doc in membersSnap.documents) {
                    val existingOwners = (doc.get("ownerUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (existingOwners.toSet() != ownerUids.toSet()) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "ownerUids" to ownerUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        ops++
                    }
                }
            }
            if (ops > 0) {
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, fleetRef.path)
        }
    }

    // --- BUSES ---

    fun observeBuses(fleetId: String, uid: String): Flow<List<Bus>> = flow {
        val path = "fleets/$fleetId/buses"
        emitAll(
            db.collection("fleets").document(fleetId).collection("buses")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Bus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.busNumber }
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addBus(
        fleet: Fleet,
        busNumber: String,
        coachName: String,
        totalCabins: Int = 30,
        defaultRoute: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("fleets").document(fleet.id).collection("buses").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "busNumber" to busNumber.trim().uppercase().take(30),
            "coachName" to coachName.trim().take(80),
            "totalCabins" to totalCabins.coerceIn(1, 60),
            "defaultRoute" to defaultRoute.trim().take(100),
            "active" to true,
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBus(bus: Bus): Result<Unit> = runCatching {
        requireUserId()
        require(bus.id.isNotBlank())
        val ref = db.collection("fleets").document(bus.fleetId).collection("buses").document(bus.id)
        try {
            ref.update(
                mapOf(
                    "busNumber" to bus.busNumber.trim().uppercase().take(30),
                    "coachName" to bus.coachName.trim().take(80),
                    "totalCabins" to bus.totalCabins.coerceIn(1, 60),
                    "defaultRoute" to bus.defaultRoute.trim().take(100),
                    "active" to bus.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteBus(fleetId: String, busId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("buses").document(busId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- TRIPS ---

    fun observeTrips(fleetId: String, uid: String): Flow<List<Trip>> = flow {
        val path = "fleets/$fleetId/trips"
        emitAll(
            db.collection("fleets").document(fleetId).collection("trips")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamErrors(uid, path)
        )
    }

    fun observeTrip(fleetId: String, tripId: String): Flow<Trip?> = flow {
        val uid = requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("trips").document(tripId)
        emitAll(
            ref.snapshots()
                .map { it.toObject(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamErrors(uid, ref.path, OperationType.GET)
        )
    }

    suspend fun createTrip(
        fleet: Fleet,
        bus: Bus,
        origin: String,
        destination: String,
        tripDate: String,
        departureTime: String,
        driverName: String,
        conductorName: String,
        notes: String,
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("fleets").document(fleet.id).collection("trips").document()
        val routeName = "${origin.trim()} -> ${destination.trim()}".take(120)
        val payload = mapOf(
            "fleetId" to fleet.id,
            "busId" to bus.id,
            "busNumber" to bus.busNumber,
            "routeName" to routeName,
            "origin" to origin.trim().take(60),
            "destination" to destination.trim().take(60),
            "tripDate" to tripDate.trim().take(30),
            "departureTime" to departureTime.trim().take(20),
            "driverName" to driverName.trim().take(80),
            "conductorName" to conductorName.trim().take(80),
            "status" to "in_progress",
            "notes" to notes.trim().take(300),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTrip(trip: Trip): Result<Unit> = runCatching {
        requireUserId()
        require(trip.id.isNotBlank())
        val ref = db.collection("fleets").document(trip.fleetId).collection("trips").document(trip.id)
        try {
            ref.update(
                mapOf(
                    "origin" to trip.origin.trim().take(60),
                    "destination" to trip.destination.trim().take(60),
                    "routeName" to "${trip.origin.trim()} -> ${trip.destination.trim()}".take(120),
                    "tripDate" to trip.tripDate.trim().take(30),
                    "departureTime" to trip.departureTime.trim().take(20),
                    "driverName" to trip.driverName.trim().take(80),
                    "conductorName" to trip.conductorName.trim().take(80),
                    "status" to trip.status,
                    "notes" to trip.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteTrip(fleetId: String, tripId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("trips").document(tripId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- STAFF ATTENDANCE (WITH PHOTO & GPS) ---

    fun observeAttendance(fleetId: String, tripId: String, uid: String): Flow<List<StaffAttendance>> = flow {
        val path = "fleets/$fleetId/attendance"
        emitAll(
            db.collection("fleets").document(fleetId).collection("attendance")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("tripId", tripId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StaffAttendance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addAttendance(
        fleet: Fleet,
        tripId: String,
        staffName: String,
        staffRole: String,
        status: String,
        photoBase64: String,
        capturedLatitude: Double?,
        capturedLongitude: Double?,
        capturedLocationLabel: String,
        notes: String,
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(photoBase64.isNotBlank()) { "Staff attendance photo is required." }
        val docRef = db.collection("fleets").document(fleet.id).collection("attendance").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "tripId" to tripId,
            "staffName" to staffName.trim().take(100),
            "staffRole" to staffRole,
            "status" to status,
            "photoBase64" to photoBase64,
            "capturedLatitude" to capturedLatitude,
            "capturedLongitude" to capturedLongitude,
            "capturedLocationLabel" to capturedLocationLabel.take(200),
            "notes" to notes.trim().take(250),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAttendance(item: StaffAttendance): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(item.fleetId).collection("attendance").document(item.id)
        try {
            ref.update(
                mapOf(
                    "staffName" to item.staffName.trim().take(100),
                    "staffRole" to item.staffRole,
                    "status" to item.status,
                    "notes" to item.notes.trim().take(250),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteAttendance(fleetId: String, attendanceId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("attendance").document(attendanceId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- DIESEL, TOLL, TAX & EXPENSES ---

    fun observeExpenses(fleetId: String, tripId: String, uid: String): Flow<List<TripExpense>> = flow {
        val path = "fleets/$fleetId/expenses"
        emitAll(
            db.collection("fleets").document(fleetId).collection("expenses")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("tripId", tripId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TripExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamErrors(uid, path)
        )
    }

    fun observeAllFleetExpenses(fleetId: String, uid: String): Flow<List<TripExpense>> = flow {
        val path = "fleets/$fleetId/expenses"
        emitAll(
            db.collection("fleets").document(fleetId).collection("expenses")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TripExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addExpense(
        fleet: Fleet,
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
        capturedLatitude: Double?,
        capturedLongitude: Double?,
        capturedLocationLabel: String,
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0) { "Amount must be greater than zero." }
        val docRef = db.collection("fleets").document(fleet.id).collection("expenses").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "tripId" to tripId,
            "category" to category,
            "amount" to amount,
            "liters" to liters.coerceAtLeast(0.0),
            "ratePerLiter" to ratePerLiter.coerceAtLeast(0.0),
            "odometerKm" to odometerKm.coerceAtLeast(0.0),
            "vendorOrPlaza" to vendorOrPlaza.trim().take(120),
            "paymentMode" to paymentMode,
            "notes" to notes.trim().take(300),
            "receiptPhotoBase64" to receiptPhotoBase64.ifBlank { null },
            "capturedLatitude" to capturedLatitude,
            "capturedLongitude" to capturedLongitude,
            "capturedLocationLabel" to capturedLocationLabel.ifBlank { null }?.take(200),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateExpense(expense: TripExpense): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(expense.fleetId).collection("expenses").document(expense.id)
        try {
            ref.update(
                mapOf(
                    "category" to expense.category,
                    "amount" to expense.amount,
                    "liters" to expense.liters,
                    "ratePerLiter" to expense.ratePerLiter,
                    "odometerKm" to expense.odometerKm,
                    "vendorOrPlaza" to expense.vendorOrPlaza.trim().take(120),
                    "paymentMode" to expense.paymentMode,
                    "notes" to expense.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteExpense(fleetId: String, expenseId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("expenses").document(expenseId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- PASSENGER BOOKINGS & BILLING ---

    fun observeBookings(fleetId: String, tripId: String, uid: String): Flow<List<PassengerBooking>> = flow {
        val path = "fleets/$fleetId/bookings"
        emitAll(
            db.collection("fleets").document(fleetId).collection("bookings")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("tripId", tripId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PassengerBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamErrors(uid, path)
        )
    }

    fun observeAllFleetBookings(fleetId: String, uid: String): Flow<List<PassengerBooking>> = flow {
        val path = "fleets/$fleetId/bookings"
        emitAll(
            db.collection("fleets").document(fleetId).collection("bookings")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PassengerBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addBooking(
        fleet: Fleet,
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
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(cabinNumbers.isNotEmpty()) { "Select at least one sleeper cabin (1-30)." }
        val sortedCabins = cabinNumbers.distinct().sorted()
        val berthLabel = sortedCabins.joinToString(", ") { "C${it.toString().padStart(2, '0')}" }.take(80)
        val invoice = "KT-${(1000..9999).random()}-${sortedCabins.first()}"
        val docRef = db.collection("fleets").document(fleet.id).collection("bookings").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "tripId" to tripId,
            "invoiceNumber" to invoice,
            "passengerName" to passengerName.trim().take(100),
            "passengerPhone" to passengerPhone.trim().take(20),
            "cabinNumbers" to sortedCabins,
            "berthNumbers" to berthLabel,
            "seatCount" to sortedCabins.size,
            "boardingPoint" to boardingPoint.trim().take(100),
            "droppingPoint" to droppingPoint.trim().take(100),
            "fareAmount" to fareAmount.coerceAtLeast(0.0),
            "paidAmount" to paidAmount.coerceAtLeast(0.0),
            "paymentStatus" to paymentStatus,
            "paymentMode" to paymentMode,
            "notes" to notes.trim().take(200),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBooking(booking: PassengerBooking): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(booking.fleetId).collection("bookings").document(booking.id)
        try {
            ref.update(
                mapOf(
                    "passengerName" to booking.passengerName.trim().take(100),
                    "passengerPhone" to booking.passengerPhone.trim().take(20),
                    "fareAmount" to booking.fareAmount,
                    "paidAmount" to booking.paidAmount,
                    "paymentStatus" to booking.paymentStatus,
                    "paymentMode" to booking.paymentMode,
                    "notes" to booking.notes.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteBooking(fleetId: String, bookingId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId).collection("bookings").document(bookingId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- 30-CABIN SLEEPER COMPARTMENT PHOTOS ---

    fun observeCompartmentPhotos(fleetId: String, tripId: String, uid: String): Flow<List<CompartmentPhoto>> = flow {
        val path = "fleets/$fleetId/compartment_photos"
        emitAll(
            db.collection("fleets").document(fleetId).collection("compartment_photos")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("tripId", tripId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CompartmentPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.cabinNumber }
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addCompartmentPhoto(
        fleet: Fleet,
        tripId: String,
        cabinNumber: Int,
        passengerName: String,
        conditionNote: String,
        photoBase64: String,
        capturedLatitude: Double?,
        capturedLongitude: Double?,
        capturedLocationLabel: String,
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(cabinNumber in 1..30) { "Cabin number must be between 1 and 30." }
        require(photoBase64.isNotBlank()) { "Cabin photo is required." }
        val deck = if (cabinNumber <= 15) "Lower" else "Upper"
        val berthLabel = "Cabin C${cabinNumber.toString().padStart(2, '0')} ($deck)"
        val docRef = db.collection("fleets").document(fleet.id).collection("compartment_photos").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "tripId" to tripId,
            "cabinNumber" to cabinNumber,
            "berthLabel" to berthLabel,
            "passengerName" to passengerName.trim().take(100),
            "conditionNote" to conditionNote.trim().take(250),
            "photoBase64" to photoBase64,
            "capturedLatitude" to capturedLatitude,
            "capturedLongitude" to capturedLongitude,
            "capturedLocationLabel" to capturedLocationLabel.take(200),
            "verifiedByOwner" to (createdByRole == UserRole.OWNER),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCompartmentPhoto(photo: CompartmentPhoto): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(photo.fleetId)
            .collection("compartment_photos").document(photo.id)
        try {
            ref.update(
                mapOf(
                    "passengerName" to photo.passengerName.trim().take(100),
                    "conditionNote" to photo.conditionNote.trim().take(250),
                    "verifiedByOwner" to photo.verifiedByOwner,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteCompartmentPhoto(fleetId: String, photoId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId)
            .collection("compartment_photos").document(photoId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- NO-FARE PASSENGERS & UPLOAD HISTORY (NAME, PHOTO, MOBILE NUMBER) ---

    fun observeNoFareRecords(fleetId: String, tripId: String, uid: String): Flow<List<NoFareRecord>> = flow {
        val path = "fleets/$fleetId/no_fare_records"
        emitAll(
            db.collection("fleets").document(fleetId).collection("no_fare_records")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("tripId", tripId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NoFareRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamErrors(uid, path)
        )
    }

    suspend fun addNoFareRecord(
        fleet: Fleet,
        tripId: String,
        personName: String,
        mobileNumber: String,
        cabinNumber: Int,
        expectedFare: Double,
        reasonOrNote: String,
        photoBase64: String,
        capturedLatitude: Double?,
        capturedLongitude: Double?,
        capturedLocationLabel: String,
        createdByName: String,
        createdByRole: UserRole
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(personName.isNotBlank()) { "Person name is required." }
        require(mobileNumber.isNotBlank()) { "Mobile number is required." }
        val docRef = db.collection("fleets").document(fleet.id).collection("no_fare_records").document()
        val payload = mapOf(
            "fleetId" to fleet.id,
            "tripId" to tripId,
            "personName" to personName.trim().take(100),
            "mobileNumber" to mobileNumber.trim().take(20),
            "cabinNumber" to cabinNumber.coerceIn(0, 30),
            "expectedFare" to expectedFare.coerceAtLeast(0.0),
            "reasonOrNote" to reasonOrNote.trim().take(250),
            "photoBase64" to photoBase64.ifBlank { null },
            "capturedLatitude" to capturedLatitude,
            "capturedLongitude" to capturedLongitude,
            "capturedLocationLabel" to capturedLocationLabel.ifBlank { null }?.take(200),
            "resolvedByOwner" to (createdByRole == UserRole.OWNER),
            "memberUids" to fleet.memberUids.ifEmpty { listOf(uid) },
            "createdByUid" to uid,
            "createdByName" to createdByName.ifBlank { "Staff" }.take(100),
            "createdByRole" to createdByRole.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNoFareRecord(record: NoFareRecord): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(record.fleetId)
            .collection("no_fare_records").document(record.id)
        try {
            ref.update(
                mapOf(
                    "personName" to record.personName.trim().take(100),
                    "mobileNumber" to record.mobileNumber.trim().take(20),
                    "cabinNumber" to record.cabinNumber.coerceIn(0, 30),
                    "expectedFare" to record.expectedFare.coerceAtLeast(0.0),
                    "reasonOrNote" to record.reasonOrNote.trim().take(250),
                    "resolvedByOwner" to record.resolvedByOwner,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteNoFareRecord(fleetId: String, recordId: String): Result<Unit> = runCatching {
        requireUserId()
        val ref = db.collection("fleets").document(fleetId)
            .collection("no_fare_records").document(recordId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }
}
