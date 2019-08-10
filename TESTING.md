# Testing
Tested on opentlc configure 3.x instance

# Create project
oc new-project pipelinelib-promotion-testing
oc new-project pipelinelib-testing

## Create additional slaves
### HOW IT SHOULD BE
oc import-image jenkins-slave-ansible --from=quay.io/redhat-cop/jenkins-slave-ansible --confirm
oc label imagestream jenkins-slave-ansible role=jenkins-slave

oc import-image jenkins-slave-image-mgmt --from=quay.io/redhat-cop/jenkins-slave-image-mgmt --confirm
oc label imagestream jenkins-slave-image-mgmt role=jenkins-slave

### REALITY
oc import-image jenkins-slave-ansible --from=quay.io/redhat-cop/jenkins-slave-ansible:v1.11 --confirm
oc label imagestream jenkins-slave-ansible role=jenkins-slave

oc import-image jenkins-slave-image-mgmt --from=siamaksade/jenkins-slave-skopeo-centos7 --confirm
oc label imagestream jenkins-slave-image-mgmt role=jenkins-slave

## Create BuildConfigs
find test -type f -name "Jenkinsfile-*" -exec bash -c '\
    oc process -f https://raw.githubusercontent.com/redhat-cop/openshift-templates/v1.4.9/jenkins-pipelines/jenkins-pipeline-template-no-ocp-triggers.yml \
    -p NAME=$(basename {} | tr 'A-Z' 'a-z') \
    -p PIPELINE_FILENAME=$(basename {}) \
    -p PIPELINE_CONTEXT_DIR=test \
    -p PIPELINE_SOURCE_REPOSITORY_URL=https://github.com/garethahealy/pipeline-library.git \
    -p PIPELINE_SOURCE_REPOSITORY_REF=$(git rev-parse --abbrev-ref HEAD)' \; | oc apply -f -

//git config --get remote.origin.url

## Create credentials
oc create secret generic my-token --from-literal=username=openshift --from-literal=password=$(oc whoami --show-token)
oc label secret my-token credential.sync.jenkins.openshift.io=true

## Link dockercfg to Jenkins service account
oc create secret generic --from-literal=registry=docker-registry.default.svc:5000 --from-literal=username=openshift --from-literal=token=$(oc whoami --show-token) local-registry-generic
oc secrets link --for=mount jenkins local-registry-generic

oc create secret docker-registry --docker-server=docker-registry.default.svc:5000 --docker-username=openshift --docker-password=$(oc whoami --show-token) --docker-email=unused local-registry
oc secrets link --for=mount jenkins local-registry

## Deploy Jenkins
oc process -p MEMORY_REQUEST=2Gi -p MEMORY_LIMIT=3Gi -f https://raw.githubusercontent.com/redhat-cop/openshift-templates/v1.4.9/jenkins/jenkins-persistent-template.yml | oc apply -f -
oc patch dc jenkins -p '{"apiVersion":"apps.openshift.io/v1","kind":"DeploymentConfig","metadata":{"name":"jenkins"},"spec":{"template":{"spec":{"containers":[{"name":"jenkins","resources":{"limits":{"cpu":"3"},"requests":{"cpu":"2"}}}]}}}}'

## Create global pipeline lib
curl --header "Authorization: Bearer $(oc whoami --show-token)" --data-urlencode "script=$(< test/create-pipeline-library.groovy)" https://$(oc get route jenkins -o jsonpath={.spec.host})/scriptText

## Give jenkins permissions in promotion project
oc policy add-role-to-user edit system:serviceaccount:pipelinelib-testing:jenkins -n pipelinelib-promotion-testing

## Start all BuildConfigs
find test -type f -name "Jenkinsfile-*" -exec bash -c 'oc start-build $(basename {} | tr 'A-Z' 'a-z')-pipeline' \;