package com.example.ui.i18n

import androidx.compose.runtime.Immutable

@Immutable
interface AppStrings {
    // Brand & Common
    val appTitleUpper: String
    val appTitle: String
    val appTagline: String
    val languageLabel: String
    val chooseLanguageHint: String
    val ok: String
    val cancel: String
    val close: String
    val delete: String
    val save: String
    val signOut: String
    val yes: String
    val partnerFallback: String
    val requiredSuffix: String
    val optionalSuffix: String

    // Roles & Locks
    val roleOwner: String
    val roleManager: String
    val roleOwnerUpper: String
    val roleManagerUpper: String
    val lockedOwnerOnlyEdit: String
    val ownerEditable: String

    // Auth Screen
    val authPortalTitle: String
    val authFeature1Title: String
    val authFeature1Desc: String
    val authFeature2Title: String
    val authFeature2Desc: String
    val authFeature3Title: String
    val authFeature3Desc: String
    val authFeature4Title: String
    val authFeature4Desc: String
    val signInWithGoogle: String
    val authFooterNote: String

    // Onboarding
    val welcomeUser: (String) -> String
    val ownerSetupTitle: String
    val ownerSetupSubtitle: String
    val fleetNameField: String
    val startAsOwner: String
    val joinTeamTitle: String
    val joinTeamSubtitle: String
    val inviteCodeField: String
    val joinFleetButton: String
    val staffAccessGateTitle: String
    val staffAccessGateSubtitle: String
    val usernameLabel: String
    val passwordLabel: String
    val unlockAppBtn: String
    val invalidStaffCredentials: String
    val ownerMustInitFirst: String
    val restrictedTenAccountsNote: String
    val authorizedSlotListTitle: String
    val authorizedSlotListDesc: String
    val managerPasswordHiddenNotice: String
    val slotStatusClaimedBy: (String) -> String
    val slotStatusAvailable: String

    // Nav Tabs
    val navTrips: String
    val navBuses: String
    val navTeam: String

    // Trips Dashboard
    val ownerLiveFleetLedger: String
    val busesTripsCount: (Int, Int) -> String
    val collected: String
    val expenses: String
    val netBalance: String
    val managerDashboardNotice: String
    val cabinsRatio: (Int, Int) -> String
    val tripBillsLabel: (String) -> String
    val expensesLabel: (String) -> String
    val netLabel: (String) -> String
    val managerLabel: (String) -> String
    val driverLabel: (String) -> String
    val addBusFirstTitle: String
    val noTripsTitle: String
    val addBusFirstOwnerBody: String
    val addBusFirstManagerBody: String
    val startTripBody: String
    val addBusNow: String
    val startFirstTrip: String
    val startNewBusTrip: String
    val selectBusLabel: String
    val fromLabel: String
    val toLabel: String
    val dateLabel: String
    val depTimeLabel: String
    val driverNameReq: String
    val conductorHelperLabel: String
    val tripNotesLabel: String
    val managerTripLockWarning: String
    val saveAndLockTrip: String

    // Buses Screen
    val ownerExpenseBreakdown: String
    val diesel: String
    val toll: String
    val taxPermit: String
    val other: String
    val passengerPendingDues: String
    val noBusesAdded: String
    val noBusesOwnerDesc: String
    val noBusesManagerDesc: String
    val addDefaultBusBtn: String
    val routePrefix: (String) -> String
    val registerSleeperBus: String
    val busNumberPlateReq: String
    val coachModelTypeReq: String
    val sleeperCabinsCount: String
    val primaryRoute: String
    val saveBus: String

    // Team Screen
    val activeMembersOwners: (Int, Int) -> String
    val ownerAuthorityDesc: String
    val managerAccessDesc: String
    val inviteCoOwnerOrManager: String
    val inviteDesc: String
    val addOwner: String
    val addManager: String
    val shareInviteCode: String
    val copyCode: String
    val shareCode: String
    val syncHistoricalTrips: String
    val fleetOwnersAndManagers: (Int) -> String
    val youTag: String
    val managerLockedNoticeTeam: String
    val switchFleetWorkspace: String
    val createOrJoinAnotherFleet: String
    val appLanguageSettingsTitle: String
    val appLanguageSettingsDesc: String

    // Trip Detail Screen
    val tabTripBills: (Int) -> String
    val tabCabins: (Int) -> String
    val tabNoFare: (Int) -> String
    val tabDieselExp: (Int) -> String
    val tabStaff: (Int) -> String
    val tripBillsKpi: String
    val netProfitKpi: String
    val pendingDueKpi: String
    val tripManagerKpi: String
    val cabinsKpi: String
    val managerTripBanner: String
    val deleteTripTitle: String
    val deleteTripBody: String
    val shareTripReportDesc: String

