package org.jenkinsci.plugins.environmentdashboard;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import java.io.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import java.sql.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import javax.servlet.ServletException;

public class DashboardBuilder extends BuildWrapper {

    private final String nameOfEnv;
    private final String componentName;
    private final String buildNumber;
    private final String buildJob;

    @DataBoundConstructor
    public DashboardBuilder(String nameOfEnv, String componentName, String buildNumber, String buildJob) {
        this.nameOfEnv = nameOfEnv;
        this.componentName = componentName;
        this.buildNumber = buildNumber;
        this.buildJob = buildJob;
    }

    public String getNameOfEnv() {
        return nameOfEnv;
    }
    public String getComponentName() {
        return componentName;
    }
    public String getBuildNumber() {
        return buildNumber;
    }
    public String getBuildJob() {
        return buildJob;
    }

    @SuppressWarnings("rawtypes")
    @Override

    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // PreBuild
        String passedBuildNumber = build.getEnvironment(listener).expand(buildNumber);
        String passedEnvName = build.getEnvironment(listener).expand(nameOfEnv);
        String passedCompName = build.getEnvironment(listener).expand(componentName);
        String passedBuildJob = build.getEnvironment(listener).expand(buildJob);
        String returnComment = null;
        if (!(passedBuildNumber.matches("^\\s*$") || passedEnvName.matches("^\\s*$") || passedCompName.matches("^\\s*$"))) {
            returnComment = writeToDB(build, listener, passedEnvName, passedCompName, passedBuildNumber, "PRE", passedBuildJob);
            listener.getLogger().println("Pre-Build Update: " + returnComment);
        }
        // TearDown - This runs post all build steps
        class TearDownImpl extends Environment {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                String passedBuildNumber = build.getEnvironment(listener).expand(buildNumber);
                String passedEnvName = build.getEnvironment(listener).expand(nameOfEnv);
                String passedCompName = build.getEnvironment(listener).expand(componentName);
                String passedBuildJob = build.getEnvironment(listener).expand(buildJob);
                String returnComment = null;
                if (!(passedBuildNumber.matches("^\\s*$") || passedEnvName.matches("^\\s*$") || passedCompName.matches("^\\s*$"))) {
                    returnComment = writeToDB(build, listener, passedEnvName, passedCompName, passedBuildNumber, "POST", passedBuildJob);
                    listener.getLogger().println("Post-Build Update: " + returnComment);
                }
                return super.tearDown(build, listener);
            }
        }
        return new TearDownImpl();
    }

    @SuppressWarnings("rawtypes")
    private String writeToDB(AbstractBuild build, BuildListener listener, String envName, String compName, String bNumber, String runTime, String buildJob) {
        String returnComment = null;
        if (envName.matches("^\\s*$") || compName.matches("^\\s*$")) {
            returnComment = "WARN: Either Environment name or Component name is empty.";
            return returnComment;
        }
        String jenkinsHome = Hudson.getInstance().root.toString();
        String jenkinsDashboardDb = (jenkinsHome + File.separator + "jenkins_dashboard");
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            returnComment = "WARN: Could not acquire Class org.h2.Driver.";
            return returnComment;
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:h2:" + jenkinsDashboardDb);
        } catch (SQLException e) {
            returnComment = "WARN: Could not acquire connection to H2 DB.";
            return returnComment;
        }
        Statement stat = null;
        try {
            stat = conn.createStatement();
        } catch (SQLException e) {
            returnComment = "WARN: Could not execute statement.";
            return returnComment;
        }
        try {
            stat.execute("CREATE TABLE IF NOT EXISTS env_dashboard (envComp VARCHAR(255), jobUrl VARCHAR(255), buildNum INTEGER, buildStatus VARCHAR(255), envName VARCHAR(255), compName VARCHAR(255), created_at TIMESTAMP,  buildJobUrl VARCHAR(255));");
        } catch (SQLException e) {
            returnComment = "WARN: Could not create table env_dashboard.";
            return returnComment;
        }
        String indexValueofTable = envName + '=' + compName;
        String currentBuildResult = "UNKNOWN";
        if (build.getResult() == null && runTime.equals("PRE")) {
            currentBuildResult = "RUNNING";
        } else if (build.getResult() == null && runTime.equals("POST")) {
            currentBuildResult = "SUCCESS";
        }  else {
            currentBuildResult = build.getResult().toString();
        }
        String currentBuildUrl = build.getUrl();

        String buildJobUrl;
        //Build job is an optional configuration setting
        if (buildJob.isEmpty()) {
            buildJobUrl = "";
        } else {
            buildJobUrl = "job/" + buildJob + "/" + bNumber;
        }

		Integer currentBuild;
		try { 
			currentBuild = Integer.parseInt(bNumber); 
		} catch(NumberFormatException e) { 
			return "The build number provided is not an integer."; 
		}
        String runQuery = null;
        if (runTime.equals("PRE")) {
            runQuery = "INSERT INTO env_dashboard VALUES( '" + indexValueofTable + "', '" + currentBuildUrl + "', " + currentBuild + ", '" + currentBuildResult + "', '" + envName + "', '" + compName + "' , + current_timestamp, '" + buildJobUrl + "');";
        } else {
            if (runTime.equals("POST")) {
                runQuery = "UPDATE env_dashboard SET buildStatus = '" + currentBuildResult + "', created_at = current_timestamp WHERE envComp = '" + indexValueofTable +"' AND joburl = '" + currentBuildUrl + "'";
            }
        }
        try {
            stat.execute(runQuery);
        } catch (SQLException e) {
            returnComment = "Error running insert query " + runQuery + ".";
            return returnComment;
        }
        try {
            stat.close();
            conn.close();
        } catch (SQLException e) {
            returnComment = "Error closing connection.";
            return returnComment;
        }
        return "Updated Dashboard DB";
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        private String nameOfEnv;
        private String componentName;
        private String buildNumber;
        private String buildJob;

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Details for Environment dashboard";
        }

        public FormValidation doCheckNameOfEnv(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set an Environment name.");
            return FormValidation.ok();
        }

        public FormValidation doCheckComponentName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Component name.");
            return FormValidation.ok();
        }

        public FormValidation doCheckBuildNumber(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the Build variable e.g: ${BUILD_NUMBER}.");
            return FormValidation.ok();
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            nameOfEnv = formData.getString("nameOfEnv");
            componentName = formData.getString("componentName");
            buildNumber = formData.getString("buildNumber");
            buildJob = formData.getString("buildJob");
            save();
            return super.configure(req,formData);
        }

    }
}
