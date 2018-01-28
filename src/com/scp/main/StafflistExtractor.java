package com.scp.main;

import com.scp.Staff;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StafflistExtractor {


    final static String userregex = ".*user:info\\/(.*)\\\"\\s.*";
    final static String sectionRegex = "<h1 id=\"toc[0-9]\"><span>(.*)</span.*";
    final static String topregex = "<h1 id=\\\"toc[0-9]\"><span>(.*)<\\/span><\\/h1>";
    final static String tdregex = "<td>(.*)<\\/td>";
    final static Pattern pattern = Pattern.compile(userregex);
    final static Pattern tdPatter = Pattern.compile(tdregex);
    final static Pattern topPattern = Pattern.compile(topregex);
    final static ArrayList<String> categories = new ArrayList<>();

    final static ArrayList<Staff> staffList = new ArrayList<>();

    static{
        categories.add("Moderators");
        categories.add("Administrators");
        categories.add("Operational Staff");
        categories.add("Junior Staff");
    }

    public static void main(String[] args) throws Exception {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;


        url = new URL("http://scp-sandbox-3.wikidot.com/tretterstaff");
        is = url.openStream();  // throws an IOException
        br = new BufferedReader(new InputStreamReader(is));

        while ((line = br.readLine()) != null) {

            // System.out.println(line);
            if (line.contains("tr")) {
                // handleSection(line, br);
            } else if (line.contains("<h1 id=\"toc")) {
                Matcher matcher = topPattern.matcher(line);
                if (matcher.find()) {
                    if(categories.contains(matcher.group(1))){
                        handleSection(line, br, matcher.group(1));
                    }
                }


            }
        }

        for(Staff staff: staffList){
            System.out.println(staff.toString());
        }
    }
    private static String dataRegex = "<td>(.*)<.*";
    final static Pattern dataPattern = Pattern.compile(dataRegex);
    private static void handleSection(String line, BufferedReader br, String type) throws Exception {


            br.readLine();
            while(!line.contains("</tr>")){
               line =  br.readLine();
            }
            line = br.readLine();
            while(!line.contains("</table>")){
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
                System.out.println("teams " + line);
                Matcher matcher = dataPattern.matcher(line);
                System.out.println(matcher.matches());
                if(!matcher.group(1).isEmpty()) {
                    staffMember.setTeams(Arrays.asList(matcher.group(1).split(",")));
                }
                if(!type.equals("Junior Staff")) {
                    line = br.readLine();
                    System.out.println("TimeZone " + line);
                    matcher = dataPattern.matcher(line);
                    matcher.matches();
                    if(!matcher.group(1).isEmpty()) {
                        staffMember.setTimeZone(matcher.group(1));
                    }
                    line = br.readLine();
                    System.out.println("Activity " + line);
                    matcher = dataPattern.matcher(line);
                    matcher.matches();
                    if(!matcher.group(1).isEmpty()) {
                        staffMember.setActivityLevel(matcher.group(1));
                    }
                    line = br.readLine();
                    matcher = dataPattern.matcher(line);
                    matcher.matches();
                    if(!matcher.group(1).isEmpty()) {
                        staffMember.setContactMethods(Arrays.asList(matcher.group(1).split(",")));
                    }
                    System.out.println("Contacts " + line);
                }
                staffList.add(staffMember);
                br.readLine();
                line = br.readLine();
            }




        /*
        while (!line.contains("</table>")) {
            while (!line.contains("<td>")) {
                if(line.contains("</table>")){
                    break;
                }
                line = br.readLine();
            }
            while (line.contains("<td>")) {
                staffList.add(new Staff());
                if (!line.contains("<td></td>")) {
                    // System.out.println(line);
                    if (line.contains("user:info")) {
                        final Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            System.out.println(matcher.group(1));
                        }
                    } else {
                        final Matcher matcher = tdPatter.matcher(line);
                        if (matcher.find()) {
                            System.out.println(matcher.group(1));
                        } else {
                            System.out.println("There was an issue, here's the line: " + line);
                        }

                    }
                }
                line = br.readLine();
            }
        }*/
    }
}
