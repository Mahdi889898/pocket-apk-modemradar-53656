package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ScrapedListing(
    val externalId: String,
    val title: String,
    val priceText: String,
    val imageUrl: String?,
    val listingUrl: String,
    val badgeText: String? = null,
    val sellerInfo: String? = null
)

sealed interface ScrapeResult {
    data class Success(val listings: List<ScrapedListing>) : ScrapeResult
    data class EmptyOrProtected(val httpCode: Int, val reason: String) : ScrapeResult
    data class Failure(val reason: String) : ScrapeResult
}

class EsamListingScraper {

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (cookies.isNotEmpty()) {
                    cookieStore[url.host] = cookies
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host].orEmpty()
            }
        })
        .build()

    suspend fun fetchListings(targetUrl: String): ScrapeResult = withContext(Dispatchers.IO) {
        if (!targetUrl.startsWith("https://") && !targetUrl.startsWith("http://")) {
            return@withContext ScrapeResult.Failure("آدرس متصل‌شده معتبر نیست.")
        }

        val request = Request.Builder()
            .url(targetUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            )
            .header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            )
            .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Referer", "https://esam.ir/")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()

                if (code == 403 || code == 429 || code == 503) {
                    return@withContext ScrapeResult.EmptyOrProtected(
                        httpCode = code,
                        reason = "سایت ایسام موقتاً درخواست خودکار را محدود کرد (کد $code). می‌توانید صفحه را مستقیم در مرورگر ببینید یا کمی بعد دوباره تلاش کنید."
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext ScrapeResult.Failure("پاسخ ناموفق از سرور سایت (کد HTTP $code)")
                }

                if (body.isBlank()) {
                    return@withContext ScrapeResult.EmptyOrProtected(
                        httpCode = code,
                        reason = "پاسخ خالی از سرور دریافت شد."
                    )
                }

                val parsed = parseHtml(body, targetUrl)
                if (parsed.isNotEmpty()) {
                    ScrapeResult.Success(parsed)
                } else {
                    val lower = body.lowercase()
                    val isChallenge = lower.contains("cf-browser-verification") ||
                        lower.contains("just a moment") ||
                        lower.contains("arvancloud") ||
                        lower.contains("captcha") ||
                        lower.contains("enable javascript")
                    val reason = if (isChallenge) {
                        "سایت در حال حاضر صفحه محافظ امنیتی (ضدربات/جاوااسکریپت) نمایش می‌دهد و اجازه خواندن مستقیم لیست را نداد."
                    } else {
                        "ساختار صفحه دریافت شد اما آگهی فعالی در خروجی HTML شناسایی نشد."
                    }
                    ScrapeResult.EmptyOrProtected(httpCode = code, reason = reason)
                }
            }
        } catch (_: UnknownHostException) {
            ScrapeResult.Failure("دسترسی به سرور esam.ir برقرار نشد. اتصال اینترنت را بررسی کنید.")
        } catch (_: SocketTimeoutException) {
            ScrapeResult.Failure("زمان انتظار برای پاسخ سایت به پایان رسید (Timeout).")
        } catch (e: IOException) {
            ScrapeResult.Failure("خطای شبکه در ارتباط با سایت: ${e.localizedMessage ?: "نامشخص"}")
        } catch (e: Exception) {
            ScrapeResult.Failure("خطا در پردازش صفحه: ${e.localizedMessage ?: "نامشخص"}")
        }
    }

    internal fun parseHtml(html: String, baseUrl: String): List<ScrapedListing> {
        val doc = Jsoup.parse(html, baseUrl)

        // Strategy 1: Semantic Esam item cards & anchors (/item/{id}/...)
        val fromAnchors = extractFromItemAnchors(doc)
        if (fromAnchors.isNotEmpty()) return fromAnchors

        // Strategy 2: Schema.org JSON-LD (ItemList / Product)
        val fromJsonLd = extractFromJsonLd(doc, baseUrl)
        if (fromJsonLd.isNotEmpty()) return fromJsonLd

        // Strategy 3: Inline hydration JSON scripts (__NEXT_DATA__ or script payloads)
        val fromScripts = extractFromInlineScripts(doc, baseUrl)
        if (fromScripts.isNotEmpty()) return fromScripts

        return emptyList()
    }

    private fun extractFromItemAnchors(doc: Document): List<ScrapedListing> {
        val results = LinkedHashMap<String, ScrapedListing>()
        val anchors = doc.select("a[href*=/item/], a[href*=/Item/]")

        for (anchor in anchors) {
            val absHref = anchor.absUrl("href").ifBlank { anchor.attr("href") }
            val idMatch = ITEM_ID_REGEX.find(absHref) ?: continue
            val externalId = idMatch.groupValues[1]

            val container = findCardContainer(anchor)
            val title = extractTitle(anchor, container)
            if (title.isBlank() || title.length < 3) continue

            val priceText = extractPrice(container)
            val imageUrl = extractImageUrl(container) ?: extractImageUrl(anchor)
            val badgeText = extractBadge(container)

            val existing = results[externalId]
            if (existing == null) {
                results[externalId] = ScrapedListing(
                    externalId = externalId,
                    title = title,
                    priceText = priceText,
                    imageUrl = imageUrl,
                    listingUrl = normalizeEsamUrl(absHref, externalId),
                    badgeText = badgeText
                )
            } else {
                // Merge richer fields if a second anchor inside the same card had better data
                val betterTitle = if (title.length > existing.title.length) title else existing.title
                val betterPrice = if (existing.priceText.isBlank() && priceText.isNotBlank()) priceText else existing.priceText
                val betterImage = existing.imageUrl ?: imageUrl
                val betterBadge = existing.badgeText ?: badgeText
                results[externalId] = existing.copy(
                    title = betterTitle,
                    priceText = betterPrice,
                    imageUrl = betterImage,
                    badgeText = betterBadge
                )
            }
        }
        return results.values.toList()
    }

    private fun findCardContainer(anchor: Element): Element {
        var current: Element? = anchor
        var best: Element = anchor
        repeat(5) {
            val parent = current?.parent() ?: return best
            val tag = parent.tagName().lowercase()
            if (tag == "body" || tag == "main" || tag == "section") return best

            val itemLinksCount = parent.select("a[href*=/item/], a[href*=/Item/]").mapNotNull {
                ITEM_ID_REGEX.find(it.attr("href"))?.groupValues?.getOrNull(1)
            }.toSet().size

            // Stop climbing if the parent holds multiple distinct listings (it's the whole grid)
            if (itemLinksCount > 1) return best

            best = parent
            val cls = parent.className().lowercase()
            if (tag == "article" || tag == "li" || cls.contains("card") ||
                cls.contains("item") || cls.contains("product")
            ) {
                return parent
            }
            current = parent
        }
        return best
    }

    private fun extractTitle(anchor: Element, container: Element): String {
        val candidates = listOfNotNull(
            container.selectFirst("h1, h2, h3, h4, [class*=title], [class*=Title], [class*=name], [class*=Name]")
                ?.text()?.trim(),
            anchor.attr("title").trim(),
            container.selectFirst("img[alt]")?.attr("alt")?.trim(),
            anchor.text().trim()
        )
        return candidates
            .map { cleanWhitespace(it) }
            .firstOrNull { candidate ->
                candidate.length >= 4 &&
                    !candidate.matches(PURE_PRICE_REGEX) &&
                    !candidate.equals("مشاهده", ignoreCase = true)
            }
            .orEmpty()
    }

    private fun extractPrice(container: Element): String {
        val priceEl = container.selectFirst(
            "[class*=price], [class*=Price], [class*=amount], [class*=toman]"
        )
        val priceElText = priceEl?.text()?.let(::cleanWhitespace).orEmpty()
        if (priceElText.isNotBlank() && DIGIT_REGEX.containsMatchIn(priceElText)) {
            return formatPriceSnippet(priceElText)
        }

        val fullText = cleanWhitespace(container.text())
        val match = PRICE_TEXT_REGEX.find(fullText)
        return match?.value?.trim().orEmpty()
    }

    private fun extractImageUrl(element: Element): String? {
        val img = element.selectFirst("img") ?: return null
        val candidates = listOf(
            img.absUrl("data-src"),
            img.absUrl("data-original"),
            img.absUrl("data-lazy-src"),
            img.absUrl("src"),
            img.attr("data-src"),
            img.attr("src")
        )
        return candidates
            .map { it.trim() }
            .firstOrNull { url ->
                url.isNotBlank() &&
                    !url.startsWith("data:image") &&
                    !url.contains("placeholder", ignoreCase = true) &&
                    !url.contains("loader", ignoreCase = true)
            }
            ?.let { url ->
                when {
                    url.startsWith("//") -> "https:$url"
                    url.startsWith("/") -> "https://esam.ir$url"
                    else -> url
                }
            }
    }

    private fun extractBadge(container: Element): String? {
        val text = container.text()
        return when {
            text.contains("مزایده") -> "مزایده"
            text.contains("ارسال رایگان") -> "ارسال رایگان"
            text.contains("فروش فوری") -> "فروش فوری"
            text.contains("کارکرده") -> "کارکرده"
            text.contains("نو") && text.contains("آکبند") -> "نو / آکبند"
            else -> null
        }
    }

    private fun extractFromJsonLd(doc: Document, baseUrl: String): List<ScrapedListing> {
        val results = LinkedHashMap<String, ScrapedListing>()
        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val raw = script.data().trim()
            if (raw.isBlank()) continue
            try {
                if (raw.startsWith("[")) {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { parseJsonLdObject(it, results, baseUrl) }
                    }
                } else {
                    parseJsonLdObject(JSONObject(raw), results, baseUrl)
                }
            } catch (_: Exception) {
                // Ignore malformed JSON-LD blocks
            }
        }
        return results.values.toList()
    }

    private fun parseJsonLdObject(
        obj: JSONObject,
        out: MutableMap<String, ScrapedListing>,
        baseUrl: String
    ) {
        val type = obj.optString("@type")
        when {
            type.equals("ItemList", ignoreCase = true) -> {
                val elements = obj.optJSONArray("itemListElement") ?: return
                for (i in 0 until elements.length()) {
                    val entry = elements.optJSONObject(i) ?: continue
                    val item = entry.optJSONObject("item") ?: entry
                    extractProductJson(item, out, baseUrl)
                }
            }
            type.equals("Product", ignoreCase = true) -> {
                extractProductJson(obj, out, baseUrl)
            }
            obj.has("@graph") -> {
                val graph = obj.optJSONArray("@graph") ?: return
                for (i in 0 until graph.length()) {
                    graph.optJSONObject(i)?.let { parseJsonLdObject(it, out, baseUrl) }
                }
            }
        }
    }

    private fun extractProductJson(
        obj: JSONObject,
        out: MutableMap<String, ScrapedListing>,
        baseUrl: String
    ) {
        val name = cleanWhitespace(obj.optString("name"))
        val url = obj.optString("url").ifBlank { baseUrl }
        if (name.isBlank()) return

        val externalId = ITEM_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
            ?: obj.optString("sku").ifBlank { name.hashCode().toString() }

        val offers = obj.optJSONObject("offers")
        val rawPrice = offers?.optString("price").orEmpty()
        val priceText = if (rawPrice.isNotBlank()) "$rawPrice تومان" else ""

        val imageUrl = when (val img = obj.opt("image")) {
            is String -> img
            is JSONArray -> img.optString(0)
            else -> null
        }?.takeIf { it.isNotBlank() }

        out[externalId] = ScrapedListing(
            externalId = externalId,
            title = name,
            priceText = priceText,
            imageUrl = imageUrl,
            listingUrl = normalizeEsamUrl(url, externalId)
        )
    }

    private fun extractFromInlineScripts(doc: Document, baseUrl: String): List<ScrapedListing> {
        val results = LinkedHashMap<String, ScrapedListing>()
        val nextData = doc.selectFirst("script#__NEXT_DATA__")?.data()
        val candidatePayloads = buildList {
            if (!nextData.isNullOrBlank()) add(nextData)
            doc.select("script:not([src])").forEach { script ->
                val content = script.data()
                if (content.contains("itemId", ignoreCase = true) ||
                    content.contains("/item/", ignoreCase = true)
                ) {
                    add(content)
                }
            }
        }

        for (payload in candidatePayloads) {
            try {
                if (payload.trim().startsWith("{")) {
                    walkJsonForItems(JSONObject(payload), results, baseUrl)
                }
            } catch (_: Exception) {
                // Fallback regex for inline JS objects with itemId + title
                INLINE_ITEM_REGEX.findAll(payload).forEach { match ->
                    val id = match.groupValues[1]
                    val title = cleanWhitespace(match.groupValues[2])
                    if (id.isNotBlank() && title.length >= 4 && !results.containsKey(id)) {
                        results[id] = ScrapedListing(
                            externalId = id,
                            title = title,
                            priceText = "",
                            imageUrl = null,
                            listingUrl = "https://esam.ir/item/$id"
                        )
                    }
                }
            }
            if (results.isNotEmpty()) break
        }
        return results.values.toList()
    }

    private fun walkJsonForItems(
        node: Any?,
        out: MutableMap<String, ScrapedListing>,
        baseUrl: String
    ) {
        when (node) {
            is JSONObject -> {
                val id = node.optString("itemId").ifBlank { node.optString("ItemId") }
                    .ifBlank { node.optString("id") }
                val title = node.optString("title").ifBlank { node.optString("Title") }
                    .ifBlank { node.optString("name") }
                if (id.matches(Regex("^\\d{5,12}$")) && title.length >= 4) {
                    val priceRaw = node.optString("price").ifBlank { node.optString("Price") }
                        .ifBlank { node.optString("currentPrice") }
                    val img = node.optString("image").ifBlank { node.optString("imageUrl") }
                        .ifBlank { node.optString("img") }.takeIf { it.isNotBlank() }
                    val url = node.optString("url").ifBlank { "https://esam.ir/item/$id" }
                    out.putIfAbsent(
                        id,
                        ScrapedListing(
                            externalId = id,
                            title = cleanWhitespace(title),
                            priceText = if (priceRaw.isNotBlank()) "$priceRaw تومان" else "",
                            imageUrl = img,
                            listingUrl = normalizeEsamUrl(url, id)
                        )
                    )
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    walkJsonForItems(node.opt(keys.next()), out, baseUrl)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    walkJsonForItems(node.opt(i), out, baseUrl)
                }
            }
        }
    }

    private fun normalizeEsamUrl(rawUrl: String, externalId: String): String {
        val trimmed = rawUrl.trim()
        return when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.startsWith("/") -> "https://esam.ir$trimmed"
            else -> "https://esam.ir/item/$externalId"
        }
    }

    private fun formatPriceSnippet(text: String): String {
        val match = PRICE_TEXT_REGEX.find(text)
        if (match != null) return match.value.trim()
        return if (text.contains("تومان") || text.contains("ریال")) text else "$text تومان"
    }

    private fun cleanWhitespace(input: String): String {
        return input.replace(Regex("\\s+"), " ").trim()
    }

    companion object {
        private val ITEM_ID_REGEX = Regex("(?i)/item/(\\d+)")
        private val DIGIT_REGEX = Regex("[0-9۰-۹]")
        private val PURE_PRICE_REGEX = Regex("^[0-9۰-۹,،.\\s]+(تومان|ریال)?$")
        private val PRICE_TEXT_REGEX = Regex("[0-9۰-۹][0-9۰-۹,،.]{2,}\\s*(تومان|ریال)")
        private val INLINE_ITEM_REGEX =
            Regex(""""(?:itemId|ItemId)"\s*:\s*"?(\d{5,12})"?[^\}]{0,200}"(?:title|Title)"\s*:\s*"([^"]{4,140})"""")
    }
}
