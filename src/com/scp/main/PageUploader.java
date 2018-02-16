package com.scp.main;

import java.net.URL;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.jsoup.Jsoup;
import org.postgresql.util.PSQLException;

import com.scp.connection.CloseableStatement;
import com.scp.connection.Configs;
import com.scp.connection.Connector;
import com.scp.connection.Queries;



public class PageUploader {
	private static final Logger logger = Logger.getLogger(PageUploader.class);
	private static XmlRpcClientConfigImpl config;
	private static XmlRpcClient client;
	private static ArrayList<Page> pages;
	private static HashSet<String> pageTitles;

	static {
		config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(Configs.getSingleProperty(
					"wikidotServer").getValue()));
			config.setBasicUserName(Configs.getSingleProperty("appName")
					.getValue());
			config.setBasicPassword(Configs.getSingleProperty("wikidotapikey")
					.getValue());
			config.setEnabledForExceptions(true);
			config.setConnectionTimeout(10 * 1000);
			config.setReplyTimeout(30 * 1000);

			client = new XmlRpcClient();
			client.setTransportFactory(new XmlRpcSun15HttpTransportFactory(
					client));
			client.setTypeFactory(new XmlRpcTypeNil(client));
			client.setConfig(config);

		} catch (Exception e) {
			logger.error("There was an exception", e);
		}

	}

	private static void loadPages() {
		pages = new ArrayList<Page>();
		pageTitles = new HashSet<String>();
		try {
			CloseableStatement stmt = Connector.getStatement(Queries
					.getQuery("getStoredPages"));
			ResultSet rs = stmt.getResultSet();
			logger.info("Beginning load of Stored Pages");
			while (rs != null && rs.next()) {

				try {
					pageTitles.add(rs.getString("pagename"));
					pages.add(new Page(rs.getString("pagename") == null ? ""
							: rs.getString("pagename"),
							rs.getString("title") == null ? "" : rs
									.getString("pagename"),
							rs.getInt("rating"),
							rs.getString("created_by") == null ? "" : rs
									.getString("created_by"), rs
									.getTimestamp("created_on"), rs
									.getBoolean("scpPage"), rs
									.getString("scpTitle") == null ? "" : rs.getString("scpTitle"),
									Tags.getTags(rs.getString("pagename"))));
				} catch (PSQLException e) {
					logger.error("Couldn't create page, keep going", e);
				}

			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			logger.error("There was an exception retreiving stored pages", e);
		}
	}

	public static void main(String[] args) {
		try {
			loadPages();
			getTags();
			listPage();
			gatherMetadata();
			uploadSeries();
			
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("deleteOldtags"));
			stmt.executeUpdate();
			StafflistExtractor.updateStaff();

		} catch (Exception e) {
			logger.error("Error checking if update required.", e);
		}
	}

	public static void uploadSeries() {
		String regex = "(?m)<li><a href=\"\\/(.+)\">(.+)<\\/a> - (.+)<\\/li>";
		Pattern r = Pattern.compile(regex);
		logger.info("Beggining gather of series pages: 1, 2, 3, 4 and jokes");
		String[] series = new String[] { "scp-series	", "scp-series-2",
				"scp-series-3", "scp-series-4", "joke-scps" };

		for (String page : series) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("site", "scp-wiki");
			params.put("page", page);

			try {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> result = (HashMap<String, Object>) pushToAPI(
						"pages.get_one", params);

				String[] lines = ((String) result.get("html")).split("\n");
				ArrayList<String[]> pagelist = new ArrayList<String[]>();
				for (String s : lines) {
					Matcher m = r.matcher(s);
					if (m.find()) {
						pagelist.add(new String[] { m.group(1), m.group(2),
								Jsoup.parse(m.group(3)).text() });
					}
				}

				ArrayList<String[]> insertPages = new ArrayList<String[]>();
				ArrayList<String[]> updateList = new ArrayList<String[]>();

				for (String[] pageParts : pagelist) {
					boolean found = false;
					for (Page pageOb : pages) {
						if (pageOb.getPageLink().equalsIgnoreCase(pageParts[0])) {
							found = true;
							if (!pageOb.getScpTitle().equalsIgnoreCase(
									pageParts[1])) {
								updateList.add(pageParts);
								pageOb.setScpTitle(pageParts[0]);
								pageOb.setScpPage(true);
							}
						}
					}
					if (!found) {
						// This should never actually happen. The site scrape
						// should handle this.
						insertPages.add(pageParts);
					}
				}

				for (String[] insert : insertPages) {
					CloseableStatement stmt = Connector.getStatement(
							Queries.getQuery("insertPage"), insert[0],
							insert[2]);
					stmt.executeUpdate();
				}
				for (String[] update : updateList) {
					CloseableStatement stmt = Connector.getStatement(
							Queries.getQuery("updateTitle"), update[2],
							update[0]);
					stmt.executeUpdate();

					stmt = Connector.getStatement(Queries.getQuery("scpTitle"),update[1],update[0]);
					stmt.executeUpdate();
				}

			} catch (Exception e) {
				if (!e.getMessage().contains("unique")) {
					logger.error(
							"There was an exception attempting to grab the series page metadata",
							e);
				}
			}
		}
		logger.info("Finished gathering series pages");
	}

	private static Map<String, ArrayList<String>> pageTags = new HashMap<>();

	private static void getTags(){
		logger.info("Gathering current tags.");
		int tags = 0;
		try {
			CloseableStatement stmt = Connector.getStatement(
					Queries.getQuery("getAllTags"));
			ResultSet rs = stmt.execute();
			while(rs != null && rs.next()){
				String page = rs.getString("pagename");
				String tag = rs.getString("tag");
				if(!pageTags.containsKey(page)){
					pageTags.put(page, new ArrayList<String>());
				}
				pageTags.get(page).add(tag);
				tags++;
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			logger.error("Exception trying to get all pagetags.",e);
		}
		logger.info("Gathered " + tags + " total tags for: " + pageTags.size());
	}

	private static void gatherMetadata() {
		try {
			logger.info("Gathering metadata.");
			int j = 0;
			Page[] pageSet = new Page[10];
			RateLimiter limiter = RateLimiter.create(200.0 / 60.0);
			for (Page str : pages) {
				if(pageTitles.contains(str.getPageLink())){
					if (j < 10) {
						pageSet[j] = str;
						j++;
					} else {
						limiter.acquire();
						getPageInfo(pageSet);
						pageSet = new Page[10];
						j = 0;
					}
				}
			}
			logger.info("Finished gathering metadata");
		} catch (Exception e) {
			logger.error(
					"There was an error attempting to get pages in groups of ten",
					e);
		}
	}
	
	

	private static Object pushToAPI(String method, Object... params)
			throws XmlRpcException {
		return (Object) client.execute(method, params);
	}

	public static void listPage() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site", Configs.getSingleProperty("site").getValue());
		try {
			logger.info("Beginning site-wide page gather");
			Object[] result = (Object[]) pushToAPI("pages.select", params);
			// Convert result to a String[]
			HashSet<String> pageList = new HashSet<String>();
			for (int i = 0; i < result.length; i++) {
				pageList.add( (String) result[i]);
			}
			for (String str : pageList) {
				if(!pageTitles.contains(str)) {
					try {
						CloseableStatement stmt = Connector.getStatement(
								Queries.getQuery("insertPage"), str, str);
						stmt.executeUpdate();
					} catch (Exception e) {
						if (!e.getMessage().contains("unique")) {
							logger.error("Couldn't insert page name", e);
						}
					}
				}
			}
			ArrayList<String> removals = new ArrayList<String>();
			for(String str: pageTitles){
				if(!pageList.contains(str)){
					removals.add(str);
				
					CloseableStatement stmt = Connector.getStatement(Queries.getQuery("deletePage"),str);
					stmt.executeUpdate();
				}
			}
			for(String str: removals){
				pageTitles.remove(str);
			}
			logger.info("Ending site-wide page gather");
		} catch (Exception e) {
			logger.error("There was an exception", e);
		}
	}

	public static String getPageInfo(Page[] pages) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site", Configs.getSingleProperty("site").getValue());
		String[] pageNames = new String[10];
		for (int i = 0; i < pages.length; i++) {
			pageNames[i] = pages[i].getPageLink();
		}
		params.put("pages", pageNames);
		ArrayList<String> keyswewant = new ArrayList<String>();
		keyswewant.add("title_shown");
		keyswewant.add("rating");
		keyswewant.add("created_at");
		keyswewant.add("title");
		keyswewant.add("created_by");
		keyswewant.add("tags");
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			@SuppressWarnings("unchecked")
			HashMap<String, HashMap<String, Object>> result = (HashMap<String, HashMap<String, Object>>) pushToAPI(
					"pages.get_meta", params);

			for (String targetName : result.keySet()) {
				try {
					// String title = (String)
					// result.get(targetName).get("title");
					String displayTitle = (String) result.get(targetName).get(
							"title_shown");
					Integer rating = (Integer) result.get(targetName).get(
							"rating");
					String creator = (String) result.get(targetName).get(
							"created_by");
					Date createdAt = df.parse((String) result.get(targetName)
							.get("created_at"));
					// For each page, if the tags don't match the database tags,
					Object[] tags = (Object[]) result.get(targetName).get(
							"tags");

					ArrayList<String> tagsToInsertForPage = new ArrayList<String>();
					ArrayList<String> tagsToDeleteForPage = new ArrayList<String>();
					ArrayList<String> tagsOnWebVersion = new ArrayList<String>();

					for (Object tag : tags) {
						tagsOnWebVersion.add(tag.toString());

					}
					if(pageTags.get(targetName) != null){

						for(String tag: tagsOnWebVersion) {
							if (!pageTags.get(targetName).contains(tag)) {
								tagsToInsertForPage.add(tag);
							}
						}

						for (String tag : pageTags.get(targetName)) {
							if(!tagsOnWebVersion.contains(tag)){
								tagsToDeleteForPage.add(tag);
							}
						}

					}else{
						tagsToInsertForPage.addAll(tagsOnWebVersion);
						logger.info("No page tags for page: " + targetName);
					}


					for (String tag : tagsToInsertForPage) {
						try {
							logger.info("CURRENT TAG: " + tag);
							CloseableStatement stmt = Connector.getStatement(
									Queries.getQuery("insertPageTag"),
									targetName, tag);
							stmt.executeUpdate();
						} catch (PSQLException e) {
							if (!e.getMessage().contains("unique")) {
								if (e.getMessage().contains("not-null")) {
									CloseableStatement stmt = Connector.getStatement(
											Queries.getQuery("insertTag"), tag);
									stmt.executeUpdate();

									stmt = Connector.getStatement(
											Queries.getQuery("insertPageTag"),
											targetName, tag);
									stmt.executeUpdate();


								} else {
									logger.error(
											"There was a problem inserting tags", e);
								}
							}
						}
					}

					for (String tag : tagsToDeleteForPage) {
						CloseableStatement stmt = Connector.getStatement(
								Queries.getQuery("deletePageTag"), targetName,
								tag);
						stmt.executeUpdate();
						
					}
					Connector.getStatement(Queries.getQuery("deleteOldtags")).executeUpdate();
					
					

					CloseableStatement stmt = Connector
							.getStatement(
									Queries.getQuery("updateMetadata"),
									displayTitle == null ? "unknown"
											: displayTitle,
									rating == null ? 0 : rating,
									creator == null ? "unknown" : creator,
									new java.sql.Timestamp(
											createdAt == null ? System
													.currentTimeMillis()
													: createdAt.getTime()),
									targetName);

					stmt.executeUpdate();
				} catch (Exception e) {
					logger.error("Error updating metadata", e);
				}
			}
		} catch (Exception e) {
			logger.error("There was an exception retreiving metadata", e);
		}

		return "I couldn't find anything matching that, apologies.";
	}
}
