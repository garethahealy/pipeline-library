#!/usr/bin/env groovy

class Rollback implements Serializable {
	String clusterUrl = ""
	String clusterToken = ""
	String projectName = ""
    String deploymentConfig = ""
    String rollbackVersion = ""
	String name = ""
	String kind = "DeploymentConfig"
}

def call(Map input) {
	call(new Rollback(input))
}

def call(Rollback input) {
	if (input.deploymentConfig?.trim().length() > 0) {
		echo "WARNING: deploymentConfig is deprecated. Please use 'name'"

		if (input.deploymentConfig.contains("/")) {
			echo "WARNING: deploymentConfig with kind is deprecated. Please use 'kind'"

			def dcSplit = input.deploymentConfig.split("/")
			input.kind = dcSplit[0]
			input.name = dcSplit[1]
		} else {
			input.name = input.deploymentConfig
		}
	}

	List supportedKinds = ["DeploymentConfig", "dc", "Deployment", "DaemonSet", "StatefulSet"]

	assert input.name?.trim(): "Param name should be defined."
	assert input.kind?.trim(): "Param kind should be defined."
	assert supportedKinds.find { it.equalsIgnoreCase(input.kind) } : "Param kind (${input.kind}) not in supported kinds; ${supportedKinds.join(',')}"

	echo "Performing rollback to last successful deployment."

	openshift.withCluster(input.clusterUrl, input.clusterToken) {
		openshift.withProject(input.projectName) {
			def resource = openshift.selector(input.kind, input.name)
			if (resource.exists()) {
				def cmd = input.rollbackVersion?.trim().length() <= 0 ? "" : "--to-revision=${input.rollbackVersion}"
				resource.rollout().undo(cmd)
			} else {
				error "Failed to find '${input.kind}/${input.name}' in ${openshift.project()}"
			}
		}

	}

	echo "Finished rollback."
}