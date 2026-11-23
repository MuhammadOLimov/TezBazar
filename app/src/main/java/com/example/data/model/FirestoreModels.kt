package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class OrderStatus(val stepIndex: Int, val defaultProgress: Double) {
    PLACED(0, 0.12),
    PREPARING(1, 0.34),
    ON_THE_WAY(2, 0.65),
    ARRIVING(3, 0.88),
    DELIVERED(4, 1.0),
    CANCELLED(-1, 0.0);

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PLACED
    }
}

enum class PaymentMethodType {
    UZCARD,
    HUMO,
    CASH;

    companion object {
        fun fromRaw(raw: String?): PaymentMethodType =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val languageCode: String = "uz",
    val preferredDistrict: String = "Yunusobod",
    val loyaltyPoints: Long = 150L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartEntry(
    @DocumentId val id: String = "",
    val productId: String = "",
    val titleUz: String = "",
    val titleRu: String = "",
    val titleEn: String = "",
    val categoryId: String = "",
    val unitPriceUzs: Long = 0L,
    val quantity: Long = 1L,
    val imageKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val status: String = OrderStatus.PLACED.name,
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val subtotalUzs: Long = 0L,
    val deliveryFeeUzs: Long = 0L,
    val discountUzs: Long = 0L,
    val totalUzs: Long = 0L,
    val paymentMethod: String = PaymentMethodType.CASH.name,
    val cardLastFour: String? = null,
    val district: String = "Yunusobod",
    val addressLine: String = "",
    val courierName: String = "",
    val courierPhone: String = "",
    val courierVehicle: String = "",
    val deliveryType: String = "COURIER",
    val pickupPointName: String? = null,
    val routeProgress: Double = 0.12,
    val etaMinutes: Long = 22L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: OrderStatus
        get() = OrderStatus.fromRaw(status)

    val parsedPaymentMethod: PaymentMethodType
        get() = PaymentMethodType.fromRaw(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val label: String = "",
    val district: String = "",
    val streetAddress: String = "",
    val entrance: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val primaryAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPaymentCard(
    @DocumentId val id: String = "",
    val brand: String = "UZCARD",
    val maskedNumber: String = "",
    val lastFour: String = "",
    val expiry: String = "",
    val holderName: String = "",
    val primaryCard: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiTasteProfile(
    @DocumentId val id: String = "",
    val topCategories: List<String> = emptyList(),
    val tasteSummary: String = "",
    val recentQueries: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
