package org.jenkinsci.plugins.environmentdashboard;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.View;
import hudson.model.ViewDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.environmentdashboard.utils.DBConnection;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Class to provide build wrapper for Dashboard.
 * 
 * author vipin
 * date 15/10/2014
 */
public class EnvDashboardView extends View {

	private String envOrder = null;

	private String compOrder = null;

	private String deployHistory = null;
	
	private String componentNameWillBeDeleted = null;

	@DataBoundConstructor
	public EnvDashboardView(final String name, final String envOrder, final String compOrder,
			final String deployHistory, final String componentNameWillBeDeleted) {
		super(name, Hudson.getInstance());
		this.envOrder = envOrder;
		this.compOrder = compOrder;
		this.deployHistory = deployHistory;
		this.componentNameWillBeDeleted = componentNameWillBeDeleted;
	}

	static {
		ensureCorrectDBSchema();
	}

	private static void ensureCorrectDBSchema() {
		String returnComment = "";
		Connection conn = null;
		Statement stat = null;
		conn = DBConnection.getConnection();
		try {
			assert conn != null;
			stat = conn.createStatement();
		} catch (SQLException e) {
			System.out.println("E13" + e.getMessage());
		}
		try {
			stat.execute("ALTER TABLE env_dashboard ADD IF NOT EXISTS packageName VARCHAR(255);");
		} catch (SQLException e) {
			System.out.println(
					"E14: Could not alter table to add package column to table env_dashboard.\n" + e.getMessage());
		} finally {
			DBConnection.closeConnection();
		}
		return;
	}

	@Override
	protected void submit(final StaplerRequest req) throws IOException, ServletException, FormException {
		req.bindJSON(this, req.getSubmittedForm());
	}

	@RequirePOST
	public void doPurgeSubmit(final StaplerRequest req, StaplerResponse res)
			throws IOException, ServletException, FormException {
		checkPermission(Jenkins.ADMINISTER);

		Connection conn = null;
		Statement stat = null;
		conn = DBConnection.getConnection();
		try {
			assert conn != null;
			stat = conn.createStatement();
			stat.execute("TRUNCATE TABLE env_dashboard");
		} catch (SQLException e) {
			System.out.println("E15: Could not truncate table env_dashboard.\n" + e.getMessage());
		} finally {
			DBConnection.closeConnection();
		}
		res.forwardToPreviousPage(req);
	}

