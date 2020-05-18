#! groovy

// used environment variables
// GIT_URL
// CLONE_BRANCH
// PIPELINE_BRANCH
// GIT_CREDENTIALS_ID
// APP_NAME
BUILD_CONFIG_NAME="dev-tasklist-s2i-build"
// DEV_NAMESPACE
TARGET_IMAGE_TAG=""
// TARGET_IMAGESTREAM_NAME
// APP_DOMAIN
// BUILD_TIMEOUT
// DEPLOYMENT_TIMEOUT
OBJECTS_FOLDER="openshift/objects/develop"

pipeline {
    agent {
        label 'maven'
    }

    stages {

        stage('Init') {
            steps {
                sh 'rm -rf src && mkdir src'
            } // steps
        } // stage

        stage('Clone') {
            steps {
                dir('src') {
                    script {
                        echo "Clone sources: ${CLONE_BRANCH} - ${GIT_URL}"
                        if("${GIT_CREDENTIALS_ID}" == "") {
                            git branch: "${CLONE_BRANCH}", url: "${GIT_URL}"
                        } else {
                            git branch: "${CLONE_BRANCH}", url: "${GIT_URL}", credentialsId: "${GIT_CREDENTIALS_ID}"
                        }
                    }
                }
                dir('cicd') {
                    script {
                        echo "Clone pipeline: ${PIPELINE_BRANCH} - ${GIT_URL}"
                        if("${GIT_CREDENTIALS_ID}" == "") {
                            git branch: "${PIPELINE_BRANCH}", url: "${GIT_URL}"
                        } else {
                            git branch: "${PIPELINE_BRANCH}", url: "${GIT_URL}", credentialsId: "${GIT_CREDENTIALS_ID}"
                        }
                    }
                }
            } // steps
        } // stage

        stage('Configure') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    APP_VERSION = (pom.version).replaceAll('-[A-Za-z]+', '')

                    TARGET_IMAGE_TAG="${APP_VERSION}-${env.BUILD_NUMBER}"
                }
            } // steps
        } // stage

        stage('BUILD - Maven build') {
            steps {
                dir('src') {
                    sh 'mvn clean package'
                }
            } // steps
        } // stage

        stage('BAKE - Bake application image') {
            steps {
                script {
                    openshift.withProject(DEV_NAMESPACE) {

                        def is = openshift.selector('is', "${TARGET_IMAGESTREAM_NAME}");
                        if(!is.exists()) {
                            openshift.create('-f', "cicd/${OBJECTS_FOLDER}/is-binary-s2i.yaml")
                        } else {
                            openshift.replace('-f', "cicd/${OBJECTS_FOLDER}/is-binary-s2i.yaml")
                        }

                        def bc = openshift.selector("bc/${BUILD_CONFIG_NAME}")
                        if(!bc.exists()) {
                            openshift.create('-f', "cicd/${OBJECTS_FOLDER}/bc-binary-s2i.yaml")
                        } else {
                            openshift.replace('-f', "cicd/${OBJECTS_FOLDER}/bc-binary-s2i.yaml")
                        } // if

                        bc.startBuild("--from-dir=src/target")
                        def builds = bc.related('builds')
                        // wait at most BUILD_TIMEOUT minutes for the build to complete
                        timeout(BUILD_TIMEOUT.toInteger()) {
                            builds.untilEach(1) {
                                return it.object().status.phase == 'Complete'
                            }
                        } // timeout

                        openshift.tag("${DEV_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:latest", "${DEV_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:${TARGET_IMAGE_TAG}")
                    } // withProject
                } // script
            } // steps
        } // stage

        stage('BUILD - Promote to DEV') {
            steps {
                script {
                    openshift.withProject(DEV_NAMESPACE) {
                        openshift.tag("${BUILD_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:${TARGET_IMAGE_TAG}", "${BUILD_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:toDev")
                    }
                } // script
            } // steps
        } // stage

        stage('DEV - Deploy') {
            steps {
                script {
                    openshift.withProject(DEV_NAMESPACE) {
                        openshift.tag("${BUILD_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:toDev", "${DEV_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:${TARGET_IMAGE_TAG}")

                        createPvc(DEV_NAMESPACE, 'dev-tasklist-data', APP_NAME, '1Gi')

                        def devDc = openshift.selector('dc', APP_NAME)
                        if(devDc.exists()) {
                            // apply from file
                            openshift.replace('-f', "cicd/${OBJECTS_FOLDER}/dev-deployment-config.yaml")
                        } else {
                            // create from file
                            openshift.create('-f', "cicd/${OBJECTS_FOLDER}/dev-deployment-config.yaml")
                        }
                        // patch image
                        dcmap = devDc.object()
                        dcmap.spec.template.spec.containers[0].image = "docker-registry.default.svc:5000/${DEV_NAMESPACE}/${TARGET_IMAGESTREAM_NAME}:${TARGET_IMAGE_TAG}"
                        openshift.apply(dcmap)

                        timeout(DEPLOYMENT_TIMEOUT.toInteger()) {
                            def rm = devDc.rollout()
                            rm.latest()
                            rm.status()
                        } // timeout

                        def devSvc = openshift.selector('svc', APP_NAME)
                        if(devSvc.exists()) {
                            openshift.replace('-f', "cicd/${OBJECTS_FOLDER}/dev-svc.yaml")
                        } else {
                            openshift.create('-f', "cicd/${OBJECTS_FOLDER}/dev-svc.yaml")
                        }

                        createSecureRoute(DEV_NAMESPACE, APP_NAME, '/csv', APP_DOMAIN)
                    } // withProject
                } // script
            } // steps
        } // stage

        stage('DEV - Run tests') {
            steps {
                script {
                    sleep 10
                    testEndpointResponse("https://${APP_NAME}-${DEV_NAMESPACE}.${APP_DOMAIN}/tasklist/api/v1/hello/pipeline", '{"hello": "pipeline!"}', 10, 30)
                } //
            } // steps
        } // stage

    } // stages

} // pipeline

