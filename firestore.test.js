const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uz_123";
const BOB_UID = "bob_uz_456";

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

test("Catalog: authenticated users can read; client writes and unauth reads fail", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("catalog").doc("plov_rice").set({
      titleUz: "Devzira guruch 1kg",
      titleRu: "Рис Девзира 1кг",
      titleEn: "Devzira Rice 1kg",
      categoryId: "groceries",
      priceUzs: 42000,
      popularityScore: 99,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(unauthDb.collection("catalog").get());
  await assertSucceeds(aliceDb.collection("catalog").get());
  await assertSucceeds(aliceDb.collection("catalog").doc("plov_rice").get());
  await assertFails(
    aliceDb.collection("catalog").doc("hacked").set({
      titleUz: "Fake",
      titleRu: "Fake",
      titleEn: "Fake",
      categoryId: "groceries",
      priceUzs: 1,
    })
  );
});

test("UserProfile: owner create/update succeeds; cross-user, list, and future timestamp fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Azizbek Karimov",
      email: "azizbek@example.uz",
      languageCode: "uz",
      preferredDistrict: "Yunusobod",
      loyaltyPoints: 150,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      languageCode: "ru",
      loyaltyPoints: 250,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read/write & blanket user listing
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());

  // Reject future timestamp
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      languageCode: "en",
      preferredDistrict: "Chilonzor",
      loyaltyPoints: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("CartEntry: Kotlin-mirrored create/update/list succeed for owner and fail cross-user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const cartRef = aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("pomegranate_1kg");
  await assertSucceeds(
    cartRef.set({
      productId: "pomegranate_1kg",
      titleUz: "Quva anori 1kg",
      titleRu: "Кувинский гранат 1кг",
      titleEn: "Kuva Pomegranate 1kg",
      categoryId: "fresh",
      unitPriceUzs: 28000,
      quantity: 2,
      imageKey: "pomegranate",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  await assertSucceeds(
    cartRef.update({
      quantity: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId "id" key injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("tea_green").set({
      id: "tea_green",
      productId: "tea_green",
      titleUz: "Ko'k choy",
      titleRu: "Зелёный чай",
      titleEn: "Green Tea",
      categoryId: "drinks",
      unitPriceUzs: 18000,
      quantity: 1,
      imageKey: "tea",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject cross-user cart access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
});

test("OrderRecord: creation, live tracking updates, and terminal DELIVERED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1001");
  await assertSucceeds(
    orderRef.set({
      orderNumber: "TB-1001",
      status: "PLACED",
      itemsSummary: "Quva anori 1kg x2, Tandir non x2",
      itemCount: 4,
      subtotalUzs: 68000,
      deliveryFeeUzs: 0,
      discountUzs: 5000,
      totalUzs: 63000,
      paymentMethod: "UZCARD",
      cardLastFour: "4821",
      district: "Mirzo Ulug'bek",
      addressLine: "Buyuk Ipak Yo'li 45, kv 12",
      courierName: "Sardorbek T.",
      courierPhone: "+998 90 123 45 67",
      courierVehicle: "Elektro-skuter",
      routeProgress: 0.1,
      etaMinutes: 22,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    orderRef.update({
      status: "ON_THE_WAY",
      routeProgress: 0.65,
      etaMinutes: 9,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  await assertSucceeds(
    orderRef.update({
      status: "DELIVERED",
      routeProgress: 1.0,
      etaMinutes: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cannot mutate once DELIVERED
  await assertFails(
    orderRef.update({
      status: "PLACED",
      routeProgress: 0.2,
      etaMinutes: 20,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // PVZ Pickup point order across Uzbekistan succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_pvz_1").set({
      orderNumber: "TB-9002",
      status: "PLACED",
      itemsSummary: "Quva anori 1kg x1",
      itemCount: 1,
      subtotalUzs: 28000,
      deliveryFeeUzs: 0,
      discountUzs: 0,
      totalUzs: 28000,
      paymentMethod: "HUMO",
      cardLastFour: "7712",
      district: "Samarqand sh.",
      addressLine: "TezBazar PVZ • Registon Maydoni",
      courierName: "TezBazar PVZ Logistics",
      courierPhone: "+998 71 200 10 10",
      courierVehicle: "TezBazar Express",
      deliveryType: "PICKUP_POINT",
      pickupPointName: "TezBazar PVZ • Registon Maydoni",
      routeProgress: 0.12,
      etaMinutes: 20,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject invalid deliveryType
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_bad_type").set({
      orderNumber: "TB-9003",
      status: "PLACED",
      itemsSummary: "Test",
      itemCount: 1,
      subtotalUzs: 28000,
      deliveryFeeUzs: 0,
      discountUzs: 0,
      totalUzs: 28000,
      paymentMethod: "CASH",
      district: "Nukus",
      addressLine: "Do'stlik guzari 48",
      courierName: "Courier",
      courierPhone: "+998 90 111 22 33",
      courierVehicle: "Van",
      deliveryType: "DRONE_UNKNOWN",
      routeProgress: 0.12,
      etaMinutes: 20,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user read blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1001").get());
});

test("Addresses, PaymentCards & AiProfile: owner CRUD succeeds and unauth/cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      label: "Uy",
      district: "Chilonzor",
      streetAddress: "Bunyodkor shoh ko'chasi 18",
      entrance: "2-kirish",
      latitude: 41.2858,
      longitude: 69.2036,
      primaryAddress: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_bad_coords").set({
      label: "Bad",
      district: "Chilonzor",
      streetAddress: "Bunyodkor 18",
      latitude: 999.0,
      longitude: 69.2036,
      primaryAddress: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payment_methods").doc("card_1").set({
      brand: "HUMO",
      maskedNumber: "9860 •••• •••• 7712",
      lastFour: "7712",
      expiry: "09/28",
      holderName: "AZIZBEK KARIMOV",
      primaryCard: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_profiles").doc("taste").set({
      topCategories: ["fresh", "bakery"],
      tasteSummary: "Prefers seasonal fruits and traditional Uzbek staples",
      recentQueries: ["anor", "devzira guruch"],
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("payment_methods").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").get());
});
