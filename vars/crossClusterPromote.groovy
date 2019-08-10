#!/usr/bin/env groovy

class CopyImageInput implements Serializable{
    String sourceImageName
    String sourceImagePath
    String sourceImageTag = "latest"
    String destinationImageName
    String destinationImageTag
    String destinationImagePath
    String targetRegistryCredentials = "other-cluster-credentials"
    String clusterUrl = ""
    String clusterToken = ""

    CopyImageInput init() {
        if(!destinationImageName?.trim()) destinationImageName = sourceImageName
        if(!destinationImageTag?.trim()) destinationImageTag = sourceImageTag
        if(!destinationImagePath?.trim()) destinationImagePath = sourceImagePath
        return this
    }
}

def call(Map input) {
    call(new CopyImageInput(input).init())
}

def call(CopyImageInput input) {
    assert input.targetRegistryCredentials?.trim()        : "Param targetRegistryCredentials should be defined."
    assert input.sourceImageName?.trim()        : "Param sourceImageName should be defined."
    assert input.sourceImageTag?.trim()        : "Param sourceImageTag should be defined."
    assert input.destinationImagePath?.trim()        : "Param destinationImagePath should be defined."
    assert input.destinationImageName?.trim()        : "Param destinationImageName should be defined."
    assert input.destinationImageTag?.trim()        : "Param destinationImageTag should be defined."

    openshift.withCluster(input.clusterUrl, input.clusterToken) {
        openshift.withProject(input.sourceImagePath) {
            def secret = openshift.selector("secret/${input.targetRegistryCredentials}")
            if (secret.exists()) {
                def secretData = secret.object().data
                def registry = sh(script:"set +x; echo ${secretData.registry} | base64 --decode", returnStdout: true)
                def token = sh(script:"set +x; echo ${secretData.token} | base64 --decode", returnStdout: true)
                def username = sh(script:"set +x; echo ${secretData.username} | base64 --decode", returnStdout: true)

                def imageStream = openshift.selector("is", "${input.sourceImageName}")
                if (imageStream.exists()) {
                    def localRegistry = imageStream.object().status.dockerImageRepository
                    def from = "docker://${localRegistry}:${input.sourceImageTag}"
                    def to = "docker://${registry}/${input.destinationImagePath}/${input.destinationImageName}:${input.destinationImageTag}"

                    def localToken = readFile("/var/run/secrets/kubernetes.io/serviceaccount/token").trim()

                    echo "Now Promoting ${from} -> ${to}"
                    sh """
                        set +x
                        skopeo copy --remove-signatures \
                            --src-creds openshift:${localToken} \
                            --src-cert-dir=/run/secrets/kubernetes.io/serviceaccount/ \
                            --dest-creds ${username}:${token} \
                            --dest-tls-verify=false \
                            ${from} ${to}
                    """
                } else {
                    error "Failed to find 'is/${input.sourceImageName}' in ${openshift.project()}"
                }
            } else {
                error "Failed to find 'secret/${input.targetRegistryCredentials}' in ${openshift.project()}"
            }
        }
    }
}
