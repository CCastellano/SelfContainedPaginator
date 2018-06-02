package com.scp.extractors.metadata;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class ExtractMetadata {

    public static void main(String[] args) throws Exception{
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        url = new URL("http://www.scp-wiki.net/attribution-metadata");
        Document doc = Jsoup.parse(url,3000);

        Element table = doc.select("table").get(0);
        Elements rows = table.select("tr");
        ArrayList<Metadata> meta = new ArrayList<>();
        for(Element row: rows){
            Elements data = row.select("td");
            if(data.size() > 0) {
                meta.add(new Metadata(data.get(0).text(),data.get(1).text(),data.get(2).text(),data.get(3).text()));
            }
        }
        for(Metadata metadata : meta){
            System.out.println(metadata.toString());
        }
    }
}
