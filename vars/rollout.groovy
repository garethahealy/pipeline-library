#!/usr/bin/env groovy

class RolloutInput implements Serializable {
    String deploymentConfigName = ""

    //Optional - Platform
    String clusterAPI           = ""
    String clusterToken         = ""
    String projectName          = ""
}

def call(Map input) {
    call(new RolloutInput(input))
}

def call(RolloutInput input) {
    assert input.deploymentConfigName?.trim() : "Param deploymentConfigName should be defined."

    openshift.withCluster(input.clusterAPI, input.clusterToken) {
        openshift.withProject(input.projectName) {
            echo "Get the Rollout Manager: ${input.deploymentConfigName}"
            def deploymentConfig = openshift.selector('dc', input.deploymentConfigName)
            if (deploymentConfig.exists()) {
                def rolloutManager = deploymentConfig.rollout()

                echo "Deploy: ${input.deploymentConfigName}"
                rolloutManager.latest()

                echo "Wait for Deployment: ${input.deploymentConfigName}"
                rolloutManager.status("-w")
            } else {
                error "Failed to find 'dc/${input.deploymentConfigName}' in ${openshift.project()}"
            }
        }
    }
}