package com.scp.main;

import com.scp.connection.CloseableStatement;
import com.scp.connection.Connector;
import com.scp.connection.Queries;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StafflistExtractor {


    final static String userregex = ".*user:info\\/(.*)\\\"\\s.*";
    final static String sectionRegex = "<h1 id=\"toc[0-9]\"><span>(.*)</span.*";
    final static String topregex = "<h1 id=\"toc[0-9]\"><span>(.*)</span></h1>";
    final static String tdregex = "<td>(.*)<\\td>";
    final static Pattern pattern = Pattern.compile(userregex);
    final static Pattern tdPatter = Pattern.compile(tdregex);
    final static Pattern topPattern = Pattern.compile(topregex);
    final static ArrayList<String> categories = new ArrayList<>();

    private static ArrayList<String> activityTypes = new ArrayList<String>();
    private static ArrayList<String> timeZones = new ArrayList<String>();

    final static ArrayList<Staff> staffList = new ArrayList<>();
    final static ArrayList<Staff> currentStaff = new ArrayList<>();

    final static HashMap<String, List<Integer>> staffTeams = new HashMap<>();


    static {
        categories.add("Moderators");
        categories.add("Administrators");
        categories.add("Operational Staff");
        categories.add("Junior Staff");
    }

    private static void getStaff() {
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("get_staff"));
            ResultSet rs = stmt.executeQuery();
            while (rs != null && rs.next()) {
                Staff staffMember = new Staff();
                staffMember.setUsername(rs.getString("username"));
                staffMember.setTimeZone(rs.getString("timezone"));
                staffMember.setContactMethods(rs.getString("contact_methods"));
                staffMember.setActivityLevel(rs.getString("activity_level"));
                staffMember.setStaff_id(rs.getInt("staff_id"));
            }
        } catch (Exception e) {

        }
    }

    private static void getTeams() {
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("get_teams"));
            ResultSet rs = stmt.executeQuery();
            while (rs != null && rs.next()) {
                staffTeams.computeIfAbsent(rs.getString("team_name"), k -> staffTeams.put(k, new ArrayList<Integer>()));
                staffTeams.get(rs.getString("team_name")).add(rs.getInt("staff_id"));
            }
        } catch (
                Exception e) {
        }

    }


    public static void main(String[] args) throws Exception {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        getStaff();
        getTeams();
        clearCaptains();


        url = new URL("http://scp-sandbox-3.wikidot.com/tretterstaff");
        is = url.openStream();  // throws an IOException
        br = new BufferedReader(new InputStreamReader(is));

        while ((line = br.readLine()) != null) {

            if (line.contains("<h1 id=\"toc")) {
                Matcher matcher = topPattern.matcher(line);
                if (matcher.find()) {
                    if (categories.contains(matcher.group(1))) {
                        handleSection(line, br, matcher.group(1));
                    }
                }


            }
        }

        for (Staff staff : staffList) {
            System.out.println(staff.toString());
        }
        for (String s : activityTypes) {
            System.out.println(s);
        }
        for (String s : timeZones) {
            System.out.println(s);
        }

        for(Staff staff: staffList){
            if(currentStaff.contains(staff)){
                updateStaff(staff);
            }else{
                staff.setStaff_id(addStaff(staff));
            }

            for(String team: staff.getTeams()){
                team = team.trim();
                if(!staffTeams.containsKey(team)){
                    addTeam(team);
                    addStaffToTeam(team,staff.getStaff_id());


                }else{
                    if(staff.getStaff_id() != null && !staffTeams.get(team).contains(staff.getStaff_id())){
                        addStaffToTeam(team, staff.getStaff_id());
                    }
                }
            }
            for(String captaincies : staff.getCaptaincies()){
                captaincies = captaincies.trim();
                insertCaptain(captaincies, staff.getStaff_id());
            }
        }

        for(Staff staff: currentStaff){
            if(!staffList.contains(staff)){
                removeStaffMember(staff);
            }
        }
    }

    private static void removeStaffMember(Staff staff){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("remove_staff_captains"),staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
             stmt = Connector.getStatement(Queries.getQuery("remove_staff_from_teams"),staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
            stmt = Connector.getStatement(Queries.getQuery("remove_staff"),staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static Integer addStaff(Staff staff){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("insert_staff"),
                    staff.getUsername(),staff.getTimeZone(), staff.getContactMethods(),staff.getActivityLevel());
            ResultSet rs = stmt.execute();
            Integer value =  rs != null && rs.next() ? rs.getInt("staff_id") : -1;
            rs.close();
            return value;
        }catch(Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    private static void updateStaff(Staff staff){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("update_staff"),
                    staff.getUsername(),staff.getTimeZone(), staff.getContactMethods(),staff.getActivityLevel(), staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void addTeam(String teamName){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("make_new_team"),teamName);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void addStaffToTeam(String teamName, int staffid){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("add_to_team"),teamName, staffid);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void clearCaptains(){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("clear_captains"));
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void insertCaptain(String team, Integer staffID){
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("insert_captain"), team, staffID);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static String dataRegex = "<td>(.*)<.*";
    final static Pattern dataPattern = Pattern.compile(dataRegex);

    private static void handleSection(String line, BufferedReader br, String type) throws Exception {
        List<String> rejected = new ArrayList<String>();
        rejected.add("-");
        rejected.add("none");

        br.readLine();
        while (!line.contains("</tr>")) {
            line = br.readLine();
        }
        line = br.readLine();
        while (!line.contains("</table>")) {
            Staff staffMember = new Staff();
            staffMember.setLevel(type);
            line = br.readLine();
            if (line.contains("user:info")) {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    System.out.println("User " + matcher.group(1));
                    staffMember.setUsername(matcher.group(1));
                }
            }
            line = br.readLine();
            // System.out.println("teams " + line);
            Matcher matcher = dataPattern.matcher(line);
            matcher.matches();
            //System.out.println(matcher.matches());
            if (!matcher.group(1).isEmpty()) {
                if (!rejected.contains(matcher.group(1).toLowerCase())) {
                    staffMember.setTeams(Arrays.asList(matcher.group(1).split(",")));
                }
            }
            if (!type.equals("Junior Staff")) {
                line = br.readLine();
                //  System.out.println("TimeZone " + line);
                matcher = dataPattern.matcher(line);
                matcher.matches();
                if (!matcher.group(1).isEmpty()) {
                    if (!rejected.contains(matcher.group(1).toLowerCase())) {
                        staffMember.setTimeZone(matcher.group(1));
                        if (!timeZones.contains(matcher.group(1).toLowerCase())) {
                            timeZones.add(matcher.group(1).toLowerCase());
                        }
                    }
                }
                line = br.readLine();
                //System.out.println("Activity " + line);
                matcher = dataPattern.matcher(line);
                matcher.matches();
                if (!matcher.group(1).isEmpty()) {
                    if (!rejected.contains(matcher.group(1).toLowerCase())) {
                        staffMember.setActivityLevel(matcher.group(1));
                        if (!activityTypes.contains(matcher.group(1).toLowerCase())) {
                            activityTypes.add(matcher.group(1).toLowerCase());
                        }
                    }
                }
                line = br.readLine();
                matcher = dataPattern.matcher(line);
                matcher.matches();
                if (!matcher.group(1).isEmpty()) {
                    if (!rejected.contains(matcher.group(1).toLowerCase())) {
                        staffMember.setContactMethods(matcher.group(1));
                    }
                }
                //System.out.println("Contacts " + line);
                line = br.readLine();
                matcher = dataPattern.matcher(line);
                matcher.matches();
                if (!matcher.group(1).isEmpty()) {
                    if (!rejected.contains(matcher.group(1).toLowerCase())) {
                        staffMember.setCaptaincies(Arrays.asList(matcher.group(1).split(",")));
                    }
                }
                //System.out.println("Captaincies " + line);
            }
            staffList.add(staffMember);
            br.readLine();
            line = br.readLine();
        }

    }
}
