package org.jenkinsci.plugins.environmentdashboard;

import hudson.model.*;
import hudson.Extension;
import java.util.*;
import java.io.File;
import java.io.IOException;
import net.sf.json.JSONObject;
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
            System.out.println("E2");
        }
        try {
            assert conn != null;
            stat = conn.createStatement();
        } catch (SQLException e) {
            System.out.println("E3");
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
                System.out.println("E8");
                return null;
            }
        }
        return orderOfComps;
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

    public String getCurrentStatus(String envCompArg){
         return getField(envCompArg, "buildstatus");
    }

    public String getCurrentUrl(String envCompArg){
        return getField(envCompArg, "joburl");
    }

    public String getCurrentNum(String envCompArg){
        return getField(envCompArg, "buildnum");
    }

    public String getCurrentTimestamp(String envCompArg){
        return getField(envCompArg, "created_at").substring(0,19);
    }

    public String getBuildUrl(String envCompArg){
        String buildJobUrl = getField(envCompArg, "buildJobUrl");

        if (buildJobUrl.isEmpty()) {
            return getCurrentUrl(envCompArg);

        } else {
            return buildJobUrl;
        }
    }


    public String getField(String envCompArg, String column) {
        String envField = "UNKNOWN";
        String queryString = "select top 1 " + column + " from env_dashboard where envcomp = '" + envCompArg + "' order by created_at desc";
        try {
            ResultSet rs = runQuery(queryString);
            while (rs.next()) {
                envField = rs.getString(column);
            }
            closeDBConnection();
        } catch (SQLException e) {
            System.out.println("E9");
            return "Error executing build " + column + " query!";
        }
        return envField;
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