	@RequirePOST
	public void doDeleteComponent(final StaplerRequest req, StaplerResponse res)
			throws IOException, ServletException, FormException {
		checkPermission(Jenkins.ADMINISTER);
		//System.out.println("[OKSY_log value of componentNameWillBeDeleted in doDeleteComponent method: ]" + componentNameWillBeDeleted);		
		Connection conn = null;
		//Statement stat = null;
		PreparedStatement preStat = null;
		conn = DBConnection.getConnection();
		String runQuery = null;
		
		try {
			assert conn != null;
			runQuery = "DELETE FROM env_dashboard where compName = ?;";
			preStat = conn.prepareStatement(runQuery);
			preStat.setString(1, componentNameWillBeDeleted);
			int query = preStat.executeUpdate();

			System.out.println("Selected component has been removed successfully!.. " + componentNameWillBeDeleted);

			
		} catch (SQLException e) {
			System.out.println("E15: Could not delete component lines.\n" + e.getMessage());
		} finally {
			DBConnection.closeConnection();
		}
		res.forwardToPreviousPage(req);
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
		private String componentNameWillBeDeleted;

		/**
		 * descriptor impl constructor This empty constructor is required for
		 * stapler. If you remove this constructor, text name of
		 * "Build Pipeline View" will be not displayed in the "NewView" page
		 */
		public DescriptorImpl() {
			load();
		}

		public static ArrayList<String> getCustomColumns() {
			Connection conn = null;
			Statement stat = null;
			ArrayList<String> columns;
			columns = new ArrayList<String>();
			String queryString = "SELECT DISTINCT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='ENV_DASHBOARD';";
			String[] fields = { "envComp", "compName", "envName", "buildstatus", "buildJobUrl", "jobUrl", "buildNum",
					"created_at", "packageName" };
			boolean columnFound = false;
			try {
				ResultSet rs = null;
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
				String col = "";
				while (rs.next()) {
					columnFound = false;
					col = rs.getString("COLUMN_NAME");
					for (String presetColumn : fields) {
						if (col.toLowerCase().equals(presetColumn.toLowerCase())) {
							columnFound = true;
							break;
						}
					}
					if (!columnFound) {
						columns.add(col.toLowerCase());
					}
				}
				DBConnection.closeConnection();
			} catch (SQLException e) {
				System.out.println("E11" + e.getMessage());
				return null;
			}
			return columns;
		}

		public ListBoxModel doFillColumnItems() {
			ListBoxModel m = new ListBoxModel();
			ArrayList<String> columns = getCustomColumns();
			int position = 0;
			m.add("Select column to remove", "");
			for (String col : columns) {
				m.add(col, col);
			}
			return m;
		}

		@SuppressWarnings("unused")
		public FormValidation doDropColumn(@QueryParameter("column") final String column) {
			Hudson.getInstance().checkPermission(Jenkins.ADMINISTER);
			Connection conn = null;
			Statement stat = null;
			if ("".equals(column)) {
				return FormValidation.ok();
			}
			String queryString = "ALTER TABLE ENV_DASHBOARD DROP COLUMN " + column + ";";
			// Get DB connection
			conn = DBConnection.getConnection();

			try {
				assert conn != null;
				stat = conn.createStatement();
			} catch (SQLException e) {
				return FormValidation.error("Failed to create statement.");
			}
			try {
				assert stat != null;
				stat.execute(queryString);
			} catch (SQLException e) {
				DBConnection.closeConnection();
				return FormValidation.error("Failed to remove column: " + column
						+ "\nThis column may have already been removed. Refresh to update the list of columns to remove.");
			}
			DBConnection.closeConnection();

			return FormValidation.ok("Successfully removed column " + column + ".");
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
			componentNameWillBeDeleted = formData.getString("componentNameWillBeDeleted");
			save();
			return super.configure(req, formData);
		}
	}

	public ArrayList<String> splitEnvOrder(String envOrder) {
		ArrayList<String> orderOfEnvs = new ArrayList<String>();
		if (!"".equals(envOrder)) {
			orderOfEnvs = new ArrayList<String>(Arrays.asList(envOrder.split("\\s*,\\s*")));
		}
		return orderOfEnvs;
	}

	public ArrayList<String> splitCompOrder(String compOrder) {
		ArrayList<String> orderOfComps = new ArrayList<String>();
		if (!"".equals(compOrder)) {
			orderOfComps = new ArrayList<String>(Arrays.asList(compOrder.split("\\s*,\\s*")));
		}
		return orderOfComps;
	}

