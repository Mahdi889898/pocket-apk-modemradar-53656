package com.example

import com.example.data.network.EsamListingScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EsamListingScraperTest {

    private val scraper = EsamListingScraper()
    private val baseUrl = "https://esam.ir/search?kw=%D9%85%D9%88%D8%AF%D9%85&so=1"

    @Test
    fun parseHtml_extractsEsamItemCardsFromAnchors() {
        val sampleHtml = """
            <html>
              <body>
                <div class="search-results-grid">
                  <article class="item-card">
                    <a href="/item/29481022/مودم-تی-پی-لینک-8961">
                      <img data-src="https://static.esam.ir/sell/itemImages/29481022.jpg" alt="مودم تی پی لینک TD-W8961N" />
                      <h2 class="item-title">مودم تی پی لینک TD-W8961N</h2>
                    </a>
                    <div class="item-price">۸۵۰,۰۰۰ تومان</div>
                    <span class="badge">ارسال رایگان</span>
                  </article>
                  <article class="item-card">
                    <a href="/item/29481055/مودم-4g-ایرانسل">
                      <img src="/images/modem2.jpg" />
                      <h3>مودم 4G ایرانسل TF-i60 آنلاک</h3>
                    </a>
                    <div class="price-box">۳,۲۰۰,۰۰۰ تومان</div>
                    <span>مزایده</span>
                  </article>
                </div>
              </body>
            </html>
        """.trimIndent()

        val listings = scraper.parseHtml(sampleHtml, baseUrl)

        assertEquals(2, listings.size)
        assertEquals("29481022", listings[0].externalId)
        assertEquals("مودم تی پی لینک TD-W8961N", listings[0].title)
        assertEquals("۸۵۰,۰۰۰ تومان", listings[0].priceText)
        assertEquals("https://static.esam.ir/sell/itemImages/29481022.jpg", listings[0].imageUrl)
        assertEquals("ارسال رایگان", listings[0].badgeText)
        assertTrue(listings[0].listingUrl.startsWith("https://esam.ir/item/29481022"))

        assertEquals("29481055", listings[1].externalId)
        assertEquals("مزایده", listings[1].badgeText)
        assertEquals("https://esam.ir/images/modem2.jpg", listings[1].imageUrl)
    }

    @Test
    fun parseHtml_extractsJsonLdFallback() {
        val jsonLdHtml = """
            <html>
              <head>
                <script type="application/ld+json">
                  {
                    "@context": "https://schema.org",
                    "@type": "ItemList",
                    "itemListElement": [
                      {
                        "@type": "ListItem",
                        "position": 1,
                        "item": {
                          "@type": "Product",
                          "name": "مودم فیبر نوری هوآوی HG8245H",
                          "url": "https://esam.ir/item/30112233/huawei-modem",
                          "image": "https://static.esam.ir/img/30112233.jpg",
                          "offers": {
                            "@type": "Offer",
                            "price": "1450000"
                          }
                        }
                      }
                    ]
                  }
                </script>
              </head>
              <body></body>
            </html>
        """.trimIndent()

        val listings = scraper.parseHtml(jsonLdHtml, baseUrl)

        assertEquals(1, listings.size)
        assertEquals("30112233", listings[0].externalId)
        assertEquals("مودم فیبر نوری هوآوی HG8245H", listings[0].title)
        assertEquals("1450000 تومان", listings[0].priceText)
    }
}
