package org.jenkinsci.plugins.environmentdashboard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.jenkinsci.plugins.environmentdashboard.entity.Build;
import org.jenkinsci.plugins.environmentdashboard.utils.DBConnection;

/**
 * This class is a data access object that handles interactions between the
 * application and the db.
 * 
 * @author robert.northard
 * @version 01/03/2015 
 */
public class DashboardDAO {

    // Create dashboard query
    private static String createDashboardTblQuery = "CREATE TABLE IF NOT EXISTS env_dashboard (envComp VARCHAR(255), jobUrl VARCHAR(255), "
            + "buildNum VARCHAR(255), buildStatus VARCHAR(255), envName VARCHAR(255), compName "
            + "VARCHAR(255), created_at TIMESTAMP,  buildJobUrl VARCHAR(255), packageName VARCHAR(255));";

    // Delete old build query
    private static String deleteOldBuildQuery = "DELETE FROM env_dashboard where created_at <= current_timestamp - ";

    // Update build query
    private static String updateBuildQuery = "UPDATE env_dashboard SET buildStatus = ?, created_at = CURRENT_TIMESTAMP WHERE "
            + "envComp = ? AND joburl = ?;";

    // Truncate table - delete old tuples in env_dashboard
    private static String truncateEnvDashbord = "TRUNCATE TABLE env_dashboard;";
    
    // Add Column - add column to db.
    private static String alterColumnQuery = "ALTER TABLE env_dashboard ADD IF NOT EXISTS ";
    
    // Delete Build query
    private static String deleteBuildQuery = "DELETE FROM env_dashboard where envComp = ? AND joburl = ?;";

    /**
     * Create dashboard table
     * 
     * @return true if table created, else false.
     * @exception SQLException
     *                unable to execute create dashboard query.
     */
    public boolean createDashboardTable() throws SQLException {

        // Get DB connection
        Connection conn = DBConnection.getConnection();
        return conn.prepareStatement(DashboardDAO.createDashboardTblQuery)
                .execute();
    }
    
    /**
     * add column to db table.
     * @param the column to add
     * @return true if added, else false.
     * @exception SQLException
     *                unable to execute add column query.
     * 
     */
    public boolean addColumn(String column) throws SQLException{
        // Get DB Connection
        Connection conn = DBConnection.getConnection();

        /* Can't use prepared statement for modifying a table as '?' are
         * bound to data items only.
         * Using temp variable as the query has a static modifier. 
         */
        String temp = DashboardDAO.alterColumnQuery + column + ";";
        
        Statement stat = conn.createStatement();
        boolean result = stat.execute(temp);
        stat.close();
        DBConnection.closeConnection();

        return result;
    }

    /**
     * Add build to environment dashboard.
     * 
     * @param index
     *            the primary key in the env_dashboard table.
     * @param build
     *            the build being added to the dashboard.
     * @param componant
     *            the component being deployed/built
     * @param buildJobUrl
     *            the URL of the build job.
     * @return true if build added else false.
     * @throws SQLException
     *             unable to execute insert build query.
     */
    public boolean addBuild(String index, Build build) throws SQLException {
        
        // Get DB Connection
        Connection conn = DBConnection.getConnection();

        // Refactor this somewhere.
        String insertBuildQuery = "INSERT INTO env_dashboard VALUES( '" 
                    + index + "', '" + build.getUrl() + "', '" +  build.getId() + "', '" 
                    + build.getResult() + "', '" + build.getEnvironment() + "', '" 
                    + build.getComponantName() + "' ,current_timestamp, '" + build.getUrl() + "' , '" 
                    + build.getPackages() +  "');";
        
        Statement stat = conn.createStatement();
        boolean result = stat.execute(insertBuildQuery);
        
        stat.close();
        DBConnection.closeConnection();

        return result;
    }

    /**
     * Update an existing logged build
     * 
     * @param envComp
     *            the environment component key.
     * @param build
     *            the build being added to the dashboard
     * @return true if build updated else false
     * @throws SQLException
     *             unable to execute update build query.
     */
    public boolean updateBuild(String envComp, Build b) throws SQLException {
        // Get DB Connection
        Connection conn = DBConnection.getConnection();
        PreparedStatement stat = conn
                .prepareStatement(DashboardDAO.updateBuildQuery);

        // Populate prepared statement.
        DashboardDAO.setValues(stat, b.getResult(), envComp, b.getUrl());

        boolean result = stat.execute();
        DBConnection.closeConnection();

        return result;
    }

    /**
     * Delete build older then specified days.
     * 
     * @param daysOld
     *            delete build in the last days i.e. 30
     * @return - true if builds deleted else false.
     * @throws SQLException
     *             unable to execute delete build query.
     */
    public boolean deleteBuilds(Integer daysOld) throws SQLException {

        boolean result = false;

        if (daysOld > 0) {
            // Get DB Connection
            Connection conn = DBConnection.getConnection();
            PreparedStatement stat = conn
                    .prepareStatement(DashboardDAO.deleteOldBuildQuery
                            + daysOld);

            result = stat.execute();
            DBConnection.closeConnection();
        }else{
            return this.truncateEnvDashboard();
        }

        return result;
    }
    
    /**
     * Delete builds.
     * 
     * @param indexValueOfTable
     * @param Build - build to delete.
     * @return - true if builds deleted else false.
     * @throws SQLException
     *             unable to execute delete build query.
     */
    public boolean deleteBuild(String indexValueofTable, Build b) throws SQLException{
      
        // Get DB Connection
        Connection conn = DBConnection.getConnection();
        PreparedStatement stat = conn
                .prepareStatement(DashboardDAO.deleteBuildQuery);

        // Populate prepared statement.
        DashboardDAO.setValues(stat, indexValueofTable, b.getUrl());

        boolean result = stat.execute();
        DBConnection.closeConnection();

        return result;
    }

    /**
     * Truncate/delete all tuples in env_dashboard table
     * 
     * @return true if truncated else false
     * @throws SQLException
     *             if unable to execute truncate query
     */
    public boolean truncateEnvDashboard() throws SQLException {
        boolean result = false;

        // Get DB Connection
        Connection conn = DBConnection.getConnection();
        PreparedStatement stat = conn
                .prepareStatement(DashboardDAO.truncateEnvDashbord);

        result = stat.execute();
        DBConnection.closeConnection();

        return result;
    }

    /**
     * Utility method to set values in a prepared statement.
     * 
     * @param preparedStatement
     *            the prepared statement/query
     * @param values
     *            the values to populate the prepared statement
     * @throws SQLException
     *             if parameterIndex does not correspond to a parameter marker
     *             in the SQL statement
     */
    public static void setValues(PreparedStatement preparedStatement,
            Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            preparedStatement.setObject(i + 1, values[i]);
        }
    }

}