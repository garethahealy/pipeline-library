#!/usr/bin/env groovy

class ConfigMapInput implements Serializable {
    String clusterAPI      = ""
    String clusterToken    = ""
    String projectName = ""
    String configMapName  = ""
}

def call(Map input) {
    call(new ConfigMapInput(input))
}

def call(ConfigMapInput input) {
    assert input.configMapName?.trim()  : "Param configMapName should be defined."

    def configMapData

    openshift.withCluster(input.clusterAPI, input.clusterToken) {
        openshift.withProject(input.projectName) {
            echo "Read ConfigMap: ${openshift.project()} / ${input.configMapName}"

            def configMap = openshift.selector("configmap/${input.configMapName}")
            if (configMap.exists()) {
                def configMapObject = configMap.object()
                configMapData = configMapObject.data
            } else {
                error "Failed to find 'configmap/${input.configMapName}'"
            }

        }
    }
     
    return configMapData
}