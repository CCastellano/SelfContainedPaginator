package com.scp;

import java.util.List;

public class Staff {

    private String username;
    private List<String> teams;
    private String timeZone;
    private String activityLevel;
    private List<String> contactMethods;

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
        this.username = username;
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
        this.timeZone = timeZone;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public List<String> getContactMethods() {
        return contactMethods;
    }

    public void setContactMethods(List<String> contactMethods) {
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
        return sb.toString();
    }
}
