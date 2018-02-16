package com.scp.connection;

import java.sql.ResultSet;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class Queries {

	private static HashMap<String, String> queryCache = new HashMap<String, String>();

	private final static String findQuery = "select * from queries";

	private static Boolean valid = false;
	private static final Logger logger = Logger.getLogger(Queries.class);

	public static void clear(){
		valid = false;
		loadQueries();
	}
	
	public static String getQuery(String query_name) {
		if (!valid) {
			loadQueries();
		}

		if (queryCache.containsKey(query_name)) {
			return queryCache.get(query_name);
		} else {
			logger.error("Cannot find query specified");
		}

		return null;
	}

	private static void loadQueries() {
		try {
			CloseableStatement stmt = Connector.getStatement(findQuery);
			ResultSet rs = stmt.getResultSet();
			while (rs != null && rs.next()) {
				queryCache.put(rs.getString("query_name"),
						rs.getString("query"));
			}
			logger.info(queryCache.toString());
			valid = true;
			stmt.close();
		} catch (Exception e) {
			logger.error("Exception trying to load queries in to cache", e);
		}
	}

}
