package eu.makcko.anitube

import eu.kanade.tachiyomi.animeextension.lib.extractors.GenericVideoExtractor
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.*
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Request
import org.jsoup.nodes.Element

class Anitube : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "AnitubeUA"
    override val baseUrl = "https://anitube.ua"
    override val lang = "uk"
    override val supportsLatest = true

    // ===============================
    // Головна сторінка / Список аніме
    // ===============================

    override fun popularAnimeRequest(page: Int): Request =
        GET("$baseUrl/anime/page/$page")

    override fun popularAnimeParse(response: okhttp3.Response): AnimesPage {
        val document = response.asJsoup()

        val animeList = document.select("div.shortstory").map { element ->
            SAnime.create().apply {
                title = element.select("h2 a").text()
                setUrlWithoutDomain(element.select("h2 a").attr("href"))
                thumbnail_url = element.select("img").attr("src")
            }
        }

        val hasNextPage = document.select("a:contains(Наступна)").isNotEmpty()

        return AnimesPage(animeList, hasNextPage)
    }

    // ===============================
    // Останні оновлення
    // ===============================

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/lastnews/page/$page")

    override fun latestUpdatesParse(response: okhttp3.Response): AnimesPage =
        popularAnimeParse(response)

    // ===============================
    // Пошук
    // ===============================

    override fun searchAnimeRequest(page: Int, query: String): Request =
        GET("$baseUrl/index.php?do=search&story=$query")

    override fun searchAnimeParse(response: okhttp3.Response): AnimesPage =
        popularAnimeParse(response)

    // ===============================
    // Деталі тайтлу
    // ===============================

    override fun animeDetailsParse(response: okhttp3.Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.select("h1").text()
            description = doc.select("div.full-text").text()
            genre = doc.select("a[rel=tag]").joinToString { it.text() }
            status = SAnime.COMPLETED
        }
    }

    // ===============================
    // Список епізодів
    // ===============================

    override fun episodeListParse(response: okhttp3.Response): List<SEpisode> {
        val doc = response.asJsoup()

        return doc.select("div.player-container a").mapIndexed { index, ep ->
            SEpisode.create().apply {
                name = "Серія ${index + 1}"
                episode_number = (index + 1).toFloat()
                setUrlWithoutDomain(ep.attr("href"))
            }
        }.reversed()
    }

    // ===============================
    // Відео
    // ===============================

    override fun videoListParse(response: okhttp3.Response): List<Video> {
        val doc = response.asJsoup()
        val iframe = doc.select("iframe").attr("src")

        return GenericVideoExtractor(client).videosFromUrl(iframe)
    }

    override fun videoListRequest(episode: SEpisode): Request =
        GET(baseUrl + episode.url)

    override fun videoUrlParse(response: okhttp3.Response): Video =
        videoListParse(response).first()
}
