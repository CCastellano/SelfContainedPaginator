package com.scp.connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class Configs {

	private static final Logger logger = Logger.getLogger(Configs.class);

	private static HashMap<String, ArrayList<Config>> cachedProperties = new HashMap<String, ArrayList<Config>>();
	private static Boolean cacheValid = false;
	
	public static ArrayList<Config> getProperty(String key) {
		if (!cacheValid) {
			loadProperties();
		}
		if (cachedProperties.containsKey(key)) {
			return cachedProperties.get(key);
		} else {
			return null;
		}
	}
	
	public static void clear(){
		cacheValid = false;
		loadProperties();
	}

	public static Config getSingleProperty(String key) {
		if (!cacheValid) {
			loadProperties();
		}
		if (cachedProperties.containsKey(key)) {
			return cachedProperties.get(key).get(0);
		} else {
			return null;
		}
	}
	
	public static java.sql.Timestamp getTimestamp(String key) {
		if (!cacheValid) {
			loadProperties();
		}
		if (cachedProperties.containsKey(key)) {
			return java.sql.Timestamp.valueOf( cachedProperties.get(key).get(0).getValue());
		} else {
			return null;
		}
	}

	public static String setProperty(String key, String value, String publicFlag) {
		try {
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("propertySet"), key, value,
					new java.sql.Date(System.currentTimeMillis()),
					publicFlag.equals("t") ? true : false);

			if (stmt.executeUpdate()) {
				cacheValid = false;
				return "Property " + key + " has been set to " + value;
			} else {
				return "Failure to set new property, please check the logs.";
			}
			
		} catch (SQLException e) {
			logger.error("Exception attempting to set property", e);
		}

		return "There was an error during the update process.  Please check the logs.";
	}

	public static String updateSingle(String key, String value,
			String publicFlag) {
		try {
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("updateCheck"), key);
			ResultSet rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				if (rs.getInt("counted") > 1) {
					stmt.close();
					return "That property has multiple values.  Please contact Dr. Magnus to have it modified.";
				} else if (rs.getInt("counted") < 1) {
					stmt.close();
					return "That property currently is not set.  This operation doesn't support insertion.";
				} else {
					stmt.close();
					CloseableStatement updateStatement = Connector
							.getStatement(Queries.getQuery("updatePush"), value,
									publicFlag.equals("t") ? true : false, key);

					if (updateStatement.executeUpdate()) {
						cacheValid = false;
						return "Updated " + key + " to value " + value;
					} else {
						return "I'm sorry, there was an error updating the key specified.";
					}
				}
			} else {
				logger.error("Exception attempting to set property.  The returned result set had no values.");
			}

			stmt.close();
		} catch (SQLException e) {
			logger.error("Exception attempting to set property", e);

		}

		return "There was an error during the update process.  Please check the logs.";
	}

	public static String removeProperty(String key, String value) {
		boolean okayToDelete = false;
		if (cachedProperties.containsKey(key)) {
			ArrayList<Config> configs = cachedProperties.get(key);
			for (Config c : configs) {
				if (c.getValue().equals(value)) {
					okayToDelete = c.isPublic();
				}
			}
		}
		if (okayToDelete) {
			try {
				CloseableStatement stmt = Connector.getStatement(Queries.getQuery("deleteConfig"),
						key, value);
				if (stmt.executeUpdate()) {
					cacheValid = false;
					return "Successfully removed " + key + " with the value "
							+ value + " from the properties table.";
				} else {
					return "There was an error removing the specified key/value pair.";
				}

			} catch (Exception e) {
				logger.error("Exception deleting property.", e);
			}
		} else {
			return "Apologies, this property is either not currently configured, or is not publically accessible.";
		}

		return "There was an unexpected error attempting to delete property.";

	}

	private static void loadProperties() {
			cachedProperties = new HashMap<String, ArrayList<Config>>();
			try {
				CloseableStatement stmt = Connector.getStatement(Queries.getQuery("kvQuery"));
				ResultSet rs = stmt.executeQuery();
				while (rs != null && rs.next()) {
					if (!cachedProperties.containsKey(rs.getString("key"))) {
						cachedProperties.put(rs.getString("key"),
								new ArrayList<Config>());
					}
					cachedProperties.get(rs.getString("key")).add(
							new Config(rs.getString("key"), rs
									.getString("value"), rs
									.getString("updated"), rs
									.getBoolean("public")));
				}
				cacheValid = true;
				stmt.close();
			} catch (SQLException e) {
				logger.error(
						"Exception attempting to retreive properties list", e);
			}
		
	}

	public static ArrayList<Config> getConfiguredProperties(boolean showPublic) {
		ArrayList<Config> keyValues = new ArrayList<Config>();
		if (!cacheValid) {
			loadProperties();
		} 

			for (String key : cachedProperties.keySet()) {
				for (Config value : cachedProperties.get(key)) {
					if (showPublic) {
						if (value.isPublic()) {
							keyValues.add(value);
						}
					} else {
						keyValues.add(value);
					}
				}
			}

			return keyValues;
	}

	public static ArrayList<String> getPropertyTypes() {
		if(!cacheValid){
			loadProperties();
		}
		ArrayList<String> keyValues = new ArrayList<String>();
		try {
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("keysQuery"));

			ResultSet rs = stmt.executeQuery();
			while (rs != null && rs.next()) {
				keyValues.add(rs.getString("key"));
			}
			stmt.close();
		} catch (SQLException e) {
			logger.error("Exception attempting to retreive properties list", e);
		} 

		return keyValues;

	}

}
