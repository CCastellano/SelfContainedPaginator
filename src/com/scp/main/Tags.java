package com.scp.main;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.scp.connection.CloseableStatement;
import com.scp.connection.Connector;
import com.scp.connection.Queries;

public class Tags {
	
	private final static Logger logger = Logger.getLogger(Tags.class);
	
	static ArrayList<String> getTags(String pagename) {
		ArrayList<String> tags = null;
		try {
			tags = new ArrayList<String>();
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("getTags"), pagename);
			ResultSet tagSet = stmt.getResultSet();
			while (tagSet != null && tagSet.next()) {
				tags.add(tagSet.getString("tag"));
			}
			tagSet.close();
			stmt.close();
		} catch (Exception e) {
			logger.error("There was an exception attempting to grab tags", e);
		}
		return tags;
	}

}
