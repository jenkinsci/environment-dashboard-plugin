package org.jenkinsci.plugins.environmentdashboard;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.View;
import hudson.model.ViewDescriptor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.environmentdashboard.utils.DBConnection;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Class to provide build wrapper for Dashboard.
 * @author vipin
 * @date 15/10/2014
 */
public class EnvDashboardView extends View {

    private String envOrder = null;

    private String compOrder = null;

    private String deployHistory = null;

    @DataBoundConstructor
    public EnvDashboardView(final String name, final String envOrder, final String compOrder, final String deployHistory) {
        super(name, Hudson.getInstance());
        this.envOrder = envOrder;
        this.compOrder = compOrder;
        this.deployHistory = deployHistory;
    }

    @Override
    protected void submit(final StaplerRequest req) throws IOException, ServletException, FormException {
        req.bindJSON(this, req.getSubmittedForm());
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        return Hudson.getInstance().doCreateItem(req, rsp);
    }

    @Extension
    public static final class DescriptorImpl extends ViewDescriptor {

        private String envOrder;
        private String compOrder;
        private String deployHistory;

        /**
         * descriptor impl constructor This empty constructor is required for stapler. If you remove this constructor, text name of
         * "Build Pipeline View" will be not displayed in the "NewView" page
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * get the display name
         *
         * @return display name
         */
        @Override
        public String getDisplayName() {
            return "Environment Dashboard";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            envOrder = formData.getString("envOrder");
            compOrder = formData.getString("compOrder");
            deployHistory = formData.getString("deployHistory");
            save();
            return super.configure(req,formData);
        }
    }

    Connection conn = null;
    Statement stat = null;

    public ArrayList<String> splitEnvOrder(String envOrder) {
        ArrayList<String> orderOfEnvs = new ArrayList<String>();
        if (! "".equals(envOrder)) {
            orderOfEnvs = new ArrayList<String>(Arrays.asList(envOrder.split("\\s*,\\s*")));
        }
        return orderOfEnvs;
    }

    public ArrayList<String> splitCompOrder(String compOrder) {
        ArrayList<String> orderOfComps = new ArrayList<String>();
        if (! "".equals(compOrder)) {
            orderOfComps = new ArrayList<String>(Arrays.asList(compOrder.split("\\s*,\\s*")));
        }
        return orderOfComps;
    }

    public ResultSet runQuery(String queryString) {

        ResultSet rs = null;
        
        //Get DB connection
        conn = DBConnection.getConnection();
        
        try {
            assert conn != null;
            stat = conn.createStatement();
        } catch (SQLException e) {
            System.out.println("E3" + e.getMessage());
        }
        try {
            assert stat != null;
            rs = stat.executeQuery(queryString);
        } catch (SQLException e) {
            System.out.println("E4" + e.getMessage());
        }
        return rs;
    }

    public ArrayList<String> getOrderOfEnvs() {
        ArrayList<String> orderOfEnvs;
        orderOfEnvs = splitEnvOrder(envOrder);
        if (orderOfEnvs == null || orderOfEnvs.isEmpty()){
            String queryString="select distinct envname from env_dashboard order by envname;";
            try {
                ResultSet rs = runQuery(queryString);
                if (rs == null ) {
                    return null;
                }
                while (rs.next()) {
                    if (orderOfEnvs != null) {
                        orderOfEnvs.add(rs.getString("envName"));
                    }
                }
                DBConnection.closeConnection();
            } catch (SQLException e) {
                System.out.println("E6" + e.getMessage());
                return null;
            }
        }
        return orderOfEnvs;
    }

    public ArrayList<String> getOrderOfComps() {
        ArrayList<String> orderOfComps;
        orderOfComps = splitCompOrder(compOrder);
        if (orderOfComps == null || orderOfComps.isEmpty()){
            String queryString="select distinct compname from env_dashboard order by compname;";
            try {
                ResultSet rs = runQuery(queryString);
                while (rs.next()) {
                    if (orderOfComps != null) {
                        orderOfComps.add(rs.getString("compName"));
                    }
                }
                DBConnection.closeConnection();
            } catch (SQLException e) {
                System.out.println("E8" + e.getMessage());
                return null;
            }
        }
        return orderOfComps;
    }

