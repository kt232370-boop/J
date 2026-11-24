const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated users cannot read or write accounts or bookmarks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      fanHandle: "@alice",
      email: "alice@example.com",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
});

test("Owner can create and partially update FanSite account without extra fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Fan",
      fanHandle: "@alicefan",
      email: "alice@example.com",
      photoUrl: "https://example.com/photo.jpg",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fanHandle: "@alice_vip",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / role escalation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId "id" key in payload
  await assertFails(
    aliceDb.collection("users").doc("another_doc").set({
      id: "another_doc",
      userId: ALICE_UID,
      displayName: "Alice",
      fanHandle: "@alice",
      email: "alice@example.com",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice account or bookmarks", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      fanHandle: "@alice",
      email: "alice@example.com",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      title: "FanSite Share",
      url: "https://share.google/s45hVkei7sq90GoI5",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fanHandle: "@hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_2").set({
      title: "Injected",
      url: "https://evil.example.com",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Owner can list, create, update, and delete bookmarks matching Kotlin repository", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Empty subcollection list succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_home").set({
      title: "FanSite Share",
      url: "https://share.google/s45hVkei7sq90GoI5",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_home").update({
      title: "FanSite Portal",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_future").set({
      title: "Future",
      url: "https://share.google/s45hVkei7sq90GoI5",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_home").delete()
  );
});
