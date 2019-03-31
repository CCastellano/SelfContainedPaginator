package com.scp.main;

import com.scp.connection.CloseableStatement;
import com.scp.connection.Connector;
import com.scp.connection.Queries;
import org.apache.log4j.Logger;

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

    private final static Logger logger = Logger.getLogger(StafflistExtractor.class);


    private final static String userregex = ".*user:info\\/(.*)\"\\sonclick=.*userid=(.*)&amp;amp;size=.*return false;\">(.*)</a>";
    final static String sectionRegex = "<h1 id=\"toc[0-9]\"><span>(.*)</span.*";
    private final static String topregex = "<h1 id=\"toc[0-9]\"><span>(.*)</span></h1>";
    private final static String tdregex = "<td>(.*)<\\td>";
    private final static Pattern pattern = Pattern.compile(userregex);
    final static Pattern tdPatter = Pattern.compile(tdregex);
    private final static Pattern topPattern = Pattern.compile(topregex);
    private final static ArrayList<String> categories = new ArrayList<>();

    private static ArrayList<String> activityTypes = new ArrayList<String>();
    private static ArrayList<String> timeZones = new ArrayList<String>();
    private static String dataRegex = "<td>(.*)<.*";
    private final static Pattern dataPattern = Pattern.compile(dataRegex);

    private final static ArrayList<Staff> staffList = new ArrayList<>();
    private final static ArrayList<Staff> currentStaff = new ArrayList<>();

    private final static HashMap<String, List<Integer>> staffTeams = new HashMap<>();


    static {
        categories.add("Moderator");
        categories.add("Administrator");
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
                currentStaff.add(staffMember);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getTeams() {
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("get_teams"));
            ResultSet rs = stmt.executeQuery();
            while (rs != null && rs.next()) {
                String team_name = rs.getString("team_name");
                if(!staffTeams.containsKey(team_name)){
                    staffTeams.put(team_name, new ArrayList<Integer>());
                }
                staffTeams.get(rs.getString("team_name")).add(rs.getInt("staff_id"));
            }
            logger.info("staffTeams: " + staffTeams.keySet());
            stmt.close();
            rs.close();
        } catch (Exception e) {
            logger.error("Exception loading teams,",e);
        }

    }


    static void updateStaff() throws Exception {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        getStaff();
        getTeams();
        clearCaptains();


        url = new URL("http://05command.wikidot.com/staff-list");
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
        staffUpload();

    }

    private static void staffUpload(){
        for(Staff staff: staffList){
            try {
                if (currentStaff.contains(staff)) {
                    staff.setStaff_id(currentStaff.get(currentStaff.indexOf(staff)).getStaff_id());
                    updateStaff(staff);
                    //logger.info("Updated staff for: " + staff.getUsername());
                } else {
                    staff.setStaff_id(addStaff(staff));
                    currentStaff.add(staff);
                            logger.info("Inserted staff for: " + staff.getUsername());
                }
                if(staff.getTeams() != null) {
                    for (String team : staff.getTeams()) {
                        team = team.trim();
                        if (!staffTeams.containsKey(team)) {
                            addTeam(team);
                            staffTeams.put(team, new ArrayList<Integer>());
                            addStaffToTeam(team, staff.getStaff_id());

                        } else {
                            if (staff.getStaff_id() != null && !staffTeams.get(team).contains(staff.getStaff_id())) {
                                addStaffToTeam(team, staff.getStaff_id());
                            }
                        }
                    }
                }
                if(staff.getCaptaincies() != null) {
                    logger.info("Captaincy for user: " + staff.getUsername());
                    for (String captaincies : staff.getCaptaincies()) {
                        captaincies = captaincies.trim();
                        insertCaptain(captaincies, staff.getStaff_id());
                        logger.info("Inserted captaincy for user.");
                    }
                }
            }catch(Exception e){
                logger.info("Exception with staff upload.",e);
            }
        }

        for(Staff staff: currentStaff){
            if(!staffList.contains(staff)){
                removeStaffMember(staff);
            }
        }
    }

    private static Integer addStaff(Staff staff){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("insert_staff"),
                    staff.getUsername(),staff.getTimeZone(), staff.getContactMethods(),staff.getActivityLevel(), staff.getLevel(), staff.getDisplayName(), staff.getWikidotId());
            ResultSet rs = stmt.execute();
            Integer value =  rs != null && rs.next() ? rs.getInt("staff_id") : -1;
            rs.close();
            return value;
        }catch(Exception e){
            logger.error("Exception: ",e);
        }
        return -1;
    }

    private static void updateStaff(Staff staff){
        try{
            if(staff.getStaff_id() == null){
                logger.info("Issue");
            }
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("update_staff"),
                    staff.getUsername(),staff.getTimeZone(), staff.getContactMethods(),staff.getActivityLevel(), staff.getLevel(),staff.getWikidotId(), staff.getDisplayName(), staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            logger.error("Exception: ",e);        }
    }

    private static void addTeam(String teamName){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("make_new_team"),teamName);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            logger.error("Exception: ",e);        }
    }

    private static void addStaffToTeam(String teamName, int staffid){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("add_to_team"),teamName, staffid);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            logger.error("Exception: ",e);        }
    }

    private static void clearCaptains(){
        try{
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("clear_captains"));
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            logger.error("Exception: ",e);        }
    }

    private static void insertCaptain(String team, Integer staffID){
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("insert_captain"), team, staffID);
            stmt.executeUpdate();
            stmt.close();
        }catch(Exception e){
            logger.info("Exception!");
        e.printStackTrace();}
    }


    private static void removeStaffMember(Staff staff) {
        try {
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("remove_staff_captains"), staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
            stmt = Connector.getStatement(Queries.getQuery("remove_staff_from_teams"), staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
            stmt = Connector.getStatement(Queries.getQuery("remove_staff"), staff.getStaff_id());
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
    }

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
                    //logger.info("User " + matcher.group(1));
                    staffMember.setUsername(matcher.group(1));
                   staffMember.setWikidotId(Integer.valueOf(matcher.group(2)));
                   staffMember.setDisplayName(matcher.group(3));
                }
            }
            line = br.readLine();
            if(line.contains("userid=")){

            }
            // logger.info("teams " + line);
            Matcher matcher = dataPattern.matcher(line);
            matcher.matches();
            //logger.info(matcher.matches());
            if (!matcher.group(1).isEmpty()) {
                if (!rejected.contains(matcher.group(1).toLowerCase())) {
                    staffMember.setTeams(Arrays.asList(matcher.group(1).split(",")));
                }
            }
            if (!type.equals("Junior Staff")) {
                line = br.readLine();
                //  logger.info("TimeZone " + line);
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
                //logger.info("Activity " + line);
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
                //logger.info("Contacts " + line);
                line = br.readLine();
                matcher = dataPattern.matcher(line);
                matcher.matches();
                if (!matcher.group(1).isEmpty()) {
                    if (!rejected.contains(matcher.group(1).toLowerCase())) {
                        staffMember.setCaptaincies(Arrays.asList(matcher.group(1).split(",")));
                    }
                }
                //logger.info("Captaincies " + line);
            }
            staffList.add(staffMember);
            br.readLine();
            line = br.readLine();
        }

    }

    public static void main(String[] args) throws Exception{
        updateStaff();
    }
}
