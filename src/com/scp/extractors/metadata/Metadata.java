package com.scp.extractors.metadata;

import org.joda.time.DateTime;

import java.time.LocalDate;

public class Metadata {

    private String title;
    private String username;
    private String authorageType;
    private LocalDate date;

    public Metadata(String title, String username, String authorageType, String date){
        this.title = title;
        this.username = username;
        this.authorageType = authorageType;
        if(!date.isEmpty()) {
            String[] tokens = date.split("-");
            this.date = LocalDate.of(Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
        }
    }

    public String toString(){
        return "Article: " + title + " author: " + username + " authorage type: " + authorageType
                + (date != null ? " date:  " + date.toString() : "");
    }
}
