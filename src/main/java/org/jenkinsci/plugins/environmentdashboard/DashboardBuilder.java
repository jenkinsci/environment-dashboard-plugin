package org.jenkinsci.plugins.environmentdashboard;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;

import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildWrapper;

import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;

import org.jenkinsci.plugins.environmentdashboard.utils.DBConnection;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Class to create Dashboard view.
 * 
 * author vipin
 * date 15/10/2014
 */
public class DashboardBuilder extends SimpleBuildWrapper implements Serializable {

	private final String nameOfEnv;
	private final String componentName;
	private final String buildNumber;
	private final String buildJob;
	private final String packageName;
	private ArrayList<ListItem> data = new ArrayList<>();
	public boolean addColumns = false;
        static final long serialVersionUID = 42L;

	@DataBoundConstructor
	public DashboardBuilder(String nameOfEnv, String componentName, String buildNumber, String buildJob,
			String packageName, boolean addColumns, ArrayList<ListItem> data) {
		this.nameOfEnv = nameOfEnv;
		this.componentName = componentName;
		this.buildNumber = buildNumber;
		this.buildJob = buildJob;
		this.packageName = packageName;
		if (addColumns) {
			this.addColumns = addColumns;
		} else {
			this.addColumns = false;
		}
		// this.data = Collections.emptyList();
		if (this.addColumns) {
			for (ListItem i : data) {
				if (!i.getColumnName().isEmpty()) {
					this.data.add(i);
				}
			}
		}
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

	public String getPackageName() {
		return packageName;
	}

	public List<ListItem> getData() {
		return data;
	}

	@SuppressWarnings("rawtypes")
        @Override
        public void setUp(SimpleBuildWrapper.Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
		// PreBuild
		final Integer numberOfDays = ((getDescriptor().getNumberOfDays() == null) ? 30
				: getDescriptor().getNumberOfDays());
		String passedBuildNumber = build.getEnvironment(listener).expand(buildNumber);
		String passedEnvName = build.getEnvironment(listener).expand(nameOfEnv);
		String passedCompName = build.getEnvironment(listener).expand(componentName);
		String passedBuildJob = build.getEnvironment(listener).expand(buildJob);
		String passedPackageName = build.getEnvironment(listener).expand(packageName);
		List<ListItem> passedColumnData = new ArrayList<ListItem>();
		if (addColumns) {
			for (ListItem item : data) {
				passedColumnData.add(new ListItem(build.getEnvironment(listener).expand(item.columnName),
						build.getEnvironment(listener).expand(item.contents)));
			}
		}
		String returnComment = null;

		if (passedPackageName == null) {
			passedPackageName = "";
		}

		if (!(passedBuildNumber.matches("^\\s*$") || passedEnvName.matches("^\\s*$")
				|| passedCompName.matches("^\\s*$"))) {
			returnComment = writeToDB(build, listener, passedEnvName, passedCompName, passedBuildNumber, "PRE",
					passedBuildJob, numberOfDays, passedPackageName, passedColumnData);
			listener.getLogger().println("Pre-Build Update: " + returnComment);
		} else {
			listener.getLogger().println("Environment dashboard not updated: one or more required values were blank");
		}
		// TearDown - This runs post all build steps
		class TearDownImpl extends SimpleBuildWrapper.Disposer implements Serializable {
			@Override
			public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
					throws IOException, InterruptedException {
				String passedBuildNumber = build.getEnvironment(listener).expand(buildNumber);
				String passedEnvName = build.getEnvironment(listener).expand(nameOfEnv);
				String passedCompName = build.getEnvironment(listener).expand(componentName);
				String passedBuildJob = build.getEnvironment(listener).expand(buildJob);
				String passedPackageName = build.getEnvironment(listener).expand(packageName);
				String doDeploy = build.getEnvironment(listener).expand("$UPDATE_ENV_DASH");
				List<ListItem> passedColumnData = Collections.emptyList();
				String returnComment = null;

				if (passedPackageName == null) {
					passedPackageName = "";
				}
				if (doDeploy == null || (!doDeploy.equals("true") && !doDeploy.equals("false"))) {
					doDeploy = "true";
				}

				if (doDeploy.equals("true")) {
					if (!(passedBuildNumber.matches("^\\s*$") || passedEnvName.matches("^\\s*$")
							|| passedCompName.matches("^\\s*$"))) {
						returnComment = writeToDB(build, listener, passedEnvName, passedCompName, passedBuildNumber,
								"POST", passedBuildJob, numberOfDays, passedPackageName, passedColumnData);
						listener.getLogger().println("Post-Build Update: " + returnComment);
					}
				} else {
					if (!(passedBuildNumber.matches("^\\s*$") || passedEnvName.matches("^\\s*$")
							|| passedCompName.matches("^\\s*$"))) {
						returnComment = writeToDB(build, listener, passedEnvName, passedCompName, passedBuildNumber,
								"NODEPLOY", passedBuildJob, numberOfDays, passedPackageName, passedColumnData);
						listener.getLogger().println("Post-Build Update: " + returnComment);
					}

				}

			}
		}
		context.setDisposer(new TearDownImpl());
	}

	@SuppressWarnings("rawtypes")
	private String writeToDB(Run<?, ?> build, TaskListener listener, String envName, String compName,
			String currentBuildNum, String runTime, String buildJob, Integer numberOfDays, String packageName,
			List<ListItem> passedColumnData) {
		String returnComment = null;
		if (envName.matches("^\\s*$") || compName.matches("^\\s*$")) {
			returnComment = "WARN: Either Environment name or Component name is empty.";
			return returnComment;
		}

		// Get DB connection
		Connection conn = DBConnection.getConnection();

		Statement stat = null;
		try {
			stat = conn.createStatement();
		} catch (SQLException e) {
			returnComment = "WARN: Could not execute statement.";
			return returnComment;
		}
		try {
			stat.execute(
					"CREATE TABLE IF NOT EXISTS env_dashboard (envComp VARCHAR(255), jobUrl VARCHAR(255), buildNum VARCHAR(255), buildStatus VARCHAR(255), envName VARCHAR(255), compName VARCHAR(255), created_at TIMESTAMP,  buildJobUrl VARCHAR(255), packageName VARCHAR(255));");
		} catch (SQLException e) {
			returnComment = "WARN: Could not create table env_dashboard.";
			return returnComment;
		}
		try {
			stat.execute("ALTER TABLE env_dashboard ADD IF NOT EXISTS packageName VARCHAR(255);");
		} catch (SQLException e) {
			returnComment = "WARN: Could not alter table env_dashboard.";
			return returnComment;
		}
		String columns = "";
		String contents = "";
		for (ListItem item : passedColumnData) {
			columns = columns + ", " + item.columnName;
			contents = contents + "', '" + item.contents;
			try {
				stat.execute("ALTER TABLE env_dashboard ADD IF NOT EXISTS " + item.columnName + " VARCHAR;");
			} catch (SQLException e) {
				returnComment = "WARN: Could not alter table env_dashboard to add column " + item.columnName + ".";
				return returnComment;
			}
		}
		String indexValueofTable = envName + '=' + compName;
		String currentBuildResult = "UNKNOWN";
		if (build.getResult() == null && runTime.equals("PRE")) {
			currentBuildResult = "RUNNING";
		} else if (build.getResult() == null && runTime.equals("POST")) {
			currentBuildResult = "SUCCESS";
		} else if (runTime.equals("NODEPLOY")) {
			currentBuildResult = "NODEPLOY";
		} else {
			currentBuildResult = build.getResult().toString();
		}
		String currentBuildUrl = build.getUrl();

		String buildJobUrl;
		// Build job is an optional configuration setting
		if (buildJob.isEmpty()) {
			buildJobUrl = "";
		} else {
			buildJobUrl = "job/" + buildJob + "/" + currentBuildNum;
		}

		String runQuery = null;
		if (runTime.equals("PRE")) {
			runQuery = "INSERT INTO env_dashboard (envComp, jobUrl, buildNum, buildStatus, envName, compName, created_at, buildJobUrl, packageName"
					+ columns + ") VALUES( '" + indexValueofTable + "', '" + currentBuildUrl + "', '" + currentBuildNum
					+ "', '" + currentBuildResult + "', '" + envName + "', '" + compName + "' , + current_timestamp, '"
					+ buildJobUrl + "' , '" + packageName + contents + "');";
		} else {
			if (runTime.equals("POST")) {
				runQuery = "UPDATE env_dashboard SET buildStatus = '" + currentBuildResult
						+ "', created_at = current_timestamp WHERE envComp = '" + indexValueofTable + "' AND joburl = '"
						+ currentBuildUrl + "';";
			} else {
				if (runTime.equals("NODEPLOY")) {
					runQuery = "DELETE FROM env_dashboard where envComp = '" + indexValueofTable + "' AND joburl = '"
							+ currentBuildUrl + "';";
				}
			}
		}
		try {
			stat.execute(runQuery);
		} catch (SQLException e) {
			returnComment = "Error running query " + runQuery + ".";
			return returnComment;
		}
		if (numberOfDays > 0) {
			runQuery = "DELETE FROM env_dashboard where created_at <= current_timestamp - " + numberOfDays;
			try {
				stat.execute(runQuery);
			} catch (SQLException e) {
				returnComment = "Error running delete query " + runQuery + ".";
				return returnComment;
			}
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
		return (DescriptorImpl) super.getDescriptor();
	}

        @Symbol("environmentDashboard")
	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		private String numberOfDays = "30";
		private Integer parseNumberOfDays;

		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "Details for Environment dashboard";
		}

		public FormValidation doCheckNameOfEnv(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set an Environment name.");
			return FormValidation.ok();
		}

		public FormValidation doCheckComponentName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a Component name.");
			return FormValidation.ok();
		}

		public FormValidation doCheckBuildNumber(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set the Build variable e.g: ${BUILD_NUMBER}.");
			return FormValidation.ok();
		}

		public FormValidation doCheckNumberOfDays(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set the number of days to retain the DB data.");
			} else {
				try {
					parseNumberOfDays = Integer.parseInt(value);
				} catch (Exception parseEx) {
					return FormValidation.error("Please provide an integer value.");
				}
			}
			return FormValidation.ok();
		}

		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			numberOfDays = formData.getString("numberOfDays");
			if (numberOfDays == null || numberOfDays.equals("")) {
				numberOfDays = "30";
			}
			save();
			return super.configure(req, formData);
		}

		public Integer getNumberOfDays() {
			return parseNumberOfDays;
		}

	}
}
