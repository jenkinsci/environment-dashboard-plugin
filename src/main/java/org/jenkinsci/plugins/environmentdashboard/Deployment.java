package org.jenkinsci.plugins.environmentdashboard;

public class Deployment {
	
	private String envName;
	private String compName;
	private String buildstatus;
	private String buildJobUrl;
	private String jobUrl;
	private String buildNum;
	private String createdAt;
	private String packageName;

	public String getEnvName() {
		return envName;
	}

	public void setEnvName(String envName) {
		this.envName = envName;
	}

	public String getCompName() {
		return compName;
	}

	public void setCompName(String compName) {
		this.compName = compName;
	}

	public String getBuildstatus() {
		return buildstatus;
	}

	public void setBuildstatus(String buildstatus) {
		this.buildstatus = buildstatus;
	}

	public String getBuildJobUrl() {
		return buildJobUrl;
	}

	public void setBuildJobUrl(String buildJobUrl) {
		this.buildJobUrl = buildJobUrl;
	}

	public String getJobUrl() {
		return jobUrl;
	}

	public void setJobUrl(String jobUrl) {
		this.jobUrl = jobUrl;
	}

	public String getBuildNum() {
		return buildNum;
	}

	public void setBuildNum(String buildNum) {
		this.buildNum = buildNum;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public String toString() {
		return "Deployment [envName=" + envName + ", compName=" + compName + ", buildstatus=" + buildstatus
				+ ", buildJobUrl=" + buildJobUrl + ", jobUrl=" + jobUrl + ", buildNum=" + buildNum + ", createdAt="
				+ createdAt + ", packageName=" + packageName + "]";
	}

}
