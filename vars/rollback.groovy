#!/usr/bin/env groovy

class Rollback implements Serializable {
	String deploymentConfig
	String rollbackVersion = ""

	//Optional - Platform
	String clusterUrl = ""
	String clusterToken = ""
	String projectName = ""
}

def call(Map input) {
	call(new Rollback(input))
}

def call(Rollback input) {
	println "Performing rollback to last successful deployment."

	String rollbackToRevision = ""
	if(input.rollbackVersion.length() > 0) {
		println "Setting revision to rollback to"
		rollbackToRevision = "--to-revision=" + input.rollbackVersion
	}

	openshift.withCluster(input.clusterUrl, input.clusterToken) {
		openshift.withProject(input.projectName) {
			def deployment = openshift.selector(input.deploymentConfig)
			if (deployment.exists()) {
				deployment.rollout().undo(rollbackToRevision)
			} else {
				error "Failed to find '${input.deploymentConfig}' in ${openshift.project()}"
			}
		}
	}

	println "Finished rollback."
}