    public Integer getLimitDeployHistory() {
        Integer lastDeploy;
        if ( deployHistory == null || deployHistory.equals("") ) {
            return 10;
        } else {
            try {
                lastDeploy = Integer.parseInt(deployHistory);
            } catch (NumberFormatException e) {
                return 10;
            }
        }
        return lastDeploy;
    }

    public ArrayList<String> getDeployments(String env, Integer lastDeploy) {
        if ( lastDeploy <= 0 ) {
            lastDeploy = 10;
        }
        ArrayList<String> deployments;
        deployments = new ArrayList<String>();
        String queryString="select top " + lastDeploy + " created_at from env_dashboard where envName ='" + env + "' order by created_at desc;";
            try {
                ResultSet rs = runQuery(queryString);
                while (rs.next()) {
                    deployments.add(rs.getString("created_at"));
                }
                DBConnection.closeConnection();
            } catch (SQLException e) {
                System.out.println("E11" + e.getMessage());
                return null;
            }
        return deployments;
    }

    public String anyJobsConfigured() {
        ArrayList<String> orderOfEnvs;
        orderOfEnvs = getOrderOfEnvs();
        if (orderOfEnvs == null || orderOfEnvs.isEmpty()){
            return "NONE";
        } else {
            return "ENVS";
        }
    }

    public String getNiceTimeStamp(String timeStamp) {
        return timeStamp.substring(0,19);
    }

    public HashMap getCompDeployed(String env, String time) {
        HashMap<String, String> deployment;
        deployment = new HashMap<String, String>();
        String[] fields = {"buildstatus", "compName", "buildJobUrl", "jobUrl", "buildNum", "packageName"};
        String queryString = "select " + StringUtils.join(fields, ", ").replace(".$","") + " from env_dashboard where envName = '" + env + "' and created_at = '" + time + "';";
        try {
            ResultSet rs = runQuery(queryString);
            rs.next();
            for (String field : fields) {
                deployment.put(field, rs.getString(field));
            }
            DBConnection.closeConnection();
        } catch (SQLException e) {
            System.out.println("E10" + e.getMessage());
            System.out.println("Error executing: " + queryString);
        }
        return deployment;
    }

    public HashMap getCompLastDeployed(String env, String comp) {
        HashMap<String, String> deployment;
        deployment = new HashMap<String, String>();
        String[] fields = {"buildstatus", "buildJobUrl", "jobUrl", "buildNum", "created_at", "packageName"};
        String queryString = "select top 1 " + StringUtils.join(fields, ", ").replace(".$","") + " from env_dashboard where envName = '" + env + "' and compName = '" + comp + "' order by created_at desc;";
        try {
            ResultSet rs = runQuery(queryString);
            rs.next();
            for (String field : fields) {
                deployment.put(field, rs.getString(field));
            }
            DBConnection.closeConnection();
        } catch (SQLException e) {
            if (e.getErrorCode() == 2000) {
                //We'll assume this comp has never been deployed to this env            }
            } else {
                System.out.println("E12" + e.getMessage());
                System.out.println("Error executing: " + queryString);
            }
        }
        return deployment;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return null;
    }

    public String getEnvOrder() {
        return envOrder;
    }

    public void setEnvOrder(final String envOrder) {
        this.envOrder = envOrder;
    }

    public String getCompOrder() {
        return compOrder;
    }

    public void setCompOrder(final String compOrder) {
        this.compOrder = compOrder;
    }

    public String getDeployHistory() {
        return deployHistory;
    }

    public void setDeployHistory(final String deployHistory) {
        this.deployHistory = deployHistory;
    }

    @Override
    public boolean contains(TopLevelItem topLevelItem) {
        return false;
    }

    @Override
    public void onJobRenamed(Item item, String s, String s2) {

    }

}
