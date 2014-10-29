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

    private int refreshInterval = 10;

    @DataBoundConstructor
    public EnvDashboardView(final String name, final String envOrder, final String compOrder, final int refreshInterval) {
        super(name, Hudson.getInstance());
        this.envOrder = envOrder;
        this.compOrder = compOrder;
        if (refreshInterval < 10) {
            this.refreshInterval = 10;
        } else {
            this.refreshInterval = refreshInterval;
        }
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

    @JavaScriptMethod
    public String writeHTML() {

        ArrayList<String> newOrderOfEnvs;
        ArrayList<String> newOrderOfComps;

        newOrderOfEnvs = getOrderOfEnvs();

        String finalHTML;

        if (newOrderOfEnvs == null || newOrderOfEnvs.isEmpty()) {
            finalHTML = "<table id=\"envDashboard\"><tr><div class=\"jumbotron\"><h1>Hi, there!</h1><p>You possible haven't set up any jobs to use the Dashboard. Configure the jobs by using the 'Details for Environment Dashboard' checkbox.</p></div></tr></table>";
            return finalHTML;
        }

        newOrderOfComps = getOrderOfComps();

        finalHTML = "        <table id=\"envDashboard\" class=\"table table-bordered\"><tbody>" +
                "            <tr>" +
                "            <td></td>";
        for (String head : newOrderOfEnvs) {
            if (!(head == null || head.matches("^\\s*$"))) {
                finalHTML = finalHTML + "<th><div align=\"center\">" + head + "</div></th>";
            }
        }
        finalHTML = finalHTML + "</tr>";
        for (String comps : newOrderOfComps) {
            if (comps == null || comps.matches("^\\s*$")) {
                continue;
            }
            finalHTML = finalHTML + "<tr><td><div align=\"center\"><strong>" + comps + "</strong></div></td>";
            for (String envs : newOrderOfEnvs) {
                if (envs == null || envs.matches("^\\s*$")) {
                    continue;
                }
                String finalEnvComp = envs + "=" + comps;
                String finalStatus = getCurrentStatus(finalEnvComp);
                String finalUrl = getCurrentUrl(finalEnvComp);
                String finalNumber = getCurrentNum(finalEnvComp);
                if (finalStatus.equals("SUCCESS")) {
                    finalHTML = finalHTML + "<td style=\"background-color:#d9edf7; color: #31708f\"><div><a href=\"" + finalUrl + "\" style=\"text-decoration: none\"><div align=\"center\" style=\"font-size:15px;\"><strong>" + finalNumber + "</strong>&nbsp; &nbsp;<i class=\"glyphicon glyphicon-ok\" style=\"font-size:20px;\"></i></div></a></div></td>";
                } else if (finalStatus.equals("RUNNING")) {
                    finalHTML = finalHTML + "<td style=\"background-color:#dff0d8; color: #3c763d\"><div><a href=\"" + finalUrl + "\" style=\"text-decoration: none\"><div align=\"center\" style=\"font-size:15px;\"><strong>" + finalNumber + "</strong>&nbsp; &nbsp;<i class=\"glyphicon glyphicon-play\" style=\"font-size:20px;\"></i></div></a></div></td>";
                } else if (finalStatus.equals("FAILURE")) {
                    finalHTML = finalHTML + "<td style=\"background-color:#f2dede; color: #a94442\"><div><a href=\"" + finalUrl + "\" style=\"text-decoration: none\"><div align=\"center\" style=\"font-size:15px;\"><strong>" + finalNumber + "</strong>&nbsp; &nbsp;<i class=\"glyphicon glyphicon-remove\" style=\"font-size:20px;\"></i></div></a></div></td>";
                } else {
                    finalHTML = finalHTML + "<td style=\"background-color:#332C2F; color: #FFFFFF\"><div><div align=\"center\" style=\"font-size:15px;\">No deploys</div></div></td>";
                }
            }
            finalHTML = finalHTML + "</tr>";
        }
        finalHTML = finalHTML + "</tbody></table>";
        return finalHTML;
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
        String envCompBuildStatus = "UNKNOWN";
        String queryString = "SELECT ed.buildstatus AS status FROM env_dashboard ed INNER JOIN (SELECT envcomp, MAX(created_at) AS MaxDateTime FROM env_dashboard GROUP BY envcomp) groupeded ON ed.envcomp =  groupeded.envcomp AND ed.created_at = groupeded.MaxDateTime AND ed.envcomp = '" + envCompArg + "';";
        try {
            ResultSet rs = runQuery(queryString);
            while (rs.next()) {
                envCompBuildStatus = rs.getString("status");
            }
            closeDBConnection();
        } catch (SQLException e) {
            System.out.println("E9");
            return "Error executing build status query!";
        }
        return envCompBuildStatus;
    }

    public String getCurrentUrl(String envCompArg){
        String envCompBuildUrl = "UNKNOWN";
        String queryString = "SELECT ed.joburl AS url FROM env_dashboard ed INNER JOIN (SELECT envcomp, MAX(created_at) AS MaxDateTime FROM env_dashboard GROUP BY envcomp) groupeded ON ed.envcomp =  groupeded.envcomp AND ed.created_at = groupeded.MaxDateTime AND ed.envcomp = '" + envCompArg + "';";
        try {
            ResultSet rs = runQuery(queryString);
            while (rs.next()) {
                envCompBuildUrl = rs.getString("url");
            }
            closeDBConnection();
        } catch (SQLException e) {
            System.out.println("E10");
            return "Error executing build url query!";
        }
        return envCompBuildUrl;
    }

    public String getCurrentNum(String envCompArg){
        String envCompBuildNumber = "UNKNOWN";
        String queryString = "SELECT ed.buildnum AS number FROM env_dashboard ed INNER JOIN (SELECT envcomp, MAX(created_at) AS MaxDateTime FROM env_dashboard GROUP BY envcomp) groupeded ON ed.envcomp =  groupeded.envcomp AND ed.created_at = groupeded.MaxDateTime AND ed.envcomp = '" + envCompArg + "';";
        try {
            ResultSet rs = runQuery(queryString);
            while (rs.next()) {
                envCompBuildNumber = rs.getString("number");
            }
            closeDBConnection();
        } catch (SQLException e) {
            System.out.println("E11");
            return "Error executing build number query!";
        }
        return envCompBuildNumber;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return null;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(final int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public int getRefreshFrequencyInMillis() {
        return refreshInterval * 1000;
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
