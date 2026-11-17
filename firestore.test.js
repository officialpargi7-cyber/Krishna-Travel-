const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const safeTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER1_UID = "owner_1";
const OWNER2_UID = "owner_2";
const MANAGER1_UID = "manager_1";
const OUTSIDER_UID = "outsider_99";
const FLEET_ID = "fleet_krishna_travels_hq";
const REGISTRY_ID = "krishna_hq";
const BUS_ID = "bus_gj01dz8325";
const TRIP_ID = "trip_001";

const OWNER1_USER = "krishna_owner1";
const OWNER1_DIGEST = "eb176276dc9843e85a38394ffe8bd49fff56dcab686e0a78a7bc1a7a3ecf242f";
const OWNER2_USER = "krishna_owner2";
const OWNER2_DIGEST = "4e7fefa0dbc087a2614ec20573a74175527e8fe39e04387fb16d12f70d072d0f";
const MGR1_USER = "krishna_mgr1";
const MGR1_DIGEST = "3235c408a42c1f27ddd0a00bc1c8d7c916a77bf07c30ced88568369187c85954";
const MGR2_USER = "krishna_mgr2";
const MGR2_DIGEST = "4e1b6f6e6ccf0123c9cf3f95518cfab8b7d25a88e4ece470767bdca7e3f97a9f";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedFleetWithOwnersAndManager() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date();
    const memberUids = [OWNER1_UID, OWNER2_UID, MANAGER1_UID];
    const ownerUids = [OWNER1_UID, OWNER2_UID];

    await db.collection("fleet_access_registry").doc(REGISTRY_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      initializedBy: OWNER1_UID,
      ownerUids,
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).set({
      name: "Krishna Travels",
      createdBy: OWNER1_UID,
      ownerUids,
      memberUids,
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).collection("members").doc(OWNER1_UID).set({
      fleetId: FLEET_ID,
      userId: OWNER1_UID,
      displayName: "Owner One",
      email: "owner1@krishna.com",
      role: "owner",
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      ownerUids,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).collection("members").doc(OWNER2_UID).set({
      fleetId: FLEET_ID,
      userId: OWNER2_UID,
      displayName: "Owner Two",
      email: "owner2@krishna.com",
      role: "owner",
      slotUsername: OWNER2_USER,
      slotAuthDigest: OWNER2_DIGEST,
      ownerUids,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).collection("members").doc(MANAGER1_UID).set({
      fleetId: FLEET_ID,
      userId: MANAGER1_UID,
      displayName: "Manager One",
      email: "manager1@krishna.com",
      role: "manager",
      slotUsername: MGR1_USER,
      slotAuthDigest: MGR1_DIGEST,
      ownerUids,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).collection("buses").doc(BUS_ID).set({
      fleetId: FLEET_ID,
      busNumber: "GJ01DZ8325",
      coachName: "Krishna 2+1 AC Sleeper",
      totalCabins: 30,
      defaultRoute: "Ahmedabad - Udaipur",
      active: true,
      memberUids,
      createdByUid: OWNER1_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("fleets").doc(FLEET_ID).collection("trips").doc(TRIP_ID).set({
      fleetId: FLEET_ID,
      busId: BUS_ID,
      busNumber: "GJ01DZ8325",
      routeName: "Ahmedabad -> Udaipur",
      origin: "Ahmedabad",
      destination: "Udaipur",
      tripDate: "2026-11-15",
      departureTime: "21:30",
      driverName: "Ramesh Bhai",
      conductorName: "Suresh",
      status: "in_progress",
      memberUids,
      createdByUid: OWNER1_UID,
      createdByName: "Owner One",
      createdByRole: "owner",
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated user cannot read or list fleet trips or registry", async () => {
  await seedFleetWithOwnersAndManager();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("fleets").doc(FLEET_ID).collection("trips").get());
  await assertFails(unauthDb.collection("fleets").doc(FLEET_ID).collection("trips").doc(TRIP_ID).get());
  await assertFails(unauthDb.collection("fleet_access_registry").doc(REGISTRY_ID).get());
});

test("Outsider authenticated user cannot read, list, create fake fleet, or join with invalid digest", async () => {
  await seedFleetWithOwnersAndManager();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const fakeDigest = "0".repeat(64);
  const now = safeTimestamp();

  // Unfiltered list or another user's filter must fail
  await assertFails(
    outsiderDb.collection("fleets").doc(FLEET_ID).collection("trips").get()
  );
  await assertFails(
    outsiderDb.collection("fleets").doc(FLEET_ID).collection("trips").where("memberUids", "array-contains", OWNER1_UID).get()
  );
  // Own-UID filter returns 0 docs (never leaks any trip)
  const snap = await assertSucceeds(
    outsiderDb.collection("fleets").doc(FLEET_ID).collection("trips").where("memberUids", "array-contains", OUTSIDER_UID).get()
  );
  if (!snap.empty) throw new Error("Outsider saw fleet trips!");
  // Direct doc read must fail
  await assertFails(outsiderDb.collection("fleets").doc(FLEET_ID).collection("trips").doc(TRIP_ID).get());

  // Outsider cannot join with bogus credentials or read owner_vault
  await assertFails(
    outsiderDb.collection("fleets").doc(FLEET_ID).collection("members").doc(OUTSIDER_UID).set({
      fleetId: FLEET_ID,
      userId: OUTSIDER_UID,
      displayName: "Hacker",
      role: "owner",
      slotUsername: "hacker_user",
      slotAuthDigest: fakeDigest,
      ownerUids: [OWNER1_UID, OUTSIDER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    outsiderDb.collection("fleets").doc(FLEET_ID).collection("owner_vault").doc("secret_1").get()
  );
});

test("Only authorized 10 slots can initialize and join fleet; legacy fleet_invites is denied", async () => {
  const owner1Db = testEnv.authenticatedContext(OWNER1_UID).firestore();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID).firestore();
  const mgr2Uid = "manager_2";
  const mgr2Db = testEnv.authenticatedContext(mgr2Uid).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const now = safeTimestamp();

  // Legacy fleet_invites collection must be completely denied
  await assertFails(
    owner1Db.collection("fleet_invites").doc("MGR-7788").set({
      fleetId: FLEET_ID,
      role: "manager",
      createdBy: OWNER1_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Outsider with bogus digest cannot initialize registry or fleet
  await assertFails(
    outsiderDb.collection("fleet_access_registry").doc(REGISTRY_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      initializedBy: OUTSIDER_UID,
      ownerUids: [OUTSIDER_UID],
      slotUsername: "krishna_owner1",
      slotAuthDigest: "f".repeat(64),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manager digest CANNOT initialize fleet_access_registry as owner
  await assertFails(
    mgr2Db.collection("fleet_access_registry").doc(REGISTRY_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      initializedBy: mgr2Uid,
      ownerUids: [mgr2Uid],
      slotUsername: MGR2_USER,
      slotAuthDigest: MGR2_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner 1 initializes shared fleet + access registry + member + membership
  await assertSucceeds(
    owner1Db.collection("fleets").doc(FLEET_ID).set({
      name: "Krishna Travels",
      createdBy: OWNER1_UID,
      ownerUids: [OWNER1_UID],
      memberUids: [OWNER1_UID],
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner1Db.collection("fleet_access_registry").doc(REGISTRY_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      initializedBy: OWNER1_UID,
      ownerUids: [OWNER1_UID],
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner1Db.collection("fleets").doc(FLEET_ID).collection("members").doc(OWNER1_UID).set({
      fleetId: FLEET_ID,
      userId: OWNER1_UID,
      displayName: "Owner One",
      email: "owner1@krishna.com",
      role: "owner",
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      ownerUids: [OWNER1_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner1Db.collection("users").doc(OWNER1_UID).collection("memberships").doc(FLEET_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      userId: OWNER1_UID,
      role: "owner",
      slotUsername: OWNER1_USER,
      slotAuthDigest: OWNER1_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manager CANNOT claim role=owner using Manager digest
  await assertFails(
    mgr2Db.collection("fleets").doc(FLEET_ID).collection("members").doc(mgr2Uid).set({
      fleetId: FLEET_ID,
      userId: mgr2Uid,
      displayName: "Manager Two",
      role: "owner",
      slotUsername: MGR2_USER,
      slotAuthDigest: MGR2_DIGEST,
      ownerUids: [OWNER1_UID, mgr2Uid],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner 2 unlocks cleanly with Owner 2 digest
  await assertSucceeds(
    owner2Db.collection("fleets").doc(FLEET_ID).collection("members").doc(OWNER2_UID).set({
      fleetId: FLEET_ID,
      userId: OWNER2_UID,
      displayName: "Owner Two",
      role: "owner",
      slotUsername: OWNER2_USER,
      slotAuthDigest: OWNER2_DIGEST,
      ownerUids: [OWNER1_UID, OWNER2_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner2Db.collection("users").doc(OWNER2_UID).collection("memberships").doc(FLEET_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      userId: OWNER2_UID,
      role: "owner",
      slotUsername: OWNER2_USER,
      slotAuthDigest: OWNER2_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner2Db.collection("fleets").doc(FLEET_ID).update({
      ownerUids: [OWNER1_UID, OWNER2_UID],
      memberUids: [OWNER1_UID, OWNER2_UID],
      updatedAt: safeTimestamp(),
    })
  );

  // Manager 2 unlocks cleanly with Manager 2 digest
  await assertSucceeds(
    mgr2Db.collection("fleets").doc(FLEET_ID).collection("members").doc(mgr2Uid).set({
      fleetId: FLEET_ID,
      userId: mgr2Uid,
      displayName: "Manager Two",
      role: "manager",
      slotUsername: MGR2_USER,
      slotAuthDigest: MGR2_DIGEST,
      ownerUids: [OWNER1_UID, OWNER2_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    mgr2Db.collection("users").doc(mgr2Uid).collection("memberships").doc(FLEET_ID).set({
      fleetId: FLEET_ID,
      fleetName: "Krishna Travels",
      userId: mgr2Uid,
      role: "manager",
      slotUsername: MGR2_USER,
      slotAuthDigest: MGR2_DIGEST,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    mgr2Db.collection("fleets").doc(FLEET_ID).update({
      memberUids: [OWNER1_UID, OWNER2_UID, mgr2Uid],
      ownerUids: [OWNER1_UID, OWNER2_UID],
      updatedAt: safeTimestamp(),
    })
  );
});

test("Owner-only owner_vault blocks Managers and permits Owners", async () => {
  await seedFleetWithOwnersAndManager();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID).firestore();
  const managerDb = testEnv.authenticatedContext(MANAGER1_UID).firestore();

  const ownerVaultDoc = owner2Db.collection("fleets").doc(FLEET_ID).collection("owner_vault").doc("audit_1");
  await assertSucceeds(ownerVaultDoc.set({ note: "Owner private ledger", updatedAt: safeTimestamp() }));
  await assertSucceeds(ownerVaultDoc.get());

  // Manager MUST get PERMISSION_DENIED on both read and write to owner_vault
  const mgrVaultDoc = managerDb.collection("fleets").doc(FLEET_ID).collection("owner_vault").doc("audit_1");
  await assertFails(mgrVaultDoc.get());
  await assertFails(mgrVaultDoc.set({ note: "Tamper", updatedAt: safeTimestamp() }));
});

test("Manager can create attendance, expense, booking, cabin photo, and no-fare record, but CANNOT update or delete any of them", async () => {
  await seedFleetWithOwnersAndManager();
  const managerDb = testEnv.authenticatedContext(MANAGER1_UID).firestore();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID).firestore();
  const now = safeTimestamp();
  const memberUids = [OWNER1_UID, OWNER2_UID, MANAGER1_UID];

  // Manager creates attendance
  const attRef = managerDb.collection("fleets").doc(FLEET_ID).collection("attendance").doc("att_1");
  await assertSucceeds(attRef.set({
    fleetId: FLEET_ID,
    tripId: TRIP_ID,
    staffName: "Ramesh Driver",
    staffRole: "Driver",
    status: "Present",
    photoBase64: "attphoto123",
    capturedLatitude: 23.0225,
    capturedLongitude: 72.5714,
    capturedLocationLabel: "Ahmedabad",
    memberUids,
    createdByUid: MANAGER1_UID,
    createdByName: "Manager One",
    createdByRole: "manager",
    createdAt: now,
    updatedAt: now,
  }));
  await assertFails(attRef.update({ status: "Late", updatedAt: safeTimestamp() }));
  await assertFails(attRef.delete());

  // Manager creates diesel expense
  const expRef = managerDb.collection("fleets").doc(FLEET_ID).collection("expenses").doc("exp_1");
  await assertSucceeds(expRef.set({
    fleetId: FLEET_ID,
    tripId: TRIP_ID,
    category: "Diesel",
    amount: 8500,
    liters: 90.5,
    ratePerLiter: 93.92,
    vendorOrPlaza: "HP Highway Pump",
    paymentMode: "Cash",
    memberUids,
    createdByUid: MANAGER1_UID,
    createdByName: "Manager One",
    createdByRole: "manager",
    createdAt: now,
    updatedAt: now,
  }));
  await assertFails(expRef.update({ amount: 12000, updatedAt: safeTimestamp() }));
  await assertFails(expRef.delete());

  // Second Owner (OWNER2_UID) CAN update Manager's uploaded expense
  await assertSucceeds(
    owner2Db.collection("fleets").doc(FLEET_ID).collection("expenses").doc("exp_1").update({
      amount: 8600,
      updatedAt: safeTimestamp(),
    })
  );

  // Manager creates booking bill and CANNOT edit or delete it
  const bookingRef = managerDb.collection("fleets").doc(FLEET_ID).collection("bookings").doc("bk_1");
  await assertSucceeds(bookingRef.set({
    fleetId: FLEET_ID,
    tripId: TRIP_ID,
    invoiceNumber: "KT-1024-14",
    passengerName: "Aarav Patel",
    passengerPhone: "9876501234",
    cabinNumbers: [14],
    berthNumbers: "C14",
    seatCount: 1,
    boardingPoint: "Ahmedabad",
    droppingPoint: "Udaipur",
    fareAmount: 1000,
    paidAmount: 1000,
    paymentStatus: "Paid",
    paymentMode: "Cash",
    memberUids,
    createdByUid: MANAGER1_UID,
    createdByName: "Manager One",
    createdByRole: "manager",
    createdAt: now,
    updatedAt: now,
  }));
  await assertFails(bookingRef.update({ paidAmount: 500, updatedAt: safeTimestamp() }));
  await assertFails(bookingRef.delete());

  // Manager creates sleeper cabin photo for cabin 14 (verifiedByOwner must be false)
  const photoRef = managerDb.collection("fleets").doc(FLEET_ID).collection("compartment_photos").doc("photo_14");
  await assertSucceeds(photoRef.set({
    fleetId: FLEET_ID,
    tripId: TRIP_ID,
    cabinNumber: 14,
    berthLabel: "Cabin C14 • Lower",
    passengerName: "Aarav Patel",
    conditionNote: "Clean sleeper cabin ready",
    photoBase64: "abc123base64data",
    verifiedByOwner: false,
    memberUids,
    createdByUid: MANAGER1_UID,
    createdByName: "Manager One",
    createdByRole: "manager",
    createdAt: now,
    updatedAt: now,
  }));

  // Manager CANNOT verify or edit cabin photo once uploaded
  await assertFails(photoRef.update({ verifiedByOwner: true, updatedAt: safeTimestamp() }));
  await assertFails(photoRef.delete());

  // Manager CANNOT create cabin photo with cabinNumber > 30
  await assertFails(
    managerDb.collection("fleets").doc(FLEET_ID).collection("compartment_photos").doc("photo_31").set({
      fleetId: FLEET_ID,
      tripId: TRIP_ID,
      cabinNumber: 31,
      berthLabel: "Cabin C31",
      photoBase64: "abc123",
      verifiedByOwner: false,
      memberUids,
      createdByUid: MANAGER1_UID,
      createdByName: "Manager One",
      createdByRole: "manager",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manager can create No-Fare record (name, mobile, photo) with resolvedByOwner=false, CANNOT edit/delete it
  const noFareRef = managerDb.collection("fleets").doc(FLEET_ID).collection("no_fare_records").doc("nf_1");
  await assertSucceeds(noFareRef.set({
    fleetId: FLEET_ID,
    tripId: TRIP_ID,
    personName: "Vikram Joshi",
    mobileNumber: "9876543210",
    cabinNumber: 18,
    expectedFare: 1200,
    reasonOrNote: "Did not pay fare at boarding",
    photoBase64: "nofarebase64",
    resolvedByOwner: false,
    memberUids,
    createdByUid: MANAGER1_UID,
    createdByName: "Manager One",
    createdByRole: "manager",
    createdAt: now,
    updatedAt: now,
  }));
  await assertFails(noFareRef.update({ resolvedByOwner: true, updatedAt: safeTimestamp() }));
  await assertFails(noFareRef.delete());
  await assertSucceeds(
    owner2Db.collection("fleets").doc(FLEET_ID).collection("no_fare_records").doc("nf_1").update({
      resolvedByOwner: true,
      updatedAt: safeTimestamp(),
    })
  );
});

test("Manager cannot self-promote role to owner", async () => {
  await seedFleetWithOwnersAndManager();
  const managerDb = testEnv.authenticatedContext(MANAGER1_UID).firestore();
  await assertFails(
    managerDb.collection("fleets").doc(FLEET_ID).collection("members").doc(MANAGER1_UID).update({
      role: "owner",
      slotUsername: MGR1_USER,
      slotAuthDigest: MGR1_DIGEST,
      updatedAt: safeTimestamp(),
    })
  );
});
