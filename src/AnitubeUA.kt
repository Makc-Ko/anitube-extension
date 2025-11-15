package eu.animaru.extension

import eu.aniyomi.animeextension.AnimeSource
import eu.aniyomi.animeextension.Anime
import eu.aniyomi.animeextension.Episode
import eu.aniyomi.animeextension.Video
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class AnitubeUA : AnimeSource {
    override val name = "Anitube UA"
    override val baseUrl = "https://anitube.ua"
    override val client: OkHttpClient = OkHttpClient()

    // Отримання списку аніме
    override fun getAnimeList(): List<Anime> {
        val html = client.newCall(Request.Builder().url("$baseUrl/anime-list").build())
            .execute().body?.string() ?: return emptyList()
        val doc = Jsoup.parse(html)
        return doc.select(".anime-item a").map {
            Anime(
                title = it.text(),
                url = it.attr("href")
            )
        }
    }

    // Отримання епізодів для конкретного аніме
    override fun getEpisodes(anime: Anime): List<Episode> {
        val html = client.newCall(Request.Builder().url(anime.url).build())
            .execute().body?.string() ?: return emptyList()
        val doc = Jsoup.parse(html)
        return doc.select(".episode-list a").mapIndexed { index, el ->
            Episode(
                name = el.text(),
                url = el.attr("href"),
                number = index + 1
            )
        }
    }

    // Отримання відео для епізоду
    override fun getVideos(episode: Episode): List<Video> {
        val html = client.newCall(Request.Builder().url(episode.url).build())
            .execute().body?.string() ?: return emptyList()
        val doc = Jsoup.parse(html)
        return doc.select("video source").map {
            Video(it.attr("src"), it.attr("type"))
        }
    }
}
