package com.scp.cappucino;

import com.google.common.base.Stopwatch;
import com.scp.rpc.RpcUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Skippucino {

    public static void main(String[] args) {
        Stopwatch timer = Stopwatch.createStarted();
        Set<String> pages = RpcUtils.listPages();
        int i = 0;
        System.out.println(pages.size());
        pages.stream().filter(page -> !page.contains("fragment:") && !page.contains("forum:")).sorted().forEach(page -> {
            try {
                Document doc = Jsoup.connect("http://www.scp-wiki.net/" + page).get();
                List<String> tags = doc.select("div.page-tags")
                        .select("a").stream().map(Element::text).collect(Collectors.toList());
                String ratingString = doc.getElementById("prw54355") == null ?
                        doc.select("span.number.prw54353") == null ? "0" : doc.select("span.number.prw54353").text()
                        : doc.getElementById("prw54355").text();
                Integer rating = Integer.parseInt(ratingString == null || ratingString.isEmpty() ? "0" : ratingString);
                String title = doc.getElementById("page-title") == null ? page : doc.getElementById("page-title").text().trim();
                System.out.println(title + ": " + rating + " " + tags);
            } catch (Exception e) {
                System.out.println("Exception for page: " + page);
                e.printStackTrace();
            }
        });
        System.out.println("Full processing took: " + timer.elapsed(TimeUnit.SECONDS));
/*
        file = new File("C:/LAS/data/attrmeta.html");
        doc = Jsoup.parse(file,"UTF-8", "www.test.com");
        Map<String, List<MetaData>> titleMetaList = new HashMap<>();
        doc.select("tr").stream().skip(1)
                .map(tableRow -> new MetaData(tableRow.getElementsByTag("td")))
                .forEach(meta -> {
                    titleMetaList.computeIfAbsent(meta.getTitle(), m -> new ArrayList<>());
                    titleMetaList.get(meta.getTitle()).add(meta);
                });
        */

    }

    public static class MetaData {

        public String title;
        public String author;
        public String type;
        public String date;

        public MetaData(List<Element> nodes) {
            title = nodes.get(0).text().trim();
            author = nodes.get(1).text().trim();
            type = nodes.get(2).text().trim();
            date = nodes.get(3).text().trim();
        }

        public String getTitle() {
            return title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MetaData metaData = (MetaData) o;

            return title != null ? title.equals(metaData.title) : metaData.title == null;
        }

        @Override
        public int hashCode() {
            return title != null ? title.hashCode() : 0;
        }
    }
}