/**
 * Test that http page from url contains given text
 *
 * @param url endpoint url
 * @param text text to search for
 * @param wait number of minutes to wait until timeout, default is 10
 * @param pollInterval number of seconds to sleep between retries, default is 30
 */
def testEndpointResponse(url, text, wait=10, pollInterval=30) {
    def cont = true
    timeout(wait) {
        while(cont) {
            def response = sh script:"curl -s -k ${url}", returnStdout: true
            if(response.contains("${text}")) {
                cont = false
                echo "Success!"
            } else {
                sleep pollInterval
            }
        } // while
    } // timeout
}

/**
 * Creates a persistent volume claim (pvc)
 *
 * @param namespace namespace to create the pvc in
 * @param name name of pvc
 * @param appName name of application
 * @param size size of pvc
 */
def createPvc(namespace, name, appName, size) {
    openshift.withProject(namespace) {
        openshift.apply("cicd/${OBJECTS_FOLDER}/pvc.yaml")
    } // withProject
}

/**
 * Creates a https route with a template to a given namespace if it does not exists
 *
 * @param namespace namespace to crete the route in
 * @param applicationName
 * @param contextRoot http context root for the application
 * @param appDomain openshift applications domain
 */
def createSecureRoute(namespace, applicationName, contextRoot, appDomain) {
    openshift.withProject(namespace) {
        def route = openshift.selector('route', "${applicationName}-secure");
        if(!route.exists()) {
            def routeObj = openshift.process(readFile(file:'src/openshift/templates/secure-route-template.yaml'),
                    '-p', "APP_NAME=${applicationName}",
                    '-p', "APP_NAMESPACE=${namespace}",
                    '-p', "APP_CONTEXT_ROOT=${contextRoot}",
                    '-p', "APP_DOMAIN=${appDomain}")
            openshift.create(routeObj)
        }
    }
}
