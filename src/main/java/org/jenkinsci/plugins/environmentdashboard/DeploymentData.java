package org.jenkinsci.plugins.environmentdashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DeploymentData {

	private String createdTime;
	private String environments;
	private String components;
	private List<Deployment> allDeploymentList = new ArrayList<>();
	private LinkedHashMap<String, LinkedHashMap<String, ArrayList<Deployment>>> deployments= new LinkedHashMap<>();

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

	public List<Deployment> getAllDeploymentList() {
		return allDeploymentList;
	}

	public void setAllDeploymentList(List<Deployment> allDeploymentList) {
		this.allDeploymentList = allDeploymentList;
	}

	public LinkedHashMap<String, LinkedHashMap<String, ArrayList<Deployment>>> getDeployments() {
		return deployments;
	}

	public void setDeployments(LinkedHashMap<String, LinkedHashMap<String, ArrayList<Deployment>>> deployments) {
		this.deployments = deployments;
	}

}
