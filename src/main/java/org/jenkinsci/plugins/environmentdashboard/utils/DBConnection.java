package org.jenkinsci.plugins.environmentdashboard.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

/**
 * Singleton class to represent a single DB connection.
 * 
 * @author robertnorthard
 * @date 18/10/2014, 01/03/2015
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static Connection con = null;

    /**
     * Added explicit private constructor as this is a utility class.
     */
    private DBConnection() {

    }

    /**
     * Return a database connection object.
     * 
     * @return a database connection object
     */
    public static Connection getConnection() {

        // Generate connection String for DB driver.
        String dbConnectionString = "jdbc:h2:"
                + Jenkins.getInstance().root.toString() + File.separator
                + "jenkins_dashboard" + ";MVCC=true";

        // Load driver and connect to DB
        try {
            Class.forName("org.h2.Driver");
            DBConnection.con = DriverManager.getConnection(dbConnectionString);
        } catch (ClassNotFoundException e) {
            LOGGER.info("WARN: Could not acquire Class org.h2.Driver." + e);
        } catch (SQLException e) {
            LOGGER.info("WARN: Could not acquire connection to H2 DB." + e);
        }
        return con;
    }

    /**
     * Close Database Connection
     * 
     * @return true if database connection closed successful, else false if
     *         connection not closed or SQLException.
     */
    public static boolean closeConnection() {

        // Prevent unchecked NullPointerException
        if (DBConnection.con != null) {
            try {
                DBConnection.con.close();
                return true;
            } catch (SQLException e) {
                LOGGER.info("E5: " + e);
                return false;
            }
        }
        // default - failed to close
        return false;
    }
}
