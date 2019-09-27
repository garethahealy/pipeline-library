#!/usr/bin/env groovy

class RolloutInput implements Serializable {
    //Required
    String deploymentConfigName = ""

    //Optional
    boolean latest = true

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
            echo "Attemping to rollout latest 'deploymentconfig/${input.deploymentConfigName}' in ${openshift.project()}"

            def deploymentConfig = openshift.selector('dc', input.deploymentConfigName)
            def rolloutManager   = deploymentConfig.rollout()

            if (input.latest) {
                rolloutManager.latest()
            }

            echo "Waiting for rollout of 'deploymentconfig/${input.deploymentConfigName}' in ${openshift.project()} to complete..."

            rolloutManager.status("--wait")
        }
    }
}