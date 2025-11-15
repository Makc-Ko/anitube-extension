import axios from "axios";
import * as cheerio from "cheerio";

export default class AnitubeProvider {
  name = "Anitube";
  base = "https://anitube.ua";

  async search(query) {
    const url = ${this.base}/?do=search&subaction=search&story=${encodeURIComponent(query)};
    const html = (await axios.get(url)).data;
    const $ = cheerio.load(html);

    const list = [];

    $(".story").each((_, el) => {
      const title = $(el).find(".zag a").text().trim();
      const link = $(el).find(".zag a").attr("href");
      const cover = $(el).find("img").attr("src");

      if (title && link) {
        list.push({
          id: link,
          title,
          cover: cover?.startsWith("http") ? cover : this.base + cover,
          url: link
        });
      }
    });

    return list;
  }

  async info(url) {
    const html = (await axios.get(url)).data;
    const $ = cheerio.load(html);

    const title = $("h1").text().trim();
    const cover = $(".story_c img").attr("src");
    const description = $(".full-text").text().trim();

    const episodes = [];

    $(".player iframe").each((i, el) => {
      const src = $(el).attr("src");

      episodes.push({
        id: ${url}#${i + 1},
        title: Епізод ${i + 1},
        url: src
      });
    });

    return {
      title,
      cover: cover?.startsWith("http") ? cover : this.base + cover,
      description,
      episodes
    };
  }

  async stream(url) {
    return {
      streams: [
        {
          url,
          quality: "auto"
        }
      ]
    };
  }
}