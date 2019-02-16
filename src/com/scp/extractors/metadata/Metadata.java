package com.scp.extractors.metadata;

public class Metadata {

    private String title = "";
    private String username = "";
    private String authorageType = "";
    private String date = "";

    public Metadata(String title, String username, String authorageType, String date){
        this.title = title;
        this.username = username;
        this.authorageType = authorageType;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthorageType() {
        return authorageType;
    }

    public void setAuthorageType(String authorageType) {
        this.authorageType = authorageType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String toString(){
        return "Article: " + title + " author: " + username + " authorage type: " + authorageType
                + (date != null ? " date:  " + date : "");
    }
}
