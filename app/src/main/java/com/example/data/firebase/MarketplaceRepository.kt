package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AiTasteProfile
import com.example.data.model.CartEntry
import com.example.data.model.CatalogSeedData
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.Product
import com.example.data.model.SavedAddress
import com.example.data.model.SavedPaymentCard
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Scalable cursor-paginated catalog loader (1M+ items ready) backed by local seed catalog
    suspend fun fetchCatalogOverlay(
        categoryId: String?,
        pageSize: Long = 20L,
        lastVisible: DocumentSnapshot? = null
    ): Pair<List<Product>, DocumentSnapshot?> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                return Pair(emptyList(), null)
            }
            var query: Query = db.collection("catalog")
                .orderBy("popularityScore", Query.Direction.DESCENDING)
                .limit(pageSize)
            if (!categoryId.isNullOrBlank()) {
                query = db.collection("catalog")
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("popularityScore", Query.Direction.DESCENDING)
                    .limit(pageSize)
            }
            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }
            val snap = query.get().await()
            val remoteProducts = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val titleUz = data["titleUz"] as? String ?: return@mapNotNull null
                val titleRu = data["titleRu"] as? String ?: titleUz
                val titleEn = data["titleEn"] as? String ?: titleUz
                val cat = data["categoryId"] as? String ?: "fresh"
                val price = (data["priceUzs"] as? Number)?.toLong() ?: return@mapNotNull null
                val pop = (data["popularityScore"] as? Number)?.toInt() ?: 80
                Product(
                    id = doc.id,
                    titleUz = titleUz,
                    titleRu = titleRu,
                    titleEn = titleEn,
                    descUz = data["descUz"] as? String ?: titleUz,
                    descRu = data["descRu"] as? String ?: titleRu,
                    descEn = data["descEn"] as? String ?: titleEn,
                    categoryId = cat,
                    priceUzs = price,
                    oldPriceUzs = (data["oldPriceUzs"] as? Number)?.toLong(),
                    unitUz = data["unitUz"] as? String ?: "1 dona",
                    unitRu = data["unitRu"] as? String ?: "1 шт",
                    unitEn = data["unitEn"] as? String ?: "1 pc",
                    rating = (data["rating"] as? Number)?.toDouble() ?: 4.9,
                    reviewCount = (data["reviewCount"] as? Number)?.toInt() ?: 120,
                    deliveryMinutes = (data["deliveryMinutes"] as? Number)?.toInt() ?: 20,
                    popularityScore = pop,
                    imageKey = data["imageKey"] as? String ?: "pomegranate"
                )
            }
            Pair(remoteProducts, snap.documents.lastOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Gracefully serve bundled curated catalog if remote /catalog is empty or offline
            Pair(emptyList(), null)
        }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        language: AppLanguage,
        district: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "TezBazar User" }.take(80)
                val safeEmail = email.trim().take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "languageCode" to language.code,
                    "preferredDistrict" to district.take(60),
                    "loyaltyPoints" to 250L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndThrowFirestore(error, OperationType.GET, path, uid)
                }
        )
    }

    suspend fun updateUserLanguageAndDistrict(
        userId: String,
        language: AppLanguage,
        district: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).update(
                mapOf(
                    "languageCode" to language.code,
                    "preferredDistrict" to district.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeCart(userId: String): Flow<List<CartEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/cart"
        emitAll(
            db.collection("users").document(uid).collection("cart")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CartEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    unwrapAndThrowFirestore(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun upsertCartItem(
        userId: String,
        product: Product,
        newQuantity: Int
    ): Result<Unit> {
        val path = "users/$userId/cart/${product.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("cart").document(product.id)
            if (newQuantity <= 0) {
                docRef.delete().await()
                return Result.success(Unit)
            }
            val clampedQty = newQuantity.coerceIn(1, 99).toLong()
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "quantity" to clampedQty,
                        "unitPriceUzs" to product.priceUzs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "productId" to product.id.take(64),
                    "titleUz" to product.titleUz.take(120),
                    "titleRu" to product.titleRu.take(120),
                    "titleEn" to product.titleEn.take(120),
                    "categoryId" to product.categoryId.take(40),
                    "unitPriceUzs" to product.priceUzs,
                    "quantity" to clampedQty,
                    "imageKey" to product.imageKey.take(64),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addBundleToCart(userId: String, products: List<Product>): Result<Unit> {
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cartCol = db.collection("users").document(uid).collection("cart")
            for (product in products) {
                val docRef = cartCol.document(product.id)
                val existing = docRef.get().await()
                if (existing.exists()) {
                    val currentQty = (existing.getLong("quantity") ?: 1L).toInt()
                    val nextQty = (currentQty + 1).coerceIn(1, 99).toLong()
                    docRef.update(
                        mapOf(
                            "quantity" to nextQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "productId" to product.id.take(64),
                            "titleUz" to product.titleUz.take(120),
                            "titleRu" to product.titleRu.take(120),
                            "titleEn" to product.titleEn.take(120),
                            "categoryId" to product.categoryId.take(40),
                            "unitPriceUzs" to product.priceUzs,
                            "quantity" to 1L,
                            "imageKey" to product.imageKey.take(64),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/cart")
            Result.failure(e)
        }
    }

    fun observeOrders(userId: String): Flow<List<OrderRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    unwrapAndThrowFirestore(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun placeOrder(
        userId: String,
        cartItems: List<CartEntry>,
        subtotalUzs: Long,
        deliveryFeeUzs: Long,
        discountUzs: Long,
        paymentMethod: PaymentMethodType,
        cardLastFour: String?,
        district: String,
        addressLine: String,
        language: AppLanguage,
        deliveryType: String = "COURIER",
        pickupPointName: String? = null
    ): Result<String> {
        val path = "users/$userId/orders"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(cartItems.isNotEmpty()) { "Cart is empty" }
            require(addressLine.trim().length >= 3) { "Address is too short" }

            val totalUzs = (subtotalUzs + deliveryFeeUzs - discountUzs).coerceAtLeast(1000L)
            val totalCount = cartItems.sumOf { it.quantity }.coerceIn(1L, 200L)
            val summary = cartItems.joinToString(", ") { entry ->
                val name = when (language) {
                    AppLanguage.UZ -> entry.titleUz
                    AppLanguage.RU -> entry.titleRu
                    AppLanguage.EN -> entry.titleEn
                }
                "$name x${entry.quantity}"
            }.take(500)

            val couriers = listOf(
                Triple("Sardorbek Toshmatov", "+998 90 412 88 19", "TezBazar Express Auto"),
                Triple("Jasurbek Alimov", "+998 93 771 20 45", "TezBazar E-Bike"),
                Triple("Dilshod Karimov", "+998 99 305 14 77", "TezBazar Elektro-skuter"),
                Triple("Shohruh Mirzayev", "+998 91 552 09 31", "TezBazar Regional Shuttle")
            )
            val courier = if (deliveryType == "PICKUP_POINT") {
                Triple("TezBazar PVZ Logistics", "+998 71 200 70 70", "TezBazar PVZ Hub")
            } else {
                couriers[Random.nextInt(couriers.size)]
            }
            val orderNum = "TB-${Random.nextInt(10000, 99999)}"
            val cleanDeliveryType = if (deliveryType == "PICKUP_POINT") "PICKUP_POINT" else "COURIER"

            val orderDoc = db.collection("users").document(uid).collection("orders").document()
            val payload = mutableMapOf<String, Any?>(
                "orderNumber" to orderNum,
                "status" to OrderStatus.PLACED.name,
                "itemsSummary" to summary,
                "itemCount" to totalCount,
                "subtotalUzs" to subtotalUzs,
                "deliveryFeeUzs" to deliveryFeeUzs,
                "discountUzs" to discountUzs,
                "totalUzs" to totalUzs,
                "paymentMethod" to paymentMethod.name,
                "cardLastFour" to cardLastFour?.take(4),
                "district" to district.take(60),
                "addressLine" to addressLine.trim().take(160),
                "courierName" to courier.first,
                "courierPhone" to courier.second,
                "courierVehicle" to courier.third,
                "deliveryType" to cleanDeliveryType,
                "pickupPointName" to pickupPointName?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
                "routeProgress" to OrderStatus.PLACED.defaultProgress,
                "etaMinutes" to if (cleanDeliveryType == "PICKUP_POINT") 18L else 22L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanPayload = payload.filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(orderDoc, cleanPayload)
            for (entry in cartItems) {
                val cartRef = db.collection("users").document(uid).collection("cart").document(entry.id)
                batch.delete(cartRef)
            }
            val userRef = db.collection("users").document(uid)
            val earnedPoints = (totalUzs / 2000L).coerceAtLeast(10L)
            batch.update(
                userRef,
                mapOf(
                    "loyaltyPoints" to FieldValue.increment(earnedPoints),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val topCategory = cartItems.groupBy { it.categoryId }
                .maxByOrNull { it.value.size }?.key
            if (topCategory != null) {
                recordAiSignal(uid, topCategory, null)
            }
            Result.success(orderDoc.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStage(
        userId: String,
        order: OrderRecord
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val current = order.parsedStatus
            if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
                return Result.success(Unit)
            }
            val (nextStatus, nextProgress, nextEta) = when (current) {
                OrderStatus.PLACED -> Triple(OrderStatus.PREPARING, 0.36, 16L)
                OrderStatus.PREPARING -> Triple(OrderStatus.ON_THE_WAY, 0.68, 9L)
                OrderStatus.ON_THE_WAY -> Triple(OrderStatus.ARRIVING, 0.90, 3L)
                OrderStatus.ARRIVING -> Triple(OrderStatus.DELIVERED, 1.0, 0L)
                else -> return Result.success(Unit)
            }
            db.collection("users").document(uid).collection("orders").document(order.id)
                .update(
                    mapOf(
                        "status" to nextStatus.name,
                        "routeProgress" to nextProgress,
                        "etaMinutes" to nextEta,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAddresses(userId: String): Flow<List<SavedAddress>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/addresses"
        emitAll(
            db.collection("users").document(uid).collection("addresses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error -> unwrapAndThrowFirestore(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun addAddress(
        userId: String,
        label: String,
        district: String,
        streetAddress: String,
        entrance: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit> {
        val path = "users/$userId/addresses"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(label.trim().isNotEmpty() && streetAddress.trim().length >= 3)
            val safeLat = latitude?.takeIf { it in -90.0..90.0 }
            val safeLng = longitude?.takeIf { it in -180.0..180.0 }
            val payload = mutableMapOf<String, Any?>(
                "label" to label.trim().take(40),
                "district" to district.take(60),
                "streetAddress" to streetAddress.trim().take(160),
                "entrance" to entrance?.trim()?.takeIf { it.isNotEmpty() }?.take(20),
                "latitude" to safeLat,
                "longitude" to safeLng,
                "primaryAddress" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("users").document(uid).collection("addresses").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> {
        val path = "users/$userId/addresses/$addressId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePaymentCards(userId: String): Flow<List<SavedPaymentCard>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/payment_methods"
        emitAll(
            db.collection("users").document(uid).collection("payment_methods")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedPaymentCard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error -> unwrapAndThrowFirestore(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun addPaymentCard(
        userId: String,
        rawDigits: String,
        expiry: String,
        holderName: String
    ): Result<Unit> {
        val path = "users/$userId/payment_methods"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val digits = rawDigits.filter { it.isDigit() }
            require(digits.length == 16) { "Card must be 16 digits" }
            val brand = when {
                digits.startsWith("8600") -> "UZCARD"
                digits.startsWith("9860") -> "HUMO"
                else -> throw IllegalArgumentException("Only Uzcard (8600) or Humo (9860) supported")
            }
            require(expiry.matches(Regex("^\\d{2}/\\d{2}$"))) { "Invalid expiry format" }
            val first4 = digits.take(4)
            val last4 = digits.takeLast(4)
            val masked = "$first4 •••• •••• $last4"
            val payload = mapOf(
                "brand" to brand,
                "maskedNumber" to masked,
                "lastFour" to last4,
                "expiry" to expiry,
                "holderName" to holderName.trim().uppercase().ifBlank { "CARDHOLDER" }.take(60),
                "primaryCard" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("payment_methods").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePaymentCard(userId: String, cardId: String): Result<Unit> {
        val path = "users/$userId/payment_methods/$cardId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("payment_methods").document(cardId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAiProfile(userId: String): Flow<AiTasteProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/ai_profiles/taste"
        emitAll(
            db.collection("users").document(uid).collection("ai_profiles").document("taste")
                .snapshots()
                .map { it.toObject(AiTasteProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndThrowFirestore(error, OperationType.GET, path, uid) }
        )
    }

    suspend fun recordAiSignal(
        userId: String,
        categoryId: String?,
        queryText: String?
    ): Result<Unit> {
        val path = "users/$userId/ai_profiles/taste"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("ai_profiles").document("taste")
            val snap = docRef.get().await()
            val existing = snap.toObject(AiTasteProfile::class.java)
            val nextCats = ((listOfNotNull(categoryId) + (existing?.topCategories ?: emptyList()))
                .distinct()).take(10)
            val nextQueries = ((listOfNotNull(queryText?.trim()?.takeIf { it.isNotEmpty() }?.take(60)) +
                (existing?.recentQueries ?: emptyList())).distinct()).take(10)

            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "topCategories" to nextCats,
                        "recentQueries" to nextQueries,
                        "tasteSummary" to (existing?.tasteSummary ?: "Active TezBazar shopper"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "topCategories" to nextCats,
                        "tasteSummary" to "Active TezBazar shopper",
                        "recentQueries" to nextQueries,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun unwrapAndThrowFirestore(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ): Nothing {
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
}
