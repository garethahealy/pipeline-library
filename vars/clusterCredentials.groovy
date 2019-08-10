#!/usr/bin/env groovy

class ClusterCredentialsInput implements Serializable {
    String projectName = ""
    String secretName  = ""
}

def call(Map input) {
    call(new ClusterCredentialsInput(input))
}

def call(ClusterCredentialsInput input) {
    assert input.secretName?.trim()  : "Param secretName should be defined."

    def encodedApi
    def encodedToken

    openshift.withCluster() {
        openshift.withProject(input.projectName) {
            echo "Get Cluster Credentials: ${openshift.project()}/${input.secretName}"

            def secret = openshift.selector("secret/${input.secretName}")
            if (secret.exists()) {
                def secretObject = secret.object()
                def secretData = secretObject.data

                encodedApi = secretData.api
                encodedToken = secretData.token
            } else {
                error "Failed to find 'secret/${input.secretName}' in ${openshift.project()}"
            }
        }
    }
     
    //NOTE: the regex here makes it so that the jenkins-client-plugin wont verify the CA
    def api      = sh(script:"set +x; echo ${encodedApi}      | base64 --decode", returnStdout: true).replaceAll(/https?/, 'insecure')
    def token    = sh(script:"set +x; echo ${encodedToken}    | base64 --decode", returnStdout: true)

    return [api, token]
}
