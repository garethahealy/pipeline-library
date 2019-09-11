#!/usr/bin/env groovy

class BinaryBuildInput implements Serializable {
    String buildConfigName = ""
    String buildFromFlag   = "--from-dir"
    String buildFromPath   = ""

    //Optional - Platform
    String clusterAPI      = ""
    String clusterToken    = ""
    String projectName     = ""
}

def call(Map input) {
    call(new BinaryBuildInput(input))
}

def call(BinaryBuildInput input) {
    assert input.buildConfigName?.trim() : "Param buildConfigName should be defined."
    assert input.buildFromFlag?.trim()   : "Param buildFromFlag should be defined."
    assert input.buildFromPath?.trim()   : "Param buildFromPath should be defined."

    openshift.withCluster(input.clusterAPI, input.clusterToken) {
        openshift.withProject(input.projectName) {
            echo "Start & Follow Build"

            def buildConfig = openshift.selector('bc', input.buildConfigName)
            if (buildConfig.exists()) {
                def build = buildConfig.startBuild("${input.buildFromFlag}=${input.buildFromPath}", '--wait')
                build.logs('-f')
            } else {
                error "Failed to find 'bc/${input.buildConfigName}' in ${openshift.project()}"
            }
        }
    }
}
