package com.scp.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class CloseableStatement {
	private Connection conn;
	private ResultSet rs = null;

	private PreparedStatement stmt;
	private static final Logger logger = Logger
			.getLogger(CloseableStatement.class);

	public CloseableStatement(PreparedStatement st, Connection conn) {
		this.conn = conn;
		this.stmt = st;
	}

	public CloseableStatement() {
		this.conn = null;
		this.stmt = null;
	}

	public void close() {
		try {
			if (!conn.isClosed()) {
				conn.close();
			}

			if ((rs != null) && !rs.isClosed()) {
				rs.close();
			}

		} catch (Exception e) {
			logger.error(
					"There was an exception trying to close a connection: ", e);
		}
	}

	public Boolean executeUpdate() throws SQLException {
		if (stmt != null) {
			Boolean result = stmt.executeUpdate() > 0;
			close();
			return result;
		} else {
			return false;
		}
	}

	public ResultSet getResultSet() throws SQLException {
		return executeQuery();
	}
	
	public ResultSet execute() throws SQLException {
		if(stmt != null){
			stmt.execute();
			return stmt.getResultSet();
		}
		return null;
	}

	public ResultSet executeQuery() throws SQLException {
		if (rs == null) {
			if (stmt != null) {
				rs = stmt.executeQuery();
			} else {
				rs = null;
			}
		}
		return rs;
	}
	
	public String toString(){
		return stmt.toString();
	}

}
