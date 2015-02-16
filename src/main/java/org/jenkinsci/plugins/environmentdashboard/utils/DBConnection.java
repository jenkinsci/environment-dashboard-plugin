package org.jenkinsci.plugins.environmentdashboard.utils;

import hudson.model.Hudson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.io.File;

/**
 * Singleton class to represent a single DB connection. 
 * @author robertnorthard
 * @date 18/10/2014
 */
public class DBConnection {

	private static Connection con = null;
	
	/**
	 * Return a database connection object.
	 * @return a database connection object
	 */
	public static Connection getConnection(){
		
		// Generate connection String for DB driver.
		String dbConnectionString = "jdbc:h2:" + Hudson.getInstance().root.toString() +
									 File.separator + "jenkins_dashboard" + ";MVCC=true";
		
		//Load driver and connect to DB
		try { 
			Class.forName("org.h2.Driver");
			DBConnection.con = DriverManager.getConnection(dbConnectionString);
		} catch (ClassNotFoundException e) {
			System.err.println("WARN: Could not acquire Class org.h2.Driver.");
		} catch (SQLException e){
			System.err.println("WARN: Could not acquire connection to H2 DB.");
		}
		return con;
	}
	
	/**
	 * Close Database Connection
	 * @return true if database connection closed successful,
	 * 			 else false if connection not closed or SQLException.
	 */
	public static boolean closeConnection(){
		
		//Prevent unchecked NullPointerException
		if(DBConnection.con != null){
			try {
				DBConnection.con.close();
				return true;
			} catch (SQLException error) { System.err.println("E5"); return false; }
		}
		//default  - failed to close
		return false;
	}
}
	
