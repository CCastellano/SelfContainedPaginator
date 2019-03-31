package com.scp.main;

import com.google.common.util.concurrent.RateLimiter;
import com.scp.connection.CloseableStatement;
import com.scp.connection.Configs;
import com.scp.connection.Connector;
import com.scp.connection.Queries;
import com.scp.extractors.metadata.ExtractMetadata;
import com.scp.rpc.RpcUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.jsoup.Jsoup;
import org.postgresql.util.PSQLException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class PageUploader {
	private static final Logger logger = Logger.getLogger(PageUploader.class);

    private static ConcurrentHashMap<String, Page> pages;


    public PageUploader() {
        loadDatabasePages();
        loadDatabaseTags();
        boolean clean = cleanUpPages();
        if (clean) {
            gatherMetadata();
            uploadSeries();
        } else {
            logger.error("There was an issue, and we can't clean-run the upload.");
        }
    }

    public static void main(String[] args) {
        try {
            new PageUploader();
            CloseableStatement stmt = Connector.getStatement(Queries.getQuery("deleteOldPages"));
            stmt.executeUpdate();
            stmt = Connector.getStatement(Queries.getQuery("deleteOldtags"));
            stmt.executeUpdate();
            StafflistExtractor.updateStaff();
            ExtractMetadata.extractMetadata();
            logger.info("Completed site upload.");
        } catch (Exception e) {
            logger.error("Error checking if update required.", e);
        }
    }

    private void loadDatabasePages() {
        pages = new ConcurrentHashMap<>();
		try {
			CloseableStatement stmt = Connector.getStatement(Queries
					.getQuery("getStoredPages"));
			ResultSet rs = stmt.getResultSet();
			logger.info("Beginning load of Stored Pages");
			while (rs != null && rs.next()) {

				try {
					pages.put(rs.getString("pagename"),
							new Page(rs.getString("pagename") == null ? "" : rs.getString("pagename"),
							rs.getString("title") == null ? "" : rs.getString("pagename"),
							rs.getInt("rating"),
							rs.getString("created_by") == null ? "" : rs.getString("created_by"),
							rs.getTimestamp("created_on"),
							rs.getBoolean("scpPage"),
							rs.getString("scpTitle") == null ? "" : rs.getString("scpTitle")));
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

    private void uploadSeries() {
		String regex = ".+href=\\\"\\/(.+)\">(.+)<\\/a>.+- (.+?)<\\/.+>";
		Pattern r = Pattern.compile(regex);
		logger.info("Beggining gather of series pages: 1, 2, 3, 4 and jokes");
		String[] series = new String[] { "scp-series	", "scp-series-2",
				"scp-series-3", "scp-series-4", "scp-series-5", "joke-scps" };

		for (String page : series) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("site", "scp-wiki");
			params.put("page", page);

			try {
				@SuppressWarnings("unchecked")
                HashMap<String, Object> result = (HashMap<String, Object>) RpcUtils.pushToAPI(
						"pages.get_one", params);

                Arrays.stream(((String) result.get("html")).split("\n"))
                        .filter(line -> r.matcher(line).find())
                        .map(line -> {
                            Matcher m = r.matcher(line);
                            m.matches();
                            return new String[]{m.group(1), m.group(2),
                                    Jsoup.parse(m.group(3)).text()};
                        })
                        .collect(Collectors.toList())
                        .stream()
                        .filter(pageParts -> pages.containsKey(pageParts[0]))
                        .filter(pageParts -> !pages.get(pageParts[0]).getScpTitle().equalsIgnoreCase(pageParts[2]))
                        .forEach(update -> {
                            try {
                                CloseableStatement stmt = Connector.getStatement(
                                        Queries.getQuery("updateTitle"), update[1],
                                        update[2], update[0]);
                                stmt.executeUpdate();
                            } catch (Exception e) {
                                logger.error("There was an exception updatinga title: ", e);
                            }
                        });


            } catch (XmlRpcException e) {
                if (e.getMessage() != null && !e.getMessage().contains("unique")) {
					logger.error(
							"There was an exception attempting to grab the series page metadata",
							e);
				}
			}
		}
		logger.info("Finished gathering series pages");
	}


    private void loadDatabaseTags() {
		logger.info("Gathering current tags.");
		int tags = 0;
		try {
			CloseableStatement stmt = Connector.getStatement(
					Queries.getQuery("getAllTags"));
			ResultSet rs = stmt.execute();
			while(rs != null && rs.next()){
                pages.get(rs.getString("pagename")).getTags().add(rs.getString("tag"));
				tags++;
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			logger.error("Exception trying to get all pagetags.",e);
		}
        logger.info("Gathered " + tags);
    }

    private void gatherMetadata() {
		try {
			logger.info("Gathering metadata.");
			int j = 0;
			Page[] pageSet = new Page[10];
			RateLimiter limiter = RateLimiter.create(200.0 / 60.0);
			for (String str : pages.keySet()) {
				Page page = pages.get(str);
					if (j < 10) {
						pageSet[j] = page;
						j++;
					} else {
						limiter.acquire();
						getPageInfo(pageSet);
						pageSet = new Page[10];
						j = 0;
					}
			}
			logger.info("Finished gathering metadata");
		} catch (Exception e) {
			logger.error(
					"There was an error attempting to get pages in groups of ten",
					e);
		}
	}


    private boolean cleanUpPages() {
		try {
			logger.info("Beginning site-wide page gather");

            Set<String> pageList = RpcUtils.listPages();
            if (pageList != null) {
                pageList.stream()
                        .filter(pageName -> !pages.containsKey(pageName))
                        .forEach(pageName -> {
                            try {
                                logger.info("Inserting new page: " + pageName);
                                CloseableStatement stmt = Connector.getStatement(
                                        Queries.getQuery("insertPage"), pageName, pageName);
                                stmt.executeUpdate();
                                pages.put(pageName, new Page(pageName));
                            } catch (Exception e) {
                                if (!e.getMessage().contains("unique")) {
                                    logger.error("Couldn't insert page name", e);
                                }
                            }
                        });

				List<String> pagesToRemove = pages.keySet().stream()
						.filter(pageName -> !pageList.contains(pageName))
						.collect(Collectors.toList());


				for (String pageName : pagesToRemove) {
                            try {
                                logger.info("Deleting removed page: " + pageName);
                                pages.remove(pageName);
                                CloseableStatement stmt = Connector.getStatement(Queries.getQuery("deletePage"), pageName);
                                stmt.executeUpdate();
                            } catch (SQLException e) {
                                logger.error("Exception attempting to delete a page", e);
                            }
				}


                logger.info("Ending site-wide page gather");
                return true;
            } else {
                logger.error("There was an issue with gathering the pagelist, aborting.");
                return false;
            }
		} catch (Exception e) {
			logger.error("There was an exception", e);
		}
        return false;
	}

    private String getPageInfo(Page[] pageList) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site", Configs.getSingleProperty("site").getValue());
		String[] pageNames = new String[10];
        for (int i = 0; i < pageList.length; i++) {
            pageNames[i] = pageList[i].getPageLink();
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
            HashMap<String, HashMap<String, Object>> result = (HashMap<String, HashMap<String, Object>>) RpcUtils.pushToAPI(
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

                    List<String> tagsToInsertForPage = new ArrayList<String>();
                    List<String> tagsToDeleteForPage = new ArrayList<String>();
					ArrayList<String> tagsOnWebVersion = new ArrayList<String>();

					for (Object tag : tags) {
						tagsOnWebVersion.add(tag.toString());

					}
                    if (pages.get(targetName) != null) {

                        tagsToDeleteForPage = pages.get(targetName).getTags().stream()
                                .filter(tag -> !tagsOnWebVersion.contains(tag))
                                .collect(Collectors.toList());

                        tagsToInsertForPage = tagsOnWebVersion.stream()
                                .filter(tag -> !pages.get(targetName).getTags().contains(tag))
                                .collect(Collectors.toList());

                    } else {
						tagsToInsertForPage.addAll(tagsOnWebVersion);
                        logger.info("No page found in the DB for page: " + targetName);
					}


					for (String tag : tagsToInsertForPage) {
						try {
                            logger.info("Inserting tag: " + tag + " for page: " + targetName);
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
                        logger.info("Removing tag: " + tag + " for page: " + targetName);
						CloseableStatement stmt = Connector.getStatement(
								Queries.getQuery("deletePageTag"), targetName,
								tag);
						stmt.executeUpdate();
						
					}

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
