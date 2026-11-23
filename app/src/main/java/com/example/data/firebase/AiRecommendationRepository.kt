package com.example.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiTasteProfile
import com.example.data.model.CartEntry
import com.example.data.model.Product
import com.example.data.model.UzbekistanGeoCatalog
import com.example.i18n.AppLanguage
import com.example.ui.viewmodel.AiChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

data class AiRecommendationBundle(
    val bundleTitle: String,
    val reasoning: String,
    val products: List<Product>
)

data class AiSearchResult(
    val detectedConcept: String,
    val explanation: String,
    val products: List<Product>
)

private const val TAG = "AiRecommendationRepo"
private const val AI_TIMEOUT_MS = 60_000L
private const val AI_CACHE_PREFS = "tezbazar_ai_cache"
private const val AI_CACHE_TTL_MS = 12 * 60 * 60 * 1000L // 12 hours

class AiRecommendationRepository(
    private val appContext: Context? = null
) {

    private val recommendationSchema = Schema.obj(
        properties = mapOf(
            "bundleTitle" to Schema.string(description = "Short enticing bundle title in requested language"),
            "reasoning" to Schema.string(description = "1-2 sentence personalized explanation mentioning user's Uzbekistan region/city or taste in requested language"),
            "productIds" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 matching product IDs strictly from the provided catalog"
            )
        )
    )

    private val visualSearchSchema = Schema.obj(
        properties = mapOf(
            "detectedConcept" to Schema.string(description = "What dish, ingredient, or product was identified in requested language"),
            "explanation" to Schema.string(description = "1-2 sentence shopping and culinary match explanation in requested language"),
            "productIds" to Schema.array(
                items = Schema.string(),
                description = "2 to 5 matching product IDs strictly from the provided catalog"
            )
        )
    )

    private val recommendationCache = mutableMapOf<String, AiRecommendationBundle>()

    private fun readDiskCache(cacheKey: String, catalog: List<Product>): AiRecommendationBundle? {
        val prefs = appContext?.getSharedPreferences(AI_CACHE_PREFS, Context.MODE_PRIVATE) ?: return null
        val raw = prefs.getString("rec_$cacheKey", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val savedAt = json.optLong("savedAt", 0L)
            if (System.currentTimeMillis() - savedAt > AI_CACHE_TTL_MS) return null
            val title = json.optString("bundleTitle", "")
            val reasoning = json.optString("reasoning", "")
            val idsArr = json.optJSONArray("productIds") ?: return null
            val ids = buildList {
                for (i in 0 until idsArr.length()) add(idsArr.getString(i))
            }
            val products = resolveCatalogIds(ids, catalog)
            if (title.isBlank() || products.isEmpty()) null
            else AiRecommendationBundle(title, reasoning, products)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveDiskCache(cacheKey: String, bundle: AiRecommendationBundle) {
        val prefs = appContext?.getSharedPreferences(AI_CACHE_PREFS, Context.MODE_PRIVATE) ?: return
        try {
            val json = JSONObject().apply {
                put("savedAt", System.currentTimeMillis())
                put("bundleTitle", bundle.bundleTitle)
                put("reasoning", bundle.reasoning)
                put("productIds", JSONArray(bundle.products.map { it.id }))
            }
            prefs.edit().putString("rec_$cacheKey", json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private val recommendationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recommendationSchema
                temperature = 0.45f
            }
        )
    }

    private val recommendationFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recommendationSchema
                temperature = 0.45f
            }
        )
    }

    private val visualSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visualSearchSchema
                temperature = 0.35f
            }
        )
    }

    private val visualSearchFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visualSearchSchema
                temperature = 0.35f
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.55f
            }
        )
    }

    private val chatFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.55f
            }
        )
    }

    private fun isQuotaOrRateLimitError(e: Throwable): Boolean {
        val msg = generateSequence(e) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return msg.contains("quota") ||
            msg.contains("rate limit") ||
            msg.contains("rate-limit") ||
            msg.contains("resource_exhausted") ||
            msg.contains("free_tier") ||
            msg.contains("429")
    }

    private fun shouldTryFallbackModel(e: Throwable): Boolean {
        if (isQuotaOrRateLimitError(e)) return true
        val msg = generateSequence(e) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return msg.contains("503") ||
            msg.contains("unavailable") ||
            msg.contains("overloaded") ||
            msg.contains("internal") ||
            msg.contains("not found") ||
            msg.contains("unsupported")
    }

    fun friendlyErrorMessage(e: Throwable, language: AppLanguage): String {
        if (e is TimeoutCancellationException) return timeoutMessage(language)
        val raw = generateSequence(e) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return when {
            isQuotaOrRateLimitError(e) -> when (language) {
                AppLanguage.UZ -> "Kunlik bepul AI so'rovlar limiti vaqtincha tugadi. Birozdan so'ng qayta urinib ko'ring yoki quyidagi katalogdan qidiring."
                AppLanguage.RU -> "Дневной лимит бесплатных AI-запросов временно исчерпан. Повторите позже или воспользуйтесь каталогом ниже."
                AppLanguage.EN -> "Daily free AI request limit temporarily reached. Please try again later or browse the catalog below."
            }
            raw.contains("network") || raw.contains("unable to resolve host") || raw.contains("failed to connect") || raw.contains("socket") -> when (language) {
                AppLanguage.UZ -> "Internet aloqasi uzildi. Tarmoqni tekshirib qayta urinib ko'ring."
                AppLanguage.RU -> "Нет подключения к интернету. Проверьте сеть и попробуйте снова."
                AppLanguage.EN -> "Network error. Please check your connection and try again."
            }
            raw.contains("app check") || raw.contains("403") || raw.contains("permission") -> when (language) {
                AppLanguage.UZ -> "AI xizmatiga ruxsat tekshiruvi amalga oshmadi. Qayta urinib ko'ring."
                AppLanguage.RU -> "Ошибка проверки доступа к AI-сервису. Попробуйте ещё раз."
                AppLanguage.EN -> "AI access verification failed. Please try again."
            }
            else -> when (language) {
                AppLanguage.UZ -> "AI xizmatida vaqtinchalik xatolik yuz berdi. Qayta urinib ko'ring."
                AppLanguage.RU -> "Временная ошибка AI-сервиса. Попробуйте снова."
                AppLanguage.EN -> "Temporary AI service error. Please try again."
            }
        }
    }

    private fun buildCatalogPromptIndex(catalog: List<Product>): String {
        return catalog.joinToString("\n") { p ->
            "- id=${p.id} | cat=${p.categoryId} | uz=${p.titleUz} | ru=${p.titleRu} | en=${p.titleEn} | price=${p.priceUzs} UZS | tags=${p.tags.joinToString(",")}"
        }
    }

    private fun resolveCatalogIds(rawIds: List<String>, catalog: List<Product>): List<Product> {
        val exactMap = catalog.associateBy { it.id }
        val lowerMap = catalog.associateBy { it.id.lowercase().trim() }
        return rawIds.mapNotNull { raw ->
            val cleaned = raw.trim()
            exactMap[cleaned] ?: lowerMap[cleaned.lowercase()]
        }.distinctBy { it.id }
    }

    private fun languageInstruction(language: AppLanguage): String = when (language) {
        AppLanguage.UZ -> "Write all human-readable text strictly in natural Uzbek (Latin script)."
        AppLanguage.RU -> "Write all human-readable text strictly in natural Russian."
        AppLanguage.EN -> "Write all human-readable text strictly in natural English."
    }

    suspend fun getSmartRecommendations(
        catalog: List<Product>,
        cartItems: List<CartEntry>,
        tasteProfile: AiTasteProfile?,
        favoriteIds: Set<String>,
        district: String,
        language: AppLanguage,
        forceRefresh: Boolean = false
    ): Result<AiRecommendationBundle> {
        val cacheKey = "${language.name}_$district"
        if (!forceRefresh) {
            recommendationCache[cacheKey]?.let { return Result.success(it) }
            readDiskCache(cacheKey, catalog)?.let { diskBundle ->
                recommendationCache[cacheKey] = diskBundle
                return Result.success(diskBundle)
            }
        }
        return try {
            val region = UzbekistanGeoCatalog.findRegionForCity(district)
            val regionName = region.localizedName(language)
            val cartDesc = if (cartItems.isEmpty()) "empty" else cartItems.joinToString { "${it.productId}(x${it.quantity})" }
            val topCats = tasteProfile?.topCategories?.joinToString().orEmpty().ifBlank { "none yet" }
            val recentQ = tasteProfile?.recentQueries?.joinToString().orEmpty().ifBlank { "none yet" }
            val favDesc = if (favoriteIds.isEmpty()) "none yet" else favoriteIds.take(6).joinToString()

            val prompt = """
                You are TezBazar's intelligent nationwide Uzbekistan marketplace recommender (similar to Uzum Market + express grocery).
                ${languageInstruction(language)}
                User context across Uzbekistan:
                - Region: $regionName, City/District: $district
                - Current cart: $cartDesc
                - Favorited product IDs: $favDesc
                - Favorite categories: $topCats
                - Recent searches: $recentQ

                Select 3 to 5 complementary products from the catalog below that create a compelling, practical bundle tailored to the user's region, season, cart, and favorites (for example: regional plov set, smart home & tech bundle, morning breakfast set, or tea & sweets).
                Return ONLY valid product IDs from this catalog:
                ${buildCatalogPromptIndex(catalog)}
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                try {
                    recommendationModel.generateContent(prompt)
                } catch (primaryErr: Exception) {
                    if (primaryErr is CancellationException) throw primaryErr
                    if (shouldTryFallbackModel(primaryErr)) {
                        Log.w(TAG, "Primary recommendation model quota hit, trying fallback model", primaryErr)
                        delay(500L)
                        recommendationFallbackModel.generateContent(prompt)
                    } else {
                        throw primaryErr
                    }
                }
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(rawJson)
            val title = json.optString("bundleTitle", "TezBazar AI")
            val reasoning = json.optString("reasoning", "")
            val idArray = json.optJSONArray("productIds")
            val selectedIds = buildList {
                if (idArray != null) {
                    for (i in 0 until idArray.length()) {
                        add(idArray.getString(i))
                    }
                }
            }
            val matched = resolveCatalogIds(selectedIds, catalog)
            if (matched.isEmpty()) {
                throw IllegalStateException("AI returned no matching catalog IDs.")
            }
            val bundle = AiRecommendationBundle(title, reasoning, matched)
            recommendationCache[cacheKey] = bundle
            saveDiskCache(cacheKey, bundle)
            Result.success(bundle)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI recommendation timed out", e)
            Result.failure(Exception(timeoutMessage(language)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI recommendation failed", e)
            Result.failure(Exception(friendlyErrorMessage(e, language), e))
        }
    }

    suspend fun searchByImage(
        bitmap: Bitmap,
        catalog: List<Product>,
        language: AppLanguage
    ): Result<AiSearchResult> {
        return try {
            val scaled = scaleBitmapUnderLimit(bitmap, maxDim = 1024)
            val prompt = """
                Analyze this image for an Uzbekistan nationwide marketplace shopper (groceries, dishes, electronics, home & beauty).
                ${languageInstruction(language)}
                Identify the food, dish, ingredient, gadget, or product shown in the image, explain briefly how to shop or prepare it, and pick 2 to 5 best matching product IDs strictly from this catalog:
                ${buildCatalogPromptIndex(catalog)}
            """.trimIndent()

            val inputContent = content {
                image(scaled)
                text(prompt)
            }
            val response = withTimeout(AI_TIMEOUT_MS) {
                try {
                    visualSearchModel.generateContent(inputContent)
                } catch (primaryErr: Exception) {
                    if (primaryErr is CancellationException) throw primaryErr
                    if (shouldTryFallbackModel(primaryErr)) {
                        Log.w(TAG, "Primary visual model quota hit, trying fallback model", primaryErr)
                        visualSearchFallbackModel.generateContent(inputContent)
                    } else {
                        throw primaryErr
                    }
                }
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI visual search response.")
            val json = JSONObject(rawJson)
            val concept = json.optString("detectedConcept", "")
            val explanation = json.optString("explanation", "")
            val idArray = json.optJSONArray("productIds")
            val rawIds = buildList {
                if (idArray != null) {
                    for (i in 0 until idArray.length()) {
                        add(idArray.getString(i))
                    }
                }
            }
            val matched = resolveCatalogIds(rawIds, catalog)
            Result.success(AiSearchResult(concept, explanation, matched))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Visual search timed out", e)
            Result.failure(Exception(timeoutMessage(language)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Visual search failed", e)
            Result.failure(Exception(friendlyErrorMessage(e, language), e))
        }
    }

    suspend fun interpretNaturalIntent(
        userQuery: String,
        catalog: List<Product>,
        language: AppLanguage
    ): Result<AiSearchResult> {
        return try {
            val prompt = """
                Interpret this user voice/text shopping request in Uzbekistan: "$userQuery"
                ${languageInstruction(language)}
                Identify what the user wants to cook, gift, buy, or assemble, provide a concise helpful explanation, and choose 2 to 5 matching product IDs strictly from this catalog:
                ${buildCatalogPromptIndex(catalog)}
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                try {
                    visualSearchModel.generateContent(prompt)
                } catch (primaryErr: Exception) {
                    if (primaryErr is CancellationException) throw primaryErr
                    if (shouldTryFallbackModel(primaryErr)) {
                        Log.w(TAG, "Primary intent model quota hit, trying fallback model", primaryErr)
                        visualSearchFallbackModel.generateContent(prompt)
                    } else {
                        throw primaryErr
                    }
                }
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI intent response.")
            val json = JSONObject(rawJson)
            val concept = json.optString("detectedConcept", userQuery)
            val explanation = json.optString("explanation", "")
            val idArray = json.optJSONArray("productIds")
            val rawIds = buildList {
                if (idArray != null) {
                    for (i in 0 until idArray.length()) {
                        add(idArray.getString(i))
                    }
                }
            }
            val matched = resolveCatalogIds(rawIds, catalog)
            Result.success(AiSearchResult(concept, explanation, matched))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Intent parsing timed out", e)
            Result.failure(Exception(timeoutMessage(language)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Intent parsing failed", e)
            Result.failure(Exception(friendlyErrorMessage(e, language), e))
        }
    }

    suspend fun searchByAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        catalog: List<Product>,
        language: AppLanguage
    ): Result<AiSearchResult> {
        return try {
            val prompt = """
                Listen carefully to this shopper's spoken request (which may be in Uzbek, Russian, or English).
                ${languageInstruction(language)}
                Set detectedConcept to a concise summary of what the shopper asked for (e.g., ingredients to cook, gift, or products to buy).
                Set explanation to a brief helpful response in the requested language, and select 2 to 5 matching product IDs strictly from this catalog:
                ${buildCatalogPromptIndex(catalog)}
            """.trimIndent()

            val audioContent = content {
                inlineData(audioBytes, mimeType)
                text(prompt)
            }
            val response = withTimeout(AI_TIMEOUT_MS) {
                try {
                    visualSearchModel.generateContent(audioContent)
                } catch (primaryErr: Exception) {
                    if (primaryErr is CancellationException) throw primaryErr
                    if (shouldTryFallbackModel(primaryErr)) {
                        Log.w(TAG, "Primary audio model quota hit, trying fallback model", primaryErr)
                        visualSearchFallbackModel.generateContent(audioContent)
                    } else {
                        throw primaryErr
                    }
                }
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI audio search response.")
            val json = JSONObject(rawJson)
            val concept = json.optString("detectedConcept", "")
            val explanation = json.optString("explanation", "")
            val idArray = json.optJSONArray("productIds")
            val rawIds = buildList {
                if (idArray != null) {
                    for (i in 0 until idArray.length()) {
                        add(idArray.getString(i))
                    }
                }
            }
            val matched = resolveCatalogIds(rawIds, catalog)
            Result.success(AiSearchResult(concept, explanation, matched))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio search timed out", e)
            Result.failure(Exception(timeoutMessage(language)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio search failed", e)
            Result.failure(Exception(friendlyErrorMessage(e, language), e))
        }
    }

    fun streamShoppingAssistant(
        userMessage: String,
        priorHistory: List<AiChatMessage>,
        catalog: List<Product>,
        district: String,
        language: AppLanguage
    ): Flow<String> = flow {
        val region = UzbekistanGeoCatalog.findRegionForCity(district)
        val regionName = region.localizedName(language)
        val pvzList = UzbekistanGeoCatalog.pickupPointsFor(district).joinToString(", ")

        val systemContext = """
            You are TezBazar AI — an expert, friendly personal shopping & culinary assistant operating across ALL 14 regions of Uzbekistan (just like Uzum Market).
            ${languageInstruction(language)}
            Current shopper location: $regionName, $district (Pickup points available: $pvzList; Courier & Free PVZ Pickup available).
            Supported payments: Uzcard (8600), Humo (9860), and Cash on Delivery; promo codes: TEZ2026 (15% off), SALOM10 (-10,000 UZS), UZUMFREE (free delivery).
            Keep answers practical, well-structured, concise, and tailored to Uzbekistan shopping. When recommending items, mention exact product names from our catalog and their UZS prices:
            ${catalog.joinToString("; ") { "${it.localizedTitle(language)} (${it.priceUzs} UZS)" }}
        """.trimIndent()

        val historyContents = buildList {
            add(content("user") { text("System context:\n$systemContext") })
            add(content("model") { text("Tushunarli. TezBazar AI yordamchisi sifatida tayyorman.") })
            priorHistory.takeLast(8).forEach { msg ->
                if (msg.text.isNotBlank()) {
                    add(content(if (msg.isUser) "user" else "model") { text(msg.text) })
                }
            }
        }

        val acc = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            try {
                val chat = chatModel.startChat(history = historyContents)
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val text = chunk.text.orEmpty()
                    if (text.isNotEmpty()) {
                        acc.append(text)
                        emit(acc.toString())
                    }
                }
            } catch (primaryErr: Exception) {
                if (primaryErr is CancellationException) throw primaryErr
                if (acc.isEmpty() && shouldTryFallbackModel(primaryErr)) {
                    Log.w(TAG, "Primary chat model quota hit, trying fallback model", primaryErr)
                    val fallbackChat = chatFallbackModel.startChat(history = historyContents)
                    fallbackChat.sendMessageStream(userMessage).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            acc.append(text)
                            emit(acc.toString())
                        }
                    }
                } else {
                    throw primaryErr
                }
            }
        }
        if (acc.isBlank()) {
            throw IllegalStateException("Model returned an empty response.")
        }
    }

    fun matchCatalogProductsInResponse(
        responseText: String,
        catalog: List<Product>,
        language: AppLanguage
    ): List<Product> {
        val lower = responseText.lowercase()
        val exactMatches = catalog.filter { p ->
            val tUz = p.titleUz.lowercase()
            val tRu = p.titleRu.lowercase()
            val tEn = p.titleEn.lowercase()
            lower.contains(tUz) || lower.contains(tRu) || lower.contains(tEn) ||
                p.tags.count { tag -> tag.length >= 4 && lower.contains(tag.lowercase()) } >= 2
        }
        return exactMatches.take(4)
    }

    private fun scaleBitmapUnderLimit(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    private fun timeoutMessage(language: AppLanguage): String = when (language) {
        AppLanguage.UZ -> "AI javobi 60 soniyadan oshdi. Qayta urinib ko'ring."
        AppLanguage.RU -> "Превышено время ожидания AI (60 сек). Повторите попытку."
        AppLanguage.EN -> "AI request timed out after 60 seconds. Please try again."
    }
}