    // Trip Bookings Tab
    val bookedKpi: String
    val newPassengerBookingBtn: String
    val filterAllBills: (Int) -> String
    val filterUnpaidNoFare: String
    val filterPaid: String
    val emptyBookingsMsg: String
    val emptyFilteredBillsMsg: String
    val paidOutOf: (String, String) -> String
    val managerTimeLine: (String, String) -> String
    val passengerBookingDialogTitle: String
    val passengerNameReq: String
    val phoneNumberLabel: String
    val selectSleeperCabinsReq: String
    val boardingLabel: String
    val droppingLabel: String
    val farePerCabinLabel: String
    val totalFareLabel: String
    val paidAmountLabel: String
    val didntGiveFareChip: String
    val didntGiveFareNoteDefault: String
    val statusPrefix: (String) -> String
    val balanceDuePrefix: (String) -> String
    val notesLabel: String
    val managerBookingLockWarning: String
    val saveAndLockBill: String

    // 30 Cabins Tab
    val cabinsHeroTitle: String
    val cabinsHeroOwnerSub: (Int) -> String
    val cabinsHeroManagerSub: (Int) -> String
    val filterAll30: String
    val filterLower15: String
    val filterUpper30: String
    val tapPhoto: String
    val photoSealed: String
    val ownerOnly: String
    val gpsTagged: String
    val passengerFallback: String
    val sealedCabinTitle: (String) -> String
    val sealedCabinBody: String
    val cabinDialogTitle: (String, String) -> String
    val lowerDeck: String
    val upperDeck: String
    val passengerNameOpt: String
    val cabinNoteLabel: String
    val defaultCabinNote: String
    val cabinPhotoSectionTitle: String
    val managerCabinLockWarning: String
    val saveAndLockPhoto: String
    val uploadedByRole: (String, String) -> String
    val sharePhotoToPassengerOwner: String
    val verifiedLabel: String
    val verifyLabel: String

    // No-Fare / Upload History Tab
    val noFareHeaderTitle: String
    val noFareHeaderOwner: String
    val noFareHeaderManager: String
    val addNoFareBtn: String
    val emptyNoFareMsg: String
    val sealedBadge: String
    val resolvedBadge: String
    val noFareBadge: String
    val mobilePrefix: (String) -> String
    val generalStanding: String
    val duePrefix: (String) -> String
    val markedResolvedByOwner: String
    val markFareCollectedResolved: String
    val noFarePhotoSealedNotice: String
    val addNoFareDialogTitle: String
    val personNameReq: String
    val mobileNumberReq: String
    val cabin1To30Label: String
    val dueFareLabel: String
    val reasonDidntPay: String
    val reasonPayOnArrival: String
    val reasonStaffKnown: String
    val reasonHalfPaid: String
    val reasonHistoryNoteLabel: String
    val personIdPhotoTitle: String
    val noFareLockWarning: String
    val saveAndLockHistory: String

    // Diesel & Expenses Tab
    val dieselLitersLabel: (String) -> String
    val totalLabel: String
    val addExpenseBtn: String
    val emptyExpensesMsg: (String) -> String
    val receiptPhotoSealedNotice: String
    val addExpenseDialogTitle: String
    val categoryLabel: String
    val litersLabel: String
    val ratePerLiterLabel: String
    val totalAmountReq: String
    val projectedTripExpenses: String
    val fuelPumpName: String
    val tollPlazaName: String
    val borderStateTaxName: String
    val vendorDescName: String
    val odometerLabel: String
    val paymentModeLabel: String
    val receiptPhotoTitle: String
    val expenseAntiTamperWarning: String
    val saveAndLockBtn: String
    val confirmLockAndSaveBtn: String
    val saveExpenseBtn: String

    // Staff Attendance Tab
    val addAttendanceBtn: String
    val emptyAttendanceMsg: String
    val dateTimePrefix: (String) -> String
    val attendancePhotoSealedNotice: String
    val uploadedByManager: (String) -> String
    val attendanceDialogTitle: String
    val staffMemberNameReq: String
    val staffRoleLabel: String
    val statusLabel: String
    val notesOptionalLabel: String
    val staffPhotoTitle: String
    val managerAttendanceLockWarning: String
    val saveAndLockAttendance: String

    // Photo, GPS & Calculator Shared Components
    val photoReady: String
    val camera: String
    val gallery: String
    val compressingAndTaggingGps: String
    val capturingGps: String
    val fetchingGps: String
    val tapToRefreshGps: String
    val locationPermissionDenied: String
    val locationNotRecorded: String
    val openInMaps: String
    val openCalculator: String
    val hideCalculator: String
    val delKey: String
    val useAmount: String

