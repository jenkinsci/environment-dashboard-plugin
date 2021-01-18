package org.jenkinsci.plugins.environmentdashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeploymentData {

	private String createdTime;
	private String environments;
	private String components;
	private List<Map<String, String>> allDeploymentList = new ArrayList<>();
	private LinkedHashMap<String, LinkedHashMap<String, ArrayList<Map<String, String>>>> deployments = new LinkedHashMap<>();

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getEnvironments() {
		return environments;
	}

	public void setEnvironments(String environments) {
		this.environments = environments;
	}

	public String getComponents() {
		return components;
	}

	public void setComponents(String components) {
		this.components = components;
	}

	public List<Map<String, String>> getAllDeploymentList() {
		return allDeploymentList;
	}

	public void setAllDeploymentList(List<Map<String, String>> allDeploymentList) {
		this.allDeploymentList = allDeploymentList;
	}

	public LinkedHashMap<String, LinkedHashMap<String, ArrayList<Map<String, String>>>> getDeployments() {
		return deployments;
	}

	public void setDeployments(LinkedHashMap<String, LinkedHashMap<String, ArrayList<Map<String, String>>>> deployments) {
		this.deployments = deployments;
	}

}
