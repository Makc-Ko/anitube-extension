package eu.kanade.tachiyomi.animeextension.ua.anitubeinua

import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.model.*
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AniTubeInUa : AnimeHttpSource() {

    override val name = "AniTube.in.ua"
    override val baseUrl = "https://anitube.in.ua"
    override val lang = "uk"
    override val supportsLatest = true

    // ============================= Popular =============================

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/")
    }

    override fun popularAnimeParse(response: okhttp3.Response): AnimesPage {
        val doc = response.use { Jsoup.parse(it.body!!.string()) }
        val items = doc.select("div.shortstory") // можливо потрібно змінити, залежно від структури
        val animeList = items.map { it.toAnime() }
        val hasNext = doc.select("a.next").isNotEmpty()
        return AnimesPage(animeList, hasNext)
    }

    // ============================== Latest ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        // Якщо сайт має окремий розділ «новинки», треба підправити URL
        return GET("$baseUrl/page/$page/")
    }

    override fun latestUpdatesParse(response: okhttp3.Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // =============================== Search =============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        // Припущення про параметри пошуку, може бути інша форма
        return GET("$baseUrl/?s=$query&page=$page")
    }

    override fun searchAnimeParse(response: okhttp3.Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // ============================ Anime details ===========================

    override fun animeDetailsRequest(anime: SAnime): Request {
        return GET(baseUrl + anime.url)
    }

    override fun animeDetailsParse(response: okhttp3.Response): SAnime {
        val doc = response.use { Jsoup.parse(it.body!!.string()) }
        return SAnime.create().apply {
            title = doc.selectFirst("h1.title")?.text() ?: ""
            thumbnail_url = doc.selectFirst("div.fullimg img")?.attr("src")
            description = doc.selectFirst("div.full-text")?.text()
            genre = doc.select("a.tag").joinToString(", ") { it.text() }
            status = SAnime.UNKNOWN
        }
    }

    // =============================== Episodes =============================

    override fun episodeListRequest(anime: SAnime): Request {
        return GET(baseUrl + anime.url)
    }

    override fun episodeListParse(response: okhttp3.Response): List<SEpisode> {
        val doc = response.use { Jsoup.parse(it.body!!.string()) }
        val eps = doc.select("div.series-block a.episode") // перевірити селектор
        return eps.mapIndexed { i, el ->
            SEpisode.create().apply {
                name = el.text()
                episode_number = (i + 1).toFloat()
                url = el.attr("href")
            }
        }.reversed()
    }

    // =============================== Videos ===============================

    override fun videoListRequest(episode: SEpisode): Request {
        return GET(baseUrl + episode.url)
    }

    override fun videoListParse(response: okhttp3.Response): List<Video> {
        val doc = response.use { Jsoup.parse(it.body!!.string()) }
        val iframe = doc.selectFirst("iframe")?.attr("src")
        return if (iframe != null) {
            listOf(Video(iframe, "АніТюб", iframe))
        } else {
            emptyList()
        }
    }

    // =============================== Helpers ===============================

    private fun Element.toAnime(): SAnime {
        val a = selectFirst("a")!!
        return SAnime.create().apply {
            title = a.attr("title").ifEmpty { a.text() }
            thumbnail_url = selectFirst("img")?.attr("src")
            url = a.attr("href").removePrefix(baseUrl)
        }
    }
}
