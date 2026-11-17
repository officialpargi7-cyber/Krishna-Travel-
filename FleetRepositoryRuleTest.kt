package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.StaffAccountRegistry
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FleetRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun exclusiveTenAccountsAndImmutability_enforcedByFirestoreRules() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val owner1Email = "owner1_$suffix@krishna.com"
        val owner2Email = "owner2_$suffix@krishna.com"
        val mgrEmail = "mgr_$suffix@krishna.com"
        val outsiderEmail = "outsider_$suffix@example.com"

        val repo = FleetRepository(firestore, auth)

        // Ensure a fresh shared registry state isn't assumed if a previous run seeded it;
        // Sign in with Owner 1 first (if registry already exists, Owner 1 still joins cleanly!)
        val mgrUid = signInTestUser(mgrEmail)
        withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Manager Raj", mgrEmail).getOrThrow() }

        // Wrong credentials must be rejected locally before hitting Firestore
        val badCreds = repo.signInWithStaffSlot("unknown_user", "wrongpass", "Manager Raj", mgrEmail)
        assertTrue(badCreds.exceptionOrNull() is FleetRepository.InvalidStaffCredentialsException)

        // Sign in as Owner 1 -> unlocks Krishna Travels HQ
        val owner1Uid = signInTestUser(owner1Email)
        withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Owner One", owner1Email).getOrThrow() }
        val owner1Membership = withTimeout(TIMEOUT_MS) {
            repo.signInWithStaffSlot("krishna_owner1", "KT@Own9142", "Owner One", owner1Email).getOrThrow()
        }
        val fleetId = owner1Membership.fleetId
        assertEquals(StaffAccountRegistry.SHARED_FLEET_ID, fleetId)
        assertEquals(UserRole.OWNER, owner1Membership.userRole)

        // Second Owner unlocks with slot #2
        val owner2Uid = signInTestUser(owner2Email)
        withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Owner Two", owner2Email).getOrThrow() }
        val owner2Membership = withTimeout(TIMEOUT_MS) {
            repo.signInWithStaffSlot("krishna_owner2", "KT@Own8357", "Owner Two", owner2Email).getOrThrow()
        }
        assertEquals(UserRole.OWNER, owner2Membership.userRole)

        // Manager unlocks with Manager slot #1
        signInTestUser(mgrEmail)
        val mgrMembership = withTimeout(TIMEOUT_MS) {
            repo.signInWithStaffSlot("krishna_mgr1", "KT#Mgr2841", "Manager Raj", mgrEmail).getOrThrow()
        }
        assertEquals(UserRole.MANAGER, mgrMembership.userRole)

        // Outsider cannot read/write Owner Vault or see fleet trips
        val outsiderUid = signInTestUser(outsiderEmail)
        withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Outsider User", outsiderEmail).getOrThrow() }
        val outsiderVaultRead = repo.readOwnerVaultEntry(fleetId, "slot_krishna_owner1")
        assertTrue(outsiderVaultRead.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderVaultRead.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
        val outsiderTrips = withTimeout(TIMEOUT_MS) {
            repo.observeTrips(fleetId, outsiderUid).first()
        }
        assertTrue(outsiderTrips.isEmpty())

        // Owner 2 adds bus GJ01DZ8325 and starts a trip
        signInTestUser(owner2Email)
        val fleet = withTimeout(TIMEOUT_MS) { repo.observeFleet(fleetId).first { it != null }!! }
        repo.syncMemberUidsAcrossFleet(fleetId, fleet.memberUids, fleet.ownerUids)
        val busId = withTimeout(TIMEOUT_MS) {
            repo.addBus(fleet, "GJ01DZ8325", "Krishna 2+1 AC Sleeper", 30, "Ahmedabad - Udaipur").getOrThrow()
        }
        val bus = withTimeout(TIMEOUT_MS) {
            repo.observeBuses(fleetId, owner2Uid).first { list -> list.any { it.id == busId } }.first { it.id == busId }
        }
        val tripId = withTimeout(TIMEOUT_MS) {
            repo.createTrip(
                fleet = fleet,
                bus = bus,
                origin = "Ahmedabad",
                destination = "Udaipur",
                tripDate = "2026-11-15",
                departureTime = "21:30",
                driverName = "Ramesh",
                conductorName = "Suresh",
                notes = "Night sleeper",
                createdByName = "Owner Two",
                createdByRole = UserRole.OWNER
            ).getOrThrow()
        }

        // Owner 2 CAN write & read owner_vault
        assertTrue(repo.writeOwnerVaultEntry(fleetId, "audit_test_$suffix", mapOf("note" to "Owner Audit")).isSuccess)
        assertTrue(repo.readOwnerVaultEntry(fleetId, "audit_test_$suffix").isSuccess)

        // Manager uploads staff attendance, expense, cabin 12 photo, & no-fare record
        signInTestUser(mgrEmail)
        // Manager MUST be denied from owner_vault read/write
        val mgrVaultWrite = repo.writeOwnerVaultEntry(fleetId, "audit_test_$suffix", mapOf("note" to "Hack"))
        assertTrue(mgrVaultWrite.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            mgrVaultWrite.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
        val mgrVaultRead = repo.readOwnerVaultEntry(fleetId, "audit_test_$suffix")
        assertTrue(mgrVaultRead.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            mgrVaultRead.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val attId = withTimeout(TIMEOUT_MS) {
            repo.addAttendance(
                fleet = fleet,
                tripId = tripId,
                staffName = "Ramesh",
                staffRole = "Driver",
                status = "Present",
                photoBase64 = "fakebase64image",
                capturedLatitude = 23.0225,
                capturedLongitude = 72.5714,
                capturedLocationLabel = "Ahmedabad, Gujarat",
                notes = "On time",
                createdByName = "Manager Raj",
                createdByRole = UserRole.MANAGER
            ).getOrThrow()
        }
        val expId = withTimeout(TIMEOUT_MS) {
            repo.addExpense(
                fleet = fleet,
                tripId = tripId,
                category = "Diesel",
                amount = 9500.0,
                liters = 100.0,
                ratePerLiter = 95.0,
                odometerKm = 124500.0,
                vendorOrPlaza = "IOCL Highway",
                paymentMode = "Cash",
                notes = "Full tank",
                receiptPhotoBase64 = "",
                capturedLatitude = 23.0225,
                capturedLongitude = 72.5714,
                capturedLocationLabel = "Ahmedabad, Gujarat",
                createdByName = "Manager Raj",
                createdByRole = UserRole.MANAGER
            ).getOrThrow()
        }
        val photoId = withTimeout(TIMEOUT_MS) {
            repo.addCompartmentPhoto(
                fleet = fleet,
                tripId = tripId,
                cabinNumber = 12,
                passengerName = "Kiran Shah",
                conditionNote = "Cabin clean",
                photoBase64 = "cabin12base64",
                capturedLatitude = 23.0225,
                capturedLongitude = 72.5714,
                capturedLocationLabel = "Ahmedabad, Gujarat",
                createdByName = "Manager Raj",
                createdByRole = UserRole.MANAGER
            ).getOrThrow()
        }

        // Manager CANNOT update or delete uploaded records
        val uploadedAtt = withTimeout(TIMEOUT_MS) {
            repo.observeAttendance(fleetId, tripId, mgrUid).first { it.any { a -> a.id == attId } }.first { it.id == attId }
        }
        val mgrUpdateAtt = repo.updateAttendance(uploadedAtt.copy(status = "Late"))
        assertTrue(mgrUpdateAtt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            mgrUpdateAtt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val uploadedExp = withTimeout(TIMEOUT_MS) {
            repo.observeExpenses(fleetId, tripId, mgrUid).first { it.any { e -> e.id == expId } }.first { it.id == expId }
        }
        val mgrDeleteExp = repo.deleteExpense(fleetId, expId)
        assertTrue(mgrDeleteExp.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            mgrDeleteExp.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val uploadedPhoto = withTimeout(TIMEOUT_MS) {
            repo.observeCompartmentPhotos(fleetId, tripId, mgrUid).first { it.any { p -> p.id == photoId } }.first { it.id == photoId }
        }
        val mgrVerifyPhoto = repo.updateCompartmentPhoto(uploadedPhoto.copy(verifiedByOwner = true))
        assertTrue(mgrVerifyPhoto.isFailure)

        // Manager uploads No-Fare person record (name, mobile, photo + GPS) and CANNOT edit/delete
        val noFareId = withTimeout(TIMEOUT_MS) {
            repo.addNoFareRecord(
                fleet = fleet,
                tripId = tripId,
                personName = "Vikram Joshi",
                mobileNumber = "9876543210",
                cabinNumber = 18,
                expectedFare = 1200.0,
                reasonOrNote = "Promised to pay at destination",
                photoBase64 = "nofarepersonbase64",
                capturedLatitude = 23.0225,
                capturedLongitude = 72.5714,
                capturedLocationLabel = "Ahmedabad, Gujarat",
                createdByName = "Manager Raj",
                createdByRole = UserRole.MANAGER
            ).getOrThrow()
        }
        val uploadedNoFare = withTimeout(TIMEOUT_MS) {
            repo.observeNoFareRecords(fleetId, tripId, mgrUid).first { it.any { n -> n.id == noFareId } }.first { it.id == noFareId }
        }
        val mgrUpdateNoFare = repo.updateNoFareRecord(uploadedNoFare.copy(resolvedByOwner = true))
        assertTrue(mgrUpdateNoFare.isFailure)
        val mgrDeleteNoFare = repo.deleteNoFareRecord(fleetId, noFareId)
        assertTrue(mgrDeleteNoFare.isFailure)

        // Owner 1 CAN verify cabin photo, edit expense, and resolve No-Fare record
        signInTestUser(owner1Email)
        val ownerVerify = withTimeout(TIMEOUT_MS) {
            repo.updateCompartmentPhoto(uploadedPhoto.copy(verifiedByOwner = true))
        }
        assertTrue(ownerVerify.isSuccess)
        val ownerUpdateExp = withTimeout(TIMEOUT_MS) {
            repo.updateExpense(uploadedExp.copy(amount = 9600.0))
        }
        assertTrue(ownerUpdateExp.isSuccess)
        val ownerResolveNoFare = withTimeout(TIMEOUT_MS) {
            repo.updateNoFareRecord(uploadedNoFare.copy(resolvedByOwner = true))
        }
        assertTrue(ownerResolveNoFare.isSuccess)
    }

    @Test
    fun unauthenticatedCaller_rejectedOnTripObservation() = runBlocking {
        auth.signOut()
        val repo = FleetRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTrips("fleet_missing", "uid_missing").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    private companion object {
        const val TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
