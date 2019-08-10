#!/usr/bin/env groovy

class RolloutInput implements Serializable {
    String clusterAPI = ""
    String clusterToken = ""
    String projectName = ""
    String deploymentConfigName = ""
    String name = ""
    String kind = "DeploymentConfig"
}

def call(Map input) {
    call(new RolloutInput(input))
}

def call(RolloutInput input) {
    if (input.deploymentConfigName?.trim().length() > 0) {
        echo "WARNING: deploymentConfigName is deprecated. Please use 'name'"

        input.name = input.deploymentConfigName
    }

    List supportedKinds = ["DeploymentConfig", "dc", "Deployment", "DaemonSet", "StatefulSet"]

    assert input.name?.trim(): "Param name should be defined."
    assert input.kind?.trim(): "Param kind should be defined."
    assert supportedKinds.find { it.equalsIgnoreCase(input.kind) } : "Param kind (${input.kind}) not in supported kinds; ${supportedKinds.join(',')}"

    openshift.withCluster(input.clusterAPI, input.clusterToken) {
        openshift.withProject(input.projectName) {
            echo "Get the Rollout Manager: ${input.kind}/${input.name}"

            def resource = openshift.selector(input.kind, input.name)
            if (resource.exists()) {
                def rolloutManager = resource.rollout()

                echo "Deploy: ${input.name}"
                rolloutManager.latest()

                echo "Wait for Deployment: ${input.name}"

                try {
                    rolloutManager.status("--watch=true")
                } catch (ex) {
                    //Something went wrong, so lets print out some helpful information
                    rolloutManager.history()
                    resource.describe()

                    error "$ex"
                }
            } else {
                error "Failed to find '${input.kind}/${input.name}'"
            }
        }
    }
}