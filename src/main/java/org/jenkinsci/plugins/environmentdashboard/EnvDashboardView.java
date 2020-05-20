package org.jenkinsci.plugins.environmentdashboard;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor.FormException;
import hudson.model.View;
import hudson.model.ViewDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
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
	
	private static final String REG_PATTERN = "\\s*,\\s*";
	
	private static final int DEPLOYMENT_HISTORY = 10;
	
	private DeploymentData deploymentData = null;
	
	private static final ArrayList<String> FIELDS = new ArrayList<String>() {{
	    add("envcomp");
	    add("compname");
	    add("envname");
	    add("buildstatus");
	    add("buildjoburl");
	    add("joburl");
	    add("buildnum");
	    add("created_at");
	    add("packageName");
	}};

	@DataBoundConstructor
	public EnvDashboardView(final String name, final String envOrder, final String compOrder,
			final String deployHistory) {
		super(name, Jenkins.get());
		this.envOrder = envOrder;
		this.compOrder = compOrder;
		this.deployHistory = deployHistory;
		this.deploymentData = new DeploymentData();
	}

	static {
		ensureCorrectDBSchema();
	}

	private static void ensureCorrectDBSchema() {
		try(Connection conn = DBConnection.getConnection();
				Statement stat = conn.createStatement();) {
			stat.execute("ALTER TABLE env_dashboard ADD IF NOT EXISTS packageName VARCHAR(255);");
		} catch (SQLException e) {
			System.err.println("E14: Could not alter table to add package column to table env_dashboard.\n" + e.getMessage());
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
			throws IOException, ServletException {
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

	@Override
	public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
		return Jenkins.get().doCreateItem(req, rsp);
	}

	public String[] splitEnvOrder(String envOrder) {
		if (!"".equals(envOrder)) {
			StringBuilder envBuilder = new StringBuilder();
			String[] envs = envOrder.split(REG_PATTERN);
			for(String env : envs) {
				envBuilder.append(env+",");
			}
			envBuilder.setLength(envBuilder.length() - 1);
			deploymentData.setEnvironments(envBuilder.toString());
			return envOrder.split(REG_PATTERN);
		} else {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}
	}

	public String[] splitCompOrder(String compOrder) {
		if (!"".equals(compOrder)) {
			StringBuilder componentBuilder = new StringBuilder();
			String[] components = compOrder.split(REG_PATTERN);
			for(String component : components) {
				componentBuilder.append(component+",");
			}
			componentBuilder.setLength(componentBuilder.length() -1);
			deploymentData.setComponents(componentBuilder.toString());
			return compOrder.split(REG_PATTERN);
		} else {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}
	}

	public ResultSet runQuery(String queryString) {
		// Get DB connection
		try {
			Connection conn = DBConnection.getConnection();
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(queryString);
			return rs;
		} catch (SQLException e) {
			System.err.println("E3" + e.getMessage());
		}
		return null;
	}

	public String[] getOrderOfEnvs() {
		String[] orderOfEnvs = splitEnvOrder(envOrder);
		if (orderOfEnvs == null || orderOfEnvs.length == 0) {
			String queryString = "select distinct envname from env_dashboard order by envname;";
			try {
				ResultSet rs = runQuery(queryString);
				if (rs == null) {
					return ArrayUtils.EMPTY_STRING_ARRAY;
				}
				StringBuilder environments = new StringBuilder();
				while (rs.next()) {
					environments.append(rs.getString("envName")+",");
				}
				environments.setLength(environments.length() - 1);
				orderOfEnvs = environments.toString().split(REG_PATTERN);
				deploymentData.setEnvironments(environments.toString());
			} catch (SQLException e) {
				System.err.println("E6" + e.getMessage());
			} finally {
				DBConnection.closeConnection();
			}
		}
		return orderOfEnvs;
	}

	public String[] getOrderOfComps() {
		String[] orderOfComps = splitCompOrder(compOrder);
		if (orderOfComps == null || orderOfComps.length == 0) {
			String queryString = "select distinct compname from env_dashboard order by compname;";
			try {
				ResultSet rs = runQuery(queryString);
				if (rs == null) {
					return ArrayUtils.EMPTY_STRING_ARRAY;
				}
				StringBuilder components = new StringBuilder();
				while (rs.next()) {
					components.append(rs.getString("compName")+",");
				}
				components.setLength(components.length() - 1);
				orderOfComps = components.toString().split(REG_PATTERN);
				deploymentData.setComponents(components.toString());
			} catch (SQLException e) {
				System.err.println("E8" + e.getMessage());
			} finally {
				DBConnection.closeConnection();
			}
		}
		return orderOfComps;
	}

	public int getLimitDeployHistory() {
		try {
			if (deployHistory == null || deployHistory.isEmpty()) {
				return DEPLOYMENT_HISTORY;
			} else {
				return Integer.parseInt(deployHistory);
			}
		} catch (NumberFormatException e) {
			return DEPLOYMENT_HISTORY;
		}
	}

	public String anyJobsConfigured() {
		deploymentData = new DeploymentData();
		String[] orderOfEnvs = getOrderOfEnvs();
		if (orderOfEnvs == null || orderOfEnvs.length == 0) {
			return "NONE";
		} else {
			return "ENVS";
		}
	}

	public String getNiceTimeStamp(String timeStamp) {
		return timeStamp == null ? null : timeStamp.substring(0, 19);
	}
	
	public Map<String, String> getDeploymentResultSet(ResultSet rs) throws SQLException {
		HashMap<String, String> values = new HashMap<>();
		for (String field :  getCustomDBColumns()) {
			values.put(field, rs.getString(field));
		}
		return values;
	}
	
	public List<Map<String, String>> getDeployments(int lastDeploy) {
		if(deploymentData.getAllDeploymentList() == null || deploymentData.getAllDeploymentList().isEmpty()) {
			if (lastDeploy <= 0) {
				lastDeploy = DEPLOYMENT_HISTORY;
			}
			StringBuilder sqlBuilder = new StringBuilder();
			for(String compName : deploymentData.getComponents().split(REG_PATTERN)) {
				for(String envName : deploymentData.getEnvironments().split(REG_PATTERN)) {
					sqlBuilder.append("(select * from env_dashboard "
							+ "where envName='"+envName+"' and compName='"+compName+"' order by created_at desc limit "+lastDeploy+") ");
					sqlBuilder.append("UNION ");
				}
			}
			sqlBuilder.setLength(sqlBuilder.length() - 6);
			sqlBuilder.append("order by created_at desc;");
			try {
				ResultSet resultSetObj = runQuery(sqlBuilder.toString());
				while (resultSetObj.next()) {
					formDataStructure(resultSetObj);
				}
			} catch (SQLException e) {
				System.err.println("E11" + e.getMessage());
			} finally {
				DBConnection.closeConnection();
			}
		}
		return deploymentData.getAllDeploymentList();
	}
	
	private void formDataStructure(ResultSet resultSetObj) throws SQLException {
		Map<String, String> deploymentObj = getDeploymentResultSet(resultSetObj);
		if(!deploymentData.getDeployments().containsKey(deploymentObj.get("envname"))) {
			LinkedHashMap<String, ArrayList<Map<String, String>>> compDeployments = new LinkedHashMap<>(); 
			ArrayList<Map<String, String>> deployment = new ArrayList<>();
			deployment.add(deploymentObj);
			compDeployments.put(deploymentObj.get("compname"), deployment);
			deploymentData.getDeployments().put(deploymentObj.get("envname"), compDeployments);
		} else if(deploymentData.getDeployments().containsKey(deploymentObj.get("envname")) && 
				!deploymentData.getDeployments().get(deploymentObj.get("envname")).containsKey(deploymentObj.get("compname"))) {
			ArrayList<Map<String, String>> deployment = new ArrayList<>();
			deployment.add(deploymentObj);
			deploymentData.getDeployments().get(deploymentObj.get("envname")).put(deploymentObj.get("compname"), deployment);
		} else {
			deploymentData.getDeployments().get(deploymentObj.get("envname")).get(deploymentObj.get("compname")).add(deploymentObj);
		}
		deploymentData.getAllDeploymentList().add(deploymentObj);
	}

	public Map<String, String> getCompLastDeployed(String envName, String compName) {
		try {
			return deploymentData.getDeployments().get(envName).get(compName).get(0);
		} catch(Exception exp) {
			return new HashMap<>();
		}
	}

	/**
	 * Popup per Env per Component
	 * @param compName
	 * @param envName
	 * @returns list of deployments
	 */
	public List<Map<String, String>> getDeploymentsByCompEnv(String compName, String envName) {
		if(deploymentData.getDeployments().containsKey(envName) && 
				deploymentData.getDeployments().get(envName).containsKey(compName)) {
			return deploymentData.getDeployments().get(envName).get(compName);
		} else {
			return new ArrayList<>();
		}
	}
	
	/**
	 * Component History & Popup per Env per Component
	 * @param compName
	 * @return list of deployments
	 */
	public List<Map<String, String>> getDeploymentsByComp(String compName) {
		ArrayList<Map<String, String>> deployments = new ArrayList<>();
		for(Map<String, String> deployment : deploymentData.getAllDeploymentList()) {
			if(deployment.get("compname").equalsIgnoreCase(compName)) {
				deployments.add(deployment);
			}
		}
		return deployments;
	}
	
	public String getDeploymentObjValue(Map<String, String> dataObj, String fieldName) {
		if(dataObj.containsKey(fieldName) && FIELDS.indexOf(fieldName) == -1) {
			return dataObj.get(fieldName);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unused")
	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {

		private String envOrder;
		private String compOrder;
		private String deployHistory;
		private static ArrayList<String> columns = new ArrayList<>();
		
		/**
		 * descriptor impl constructor This empty constructor is required for
		 * stapler. If you remove this constructor, text name of
		 * "Build Pipeline View" will be not displayed in the "NewView" page
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * @returns all the column names
		 */
		public static List<String> getCustomColumns() {
			if(columns.isEmpty()) {
				String queryString = "SELECT DISTINCT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='ENV_DASHBOARD';";
				try {
					Connection conn = DBConnection.getConnection();
					Statement stat = conn.createStatement();
					ResultSet rs = stat.executeQuery(queryString);
					while (rs.next()) {
						String col = rs.getString("COLUMN_NAME");
						columns.add(col.toLowerCase());
					}
				} catch (SQLException e) {
					System.out.println("E11" + e.getMessage());
				} finally {
					DBConnection.closeConnection();
				}
			}
			return columns;
		}

		public ListBoxModel doFillColumnItems() {
			ListBoxModel m = new ListBoxModel();
			List<String> allColumns = getCustomColumns();
			m.add("Select column to remove", "");
			for (String column : allColumns) {
				m.add(column, column);
			}
			return m;
		}

		public FormValidation doDropColumn(@QueryParameter("column") final String column) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
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
			save();
			return super.configure(req, formData);
		}
	}

	public List<String> getCustomDBColumns() {
		return DescriptorImpl.getCustomColumns();
	}

	@Override
	public Collection<TopLevelItem> getItems() {
	        return Collections.emptyList();
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
