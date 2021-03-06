package com.scp.main;

import java.util.List;

public class Staff {

    private String username;
    private List<String> teams;
    private String timeZone = "";
    private String activityLevel = "";
    private String contactMethods = "";

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    private String displayName = "";

    public int getWikidotId() {
        return wikidotId;
    }

    public void setWikidotId(int wikidotId) {
        this.wikidotId = wikidotId;
    }

    private int wikidotId;

    public Integer getStaff_id() {
        return staff_id;
    }

    public void setStaff_id(Integer staff_id) {
        this.staff_id = staff_id;
    }

    private Integer staff_id;

    public List<String> getCaptaincies() {
        return captaincies;
    }

    public void setCaptaincies(List<String> captaincies) {
        this.captaincies = captaincies;
    }

    private List<String> captaincies;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    private String level;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.trim();
    }

    public List<String> getTeams() {
        return teams;
    }

    public void setTeams(List<String> teams) {
        this.teams = teams;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone.trim();
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getContactMethods() {
        return contactMethods;
    }

    public void setContactMethods(String contactMethods) {
        this.contactMethods = contactMethods;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("User: " + username);
        if(level != null){
            sb.append(" | Staff Level: " +level);
        }
        if(teams != null && !teams.isEmpty()){
            sb.append(" | Teams: " + teams.toString());
        }
        if(timeZone != null){
            sb.append(" | TimeZone: " + timeZone);
        }
        if(activityLevel != null ){
            sb.append(" | Activity Level: " + activityLevel.toString());
        }
        if(contactMethods != null && !contactMethods.isEmpty() ){
            sb.append(" | Contact Methods: " + contactMethods.toString());
        }
        if(captaincies != null && !captaincies.isEmpty() ){
            sb.append(" | Captaincies: " + captaincies.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object staff){
        return staff instanceof Staff && ((Staff)staff).getUsername().equalsIgnoreCase(this.username);
    }
}