    // ViewModel Action Feedback
    val msgFleetCreated: (String) -> String
    val msgJoinedFleet: (String, String) -> String
    val msgInviteReady: (String, String) -> String
    val msgRoleUpdated: (String, String) -> String
    val msgBusAdded: (String) -> String
    val msgBusRemoved: String
    val msgTripCreatedLocked: String
    val msgTripStatusUpdated: (String) -> String
    val msgTripDeleted: String
    val msgAttendanceSavedLocked: String
    val msgAttendanceDeleted: String
    val msgExpenseSavedLocked: (String) -> String
    val msgExpenseUpdated: String
    val msgExpenseDeleted: String
    val msgBookingSavedLocked: String
    val msgBookingUpdated: String
    val msgBookingDeleted: String
    val msgCabinPhotoSavedLocked: (String) -> String
    val msgCabinVerified: String
    val msgVerificationRemoved: String
    val msgCabinPhotoDeleted: String
    val msgNoFareSavedLocked: String
    val msgMarkedResolved: String
    val msgMarkedPending: String
    val msgRecordDeleted: String
    val msgOwnerOnlyError: String

    fun roleLabel(wireOrRole: String): String =
        if (wireOrRole.equals("owner", ignoreCase = true)) roleOwner else roleManager

    fun tripStatusLabel(wireStatus: String): String = when (wireStatus) {
        "scheduled" -> when (languageCodeHint()) {
            "hi" -> "निर्धारित"
            "gu" -> "નિર્ધારિત"
            else -> "Scheduled"
        }
        "completed" -> when (languageCodeHint()) {
            "hi" -> "पूर्ण"
            "gu" -> "પૂર્ણ"
            else -> "Completed"
        }
        else -> when (languageCodeHint()) {
            "hi" -> "चल रही है"
            "gu" -> "ચાલુ છે"
            else -> "In Progress"
        }
    }

    fun expenseCategoryLabel(cat: String): String = when (cat) {
        "All" -> when (languageCodeHint()) {
            "hi" -> "सभी"
            "gu" -> "બધા"
            else -> "All"
        }
        "Diesel" -> diesel
        "Toll" -> toll
        "Tax" -> taxPermit
        "Maintenance" -> when (languageCodeHint()) {
            "hi" -> "मरम्मत"
            "gu" -> "રિપેરિંગ"
            else -> "Maintenance"
        }
        "Allowance" -> when (languageCodeHint()) {
            "hi" -> "भत्ता"
            "gu" -> "ભથ્થું"
            else -> "Allowance"
        }
        "Other" -> other
        else -> cat
    }

    fun paymentStatusLabel(status: String): String = when (status) {
        "Paid" -> when (languageCodeHint()) {
            "hi" -> "भुगतान हो गया"
            "gu" -> "ચૂકવાઈ ગયું"
            else -> "Paid"
        }
        "Partial" -> when (languageCodeHint()) {
            "hi" -> "आंशिक"
            "gu" -> "અડધું"
            else -> "Partial"
        }
        else -> when (languageCodeHint()) {
            "hi" -> "बाकी"
            "gu" -> "બાકી"
            else -> "Pending"
        }
    }

    fun paymentModeLabel(mode: String): String = when (mode) {
        "Cash" -> when (languageCodeHint()) {
            "hi" -> "नकद"
            "gu" -> "રોકડ"
            else -> "Cash"
        }
        "Card" -> when (languageCodeHint()) {
            "hi" -> "कार्ड"
            "gu" -> "કાર્ડ"
            else -> "Card"
        }
        "Online" -> when (languageCodeHint()) {
            "hi" -> "ऑनलाइन"
            "gu" -> "ઓનલાઈન"
            else -> "Online"
        }
        else -> mode
    }

    fun staffRoleLabel(role: String): String = when (role) {
        "Driver" -> when (languageCodeHint()) {
            "hi" -> "ड्राइवर"
            "gu" -> "ડ્રાઇવર"
            else -> "Driver"
        }
        "Conductor" -> when (languageCodeHint()) {
            "hi" -> "कंडक्टर"
            "gu" -> "કંડક્ટર"
            else -> "Conductor"
        }
        "Helper" -> when (languageCodeHint()) {
            "hi" -> "हेल्पर"
            "gu" -> "હેલ્પર"
            else -> "Helper"
        }
        "Cleaner" -> when (languageCodeHint()) {
            "hi" -> "क्लीनर"
            "gu" -> "ક્લીનર"
            else -> "Cleaner"
        }
        else -> role
    }

    fun attendanceStatusLabel(status: String): String = when (status) {
        "Present" -> when (languageCodeHint()) {
            "hi" -> "उपस्थित"
            "gu" -> "હાજર"
            else -> "Present"
        }
        "Late" -> when (languageCodeHint()) {
            "hi" -> "देरी से"
            "gu" -> "મોડું"
            else -> "Late"
        }
        "Half-Day" -> when (languageCodeHint()) {
            "hi" -> "आधा दिन"
            "gu" -> "અડધો દિવસ"
            else -> "Half-Day"
        }
        else -> status
    }

    private fun languageCodeHint(): String = when (ok) {
        "ठीक है" -> "hi"
        "બરાબર" -> "gu"
        else -> "en"
    }
}
