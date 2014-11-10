package org.jenkinsci.plugins.environmentdashboard;

import hudson.model.*;
import hudson.Extension;
import java.util.*;
import java.io.File;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import java.sql.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import javax.servlet.ServletException;
import hudson.model.Descriptor.FormException;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Created by v.raveendran.nair on 15/10/2014.
 */
public class EnvDashboardView extends View {

    private String envOrder = null;

    private String compOrder = null;

    private boolean showHeader = false;


    @DataBoundConstructor
    public EnvDashboardView(final String name, final String envOrder, final String compOrder, final boolean showHeader) {
        super(name, Hudson.getInstance());
        this.envOrder = envOrder;
        this.compOrder = compOrder;
        this.showHeader = showHeader;
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

    String jenkinsHome = Hudson.getInstance().root.toString();
    String jenkinsDashboardDb = (jenkinsHome + File.separator + "jenkins_dashboard");

    public ResultSet runQuery(String queryString) {

        ResultSet rs = null;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("E1");
        }
        try {
            conn = DriverManager.getConnection("jdbc:h2:" + jenkinsDashboardDb);
        } catch (SQLException e) {
            System.out.println("E2" + e.getMessage());
        }
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

    public void closeDBConnection() {
        try {
            stat.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("E5");
        }
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
                closeDBConnection();
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
                closeDBConnection();
            } catch (SQLException e) {
                System.out.println("E8" + e.getMessage());
                return null;
            }
        }
        return orderOfComps;
    }

    public ArrayList<String> getDeployments(String env) {
        ArrayList<String> deployments;
        deployments = new ArrayList<String>();
        String queryString="select created_at from env_dashboard where envName ='" + env + "' order by created_at desc;";
            try {
                ResultSet rs = runQuery(queryString);
                while (rs.next()) {
                    deployments.add(rs.getString("created_at"));
                }
                closeDBConnection();
            } catch (SQLException e) {
                System.out.println("E11" + e.getMessage());
                return null;
            }
        return deployments;

    }

    public String getenvComps(String env, String comp) {
        return env + "=" + comp;
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
        String[] fields = {"buildstatus", "compName", "buildJobUrl", "jobUrl", "buildNum"};
        String queryString = "select " + StringUtils.join(fields, ", ").replace(".$","") + " from env_dashboard where envName = '" + env + "' and created_at = '" + time + "';";
        try {
            ResultSet rs = runQuery(queryString);
            rs.next();
            for (String field : fields) {
                deployment.put(field, rs.getString(field));
            }
            closeDBConnection();
        } catch (SQLException e) {
            System.out.println("E10" + e.getMessage());
            System.out.println("Error executing: " + queryString);
        }
        return deployment;
    }

    public HashMap getCompLastDeployed(String env, String comp) {
        HashMap<String, String> deployment;
        deployment = new HashMap<String, String>();
        String[] fields = {"buildstatus", "buildJobUrl", "jobUrl", "buildNum", "created_at"};
        String queryString = "select top 1 " + StringUtils.join(fields, ", ").replace(".$","") + " from env_dashboard where envName = '" + env + "' and compName = '" + comp + "' order by created_at desc;";
        try {
            ResultSet rs = runQuery(queryString);
            rs.next();
            for (String field : fields) {
                deployment.put(field, rs.getString(field));
            }
            closeDBConnection();
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


    public String getStatusChar(String statusWord) {

        String statusChar;

        if ("SUCCESS".equals(statusWord)) {
            statusChar = "<span title=\"" + statusWord  + "\" style=\"color:green;\">&#10004;</font>";
        } else if ("FAILURE".equals(statusWord)) {
            statusChar = "<span title=\"\" + statusWord  + \"\" style=\"color:darkred;\">&#x2716;</font>";
        } else if ("RUNNING".equals(statusWord)) {
            statusChar = "<span title=\"\" + statusWord  + \"\" style=\"color:blue;\">&#9658;</font>";
        } else {
            statusChar = "?";
        }

        return statusChar;
    }


        @Override
    public Collection<TopLevelItem> getItems() {
        return null;
    }

    public boolean getShowHeader() {
        return showHeader;
    }

    public void setShowHeader(final boolean showHeader) {
        this.showHeader = showHeader;
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

    @Override
    public boolean contains(TopLevelItem topLevelItem) {
        return false;
    }

    @Override
    public void onJobRenamed(Item item, String s, String s2) {

    }

}
