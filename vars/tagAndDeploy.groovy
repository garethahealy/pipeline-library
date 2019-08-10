#!/usr/bin/env groovy

class TagAndDeployInput implements Serializable {
    String imageName                    = ''
    String imageNamespace               = ''
    String imageVersion                 = ''
    String registryFQDN                 = ''
    String clusterAPI                   = ''
    String clusterToken                 = ''
    String deployDestinationProjectName = ''
    String deployDestinationVersionTag  = ''  
    String tagDestinationTLSVerify      = 'false'
    String tagSourceTLSVerify           = 'false'
}

def call(Map input) {
    call(new TagAndDeployInput(input))
}

def call(TagAndDeployInput input) {
    assert input.registryFQDN?.trim()             : "Param registryFQDN should be defined."
    assert input.imageNamespace?.trim()           : "Param imageNamespace should be defined."
    assert input.imageName?.trim()                : "Param imageName should be defined."
    assert input.imageVersion?.trim()             : "Param imageVersion should be defined."
    assert input.deployDestinationVersionTag?.trim()      : "Param deployDestinationVersionTag should be defined."
    assert input.tagSourceTLSVerify?.trim()   : "Param tagSourceTLSVerify should be defined."
    assert input.tagDestinationTLSVerify?.trim()   : "Param tagDestinationTLSVerify should be defined."

    def source = "${input.registryFQDN}/${input.imageNamespace}/${input.imageName}:${input.imageVersion}"
    def destination = "${input.registryFQDN}/${input.imageNamespace}/${input.imageName}:${input.deployDestinationVersionTag}"

    echo "Tag ${source} as ${destination}"
    sh """
        skopeo copy \
            --authfile /var/run/secrets/kubernetes.io/dockerconfigjson/.dockerconfigjson \
            --src-tls-verify=${input.tagSourceTLSVerify} \
            --dest-tls-verify=${input.tagDestinationTLSVerify} \
            docker://${source} docker://${destination}
    """

    echo "Deploy to ${input.deployDestinationProjectName}"
    rollout([
        clusterAPI     : input.clusterAPI,
        clusterToken   : input.clusterToken,
        projectName    : input.deployDestinationProjectName,
        deploymentConfigName: input.imageName
    ])
}
