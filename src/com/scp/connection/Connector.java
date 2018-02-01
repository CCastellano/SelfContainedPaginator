package com.scp.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

public class Connector {

	private final static Logger logger = Logger.getLogger(Connector.class);

	public static Connection getConnection() {
		try {
			return DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1/helen_db", "helen_bot",
					"helenrevolver");
		} catch (SQLException e) {
			return null;
		}
	}

	public static CloseableStatement getStatement(String queryString) {
		try {
			Connection conn = getConnection();
			return new CloseableStatement(conn.prepareStatement(queryString),
					conn);
		} catch (Exception e) {
			// TODO exception text
		}
		return new CloseableStatement();
	}

	public static CloseableStatement getStatement(String queryString,
			Object... args) {
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(queryString);
			int i = 1;
			if (!(args.length == 1 && args[0] == null)) {
				for (Object o: args) {
					if (o instanceof String) {
						stmt.setString(i, (String) o);
					} else if (o instanceof Integer) {
						stmt.setInt(i, (Integer) o);
					} else if (o instanceof java.sql.Timestamp) {
						stmt.setTimestamp(i, (java.sql.Timestamp) o);
					} else if (o instanceof Boolean) {
						stmt.setBoolean(i, (Boolean) o);
					} else if (o instanceof Date) {
						stmt.setDate(i, new java.sql.Date(((Date) o).getTime()));
					} else {
						logger.error("Unknown object type: " + o.toString());
					}
					i++;
				}
			}
			return new CloseableStatement(stmt, conn);
		} catch (Exception e) {
			logger.error("Error constructing statement.", e);
			e.printStackTrace();
		}
		return null;
	}
	
	public static CloseableStatement getArrayStatement(String queryString,
			String[] args) {
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(queryString);
			stmt.setArray(1, conn.createArrayOf("text", (Object[])args));
			return new CloseableStatement(stmt, conn);
		} catch (Exception e) {
			logger.error("Error constructing statement.", e);
		}
		return null;
	}
}
