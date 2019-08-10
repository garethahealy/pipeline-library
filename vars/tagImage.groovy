#!/usr/bin/env groovy

class TagImageInput implements Serializable {
    String clusterUrl = ""
    String clusterToken = ""
    String projectName = ""
    String sourceImagePath =  ""
    String sourceImageName = ""
    String sourceImageTag = "latest"
    String toImagePath
    String toImageName
    String toImageTag

    TagImageInput init() {
        if(!toImageName?.trim()) toImageName = sourceImageName
        if(!toImageTag?.trim()) toImageTag = sourceImageTag
        return this
    }
}

def call(Map input) {
    call(new TagImageInput(input).init())
}

def call(TagImageInput input) {
    assert input.sourceImageName?.trim() : "Param sourceImageName should be defined."
    assert input.sourceImageTag?.trim() : "Param sourceImageTag should be defined."
    assert input.toImageName?.trim() : "Param toImageName should be defined."
    assert input.toImageTag?.trim() : "Param toImageTag should be defined."

    openshift.withCluster(input.clusterUrl, input.clusterToken) {
        openshift.withProject(input.projectName) {
            def source = input.sourceImagePath?.trim() ? "${input.sourceImagePath}/${input.sourceImageName}:${input.sourceImageTag}" : "${input.sourceImageName}:${input.sourceImageTag}"
            def destination = input.toImagePath?.trim() ? "${input.toImagePath}/${input.toImageName}:${input.toImageTag}" : "${input.toImageName}:${input.toImageTag}"

            openshift.tag(source, destination)
        }
    }
}
