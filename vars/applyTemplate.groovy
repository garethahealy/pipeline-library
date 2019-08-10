#!/usr/bin/env groovy
import groovy.json.JsonSlurperClassic

class ApplyTemplateInput implements Serializable {
    String clusterUrl = ""
    String clusterToken = ""
    String projectName = ""
    String templateFile = ""
    String parameterFile = ""
    String templateProject = ""
}

def call(Map input) {
    call(new ApplyTemplateInput(input))
}

def call(ApplyTemplateInput input) {
    assert input.templateFile?.trim(): "Param templateFile should be defined."

    def paramFileArgument = input.parameterFile ? "--param-file=${input.parameterFile}" : ""

    openshift.withCluster(input.clusterUrl, input.clusterToken) {
        openshift.withProject(input.projectName) {
            //def templateMap = getRawTemplate(input.templateProject, input.templateFile)
            def models = openshift.process( "--filename=${input.templateFile}", "--param-file=${input.parameterFile}", "--ignore-unknown-parameters")

            echo "Creating this template will instantiate ${models.size()} objects"

            // We need to find DeploymentConfig definitions inside 
            // So iterating trough all the objects loaded from the Template
            for (o in models) {
                if (o.kind == "DeploymentConfig") {
                    // The bug in OCP 3.7 is that when applying DC the "Image" can't be undefined
                    // But when using automatic triggers it updates the value for this on runtime
                    // So when applying this dynamic value gets overwriten and breaks deployments

                    // We will check if this DeploymentConfig already pre-exists and fetch the current value of Image
                    // And set this Image -value into the DeploymentConfig template we are applying
                    def dcSelector = openshift.selector("deploymentconfig/${o.metadata.name}")
                    def foundObjects = dcSelector.exists()
                    if (foundObjects) {
                        echo "This DC exists, copying the image value"
                        def dcObjs = dcSelector.objects(exportable: true)
                        echo "Image now: ${dcObjs[0].spec.template.spec.containers[0].image}"
                        o.spec.template.spec.containers[0].image = dcObjs[0].spec.template.spec.containers[0].image
                    }
                }
            }

            def created = openshift.apply(models)
            echo "Created: ${created.names()}"
        }
    }
}

/**
 * Loads the template into memory
 *
 * @param project project name if template already exists in openshift
 * @param template template can be; file: or http: or a name of the template which exists in openshift
 * @return Map of template loaded into memory
 */
Map getRawTemplate(String project, String template) {
    def answer

    if (template.startsWith("file:")) {
        //Format: https://en.wikipedia.org/wiki/File_URI_scheme#Examples
        if (template.endsWith(".yaml") || template.endsWith(".yml")) {
            answer = readYaml file: new File(new URI(template)).getCanonicalPath()
        } else if (template.endsWith(".json")) {
            def fileContent = readFile file: new File(new URI(template)).getCanonicalPath()
            answer = new JsonSlurperClassic().parseText(fileContent)
        } else {
            error "Unknown file extension for: $template - Expected: yaml / yml / json"
        }
    } else if (template.startsWith("http:") || template.startsWith("https:")) {
        if (template.endsWith(".json")) {
            answer = new JsonSlurperClassic().parse(new URI(template).toURL(), "UTF-8")
        } else {
            error "Unknown file extension for: $template - Expected: json"
        }
    } else {
        openshift.withProject(project) {
            def templateSelector = openshift.selector("template", template)
            if (templateSelector.exists()) {
                answer = templateSelector.object()
            } else {
                error "Unable to find template '${project}/${template}'"
            }
        }
    }

    return answer
}
