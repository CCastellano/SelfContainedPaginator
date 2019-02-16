package com.scp.extractors.metadata;

import com.scp.connection.CloseableStatement;
import com.scp.connection.Connector;
import com.scp.connection.Queries;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;

public class ExtractMetadata {

    public static void extractMetadata() throws Exception {
        URL url = new URL("http://www.scp-wiki.net/attribution-metadata");
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
            CloseableStatement stmt = Connector.getStatement(
                    Queries.getQuery("insertMetadata"), metadata.getTitle().toLowerCase(), metadata.getUsername().toLowerCase(),
                    metadata.getAuthorageType().toLowerCase(), metadata.getDate());
            stmt.executeUpdate();
        }
    }
}