	public ResultSet runQuery(String queryString) {
		Connection conn = null;
		Statement stat = null;

		ResultSet rs = null;

		// Get DB connection
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
		if (orderOfEnvs == null || orderOfEnvs.isEmpty()) {
			String queryString = "select distinct envname from env_dashboard order by envname;";
			try {
				ResultSet rs = runQuery(queryString);
				if (rs == null) {
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
		if (orderOfComps == null || orderOfComps.isEmpty()) {
			String queryString = "select distinct compname from env_dashboard order by compname;";
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
		if (deployHistory == null || deployHistory.equals("")) {
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
		if (lastDeploy <= 0) {
			lastDeploy = 10;
		}
		ArrayList<String> deployments;
		deployments = new ArrayList<String>();
		String queryString = "select top " + lastDeploy + " created_at from env_dashboard where envName ='" + env
				+ "' order by created_at desc;";
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
		if (orderOfEnvs == null || orderOfEnvs.isEmpty()) {
			return "NONE";
		} else {
			return "ENVS";
		}
	}

	public String getNiceTimeStamp(String timeStamp) {
		return timeStamp.substring(0, 19);
	}

	public HashMap getCompDeployed(String env, String time) {
		HashMap<String, String> deployment;
		deployment = new HashMap<String, String>();
		String[] fields = { "buildstatus", "compName", "buildJobUrl", "jobUrl", "buildNum", "packageName" };
		String queryString = "select " + StringUtils.join(fields, ", ").replace(".$", "")
				+ " from env_dashboard where envName = '" + env + "' and created_at = '" + time + "';";
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

	public ArrayList<String> getCustomDBColumns() {
		return DescriptorImpl.getCustomColumns();
	}

	public ArrayList<HashMap<String, String>> getDeploymentsByComp(String comp, Integer lastDeploy) {
		if (lastDeploy <= 0) {
			lastDeploy = 10;
		}
		ArrayList<HashMap<String, String>> deployments;
		deployments = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> hash;
		String[] fields = { "envName", "buildstatus", "buildJobUrl", "jobUrl", "buildNum", "created_at",
				"packageName" };
		ArrayList<String> allDBFields = getCustomDBColumns();
		for (String field : fields) {
			allDBFields.add(field);
		}
		String queryString = "select top " + lastDeploy + " * from env_dashboard where compName='" + comp
				+ "' order by created_at desc;";
		try {
			ResultSet rs = runQuery(queryString);
			while (rs.next()) {
				hash = new HashMap<String, String>();
				for (String field : allDBFields) {
					hash.put(field, rs.getString(field));
				}
				deployments.add(hash);
			}
			DBConnection.closeConnection();
		} catch (SQLException e) {
			System.out.println("E11" + e.getMessage());
			return null;
		}
		return deployments;
	}

	public ArrayList<HashMap<String, String>> getDeploymentsByCompEnv(String comp, String env, Integer lastDeploy) {
		if (lastDeploy <= 0) {
			lastDeploy = 10;
		}
		ArrayList<HashMap<String, String>> deployments;
		deployments = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> hash;
		String[] fields = { "envName", "buildstatus", "buildJobUrl", "jobUrl", "buildNum", "created_at",
				"packageName" };
		ArrayList<String> allDBFields = getCustomDBColumns();
		for (String field : fields) {
			allDBFields.add(field);
		}
		String queryString = "select top " + lastDeploy + " " + StringUtils.join(allDBFields, ", ").replace(".$", "")
				+ " from env_dashboard where compName='" + comp + "' and envName='" + env
				+ "' order by created_at desc;";
		try {
			ResultSet rs = runQuery(queryString);
			while (rs.next()) {
				hash = new HashMap<String, String>();
				for (String field : allDBFields) {
					hash.put(field, rs.getString(field));
				}
				deployments.add(hash);
			}
			DBConnection.closeConnection();
		} catch (SQLException e) {
			System.out.println("E11" + e.getMessage());
			return null;
		}
		return deployments;
	}

	public HashMap getCompLastDeployed(String env, String comp) {
		HashMap<String, String> deployment;
		deployment = new HashMap<String, String>();
		String[] fields = { "buildstatus", "buildJobUrl", "jobUrl", "buildNum", "created_at", "packageName" };
		ArrayList<String> allDBFields = getCustomDBColumns();
		for (String field : fields) {
			allDBFields.add(field);
		}
		String queryString = "select top 1 " + StringUtils.join(allDBFields, ", ").replace(".$", "")
				+ " from env_dashboard where envName = '" + env + "' and compName = '" + comp
				+ "' order by created_at desc;";
		try {
			ResultSet rs = runQuery(queryString);
			rs.next();
			for (String field : allDBFields) {
				deployment.put(field, rs.getString(field));
			}
			DBConnection.closeConnection();
		} catch (SQLException e) {
			if (e.getErrorCode() == 2000) {
				// We'll assume this comp has never been deployed to this env }
			} else {
				System.out.println("E12" + e.getMessage());
				System.out.println("Error executing: " + queryString);
			}
		}
		return deployment;
	}

	@Override
	public Collection<TopLevelItem> getItems() {
	        return Collections.EMPTY_LIST;
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

	public String getcomponentNameWillBeDeleted() {
		return componentNameWillBeDeleted;
	}

	public void setcomponentNameWillBeDeleted(final String componentNameWillBeDeleted) {
		this.componentNameWillBeDeleted = componentNameWillBeDeleted;
	}
	
	@Override
	public boolean contains(TopLevelItem topLevelItem) {
		return false;
	}

	@Override
	public void onJobRenamed(Item item, String s, String s2) {

	}
}
