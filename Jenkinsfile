import groovy.json.JsonSlurper

    // BackOffice API
    //APP_NAME = "backoapi"
    APP_NAME = "${env.gitlabSourceRepoName}"
   // def GIT_REPO_URL = "${env.gitlabSourceRepoHttpUrl}"
    def GIT_REPO_URL = "https://10.1.4.173/lohith.s/openshift-new.git/"
    def GIT_REPO_BRANCH = "${env.gitlabBranch}"
    def GIT_REPO_SECRET = 'lohith'

    def APP_TEST_PROFILE = "test"
    def APP_PROD_PROFILE = "prod"
    def TEST_NAMESPACE = 'crm-batch-test'
    def PROD_NAMESPACE = 'crm-batch-prod'

    def TEST_CERT_PATH = 'certificates/test'
    def PROD_CERT_PATH = 'certificates/prod'
    def APP_TEST_HOST = "${APP_NAME}.crm-mstest.omantel.om"
    def APP_PROD_HOST = "${APP_NAME}.crm.omantel.om"
    //def APP_PROD_REPLICAS = 2

    def MAVEN_MIRROR_URL = "http://10.164.152.60:8082/artifactory/libs-release/"
    def MAVEN_ARGS = "-f pom.xml -s settings-maven.xml"

    def SONAR_HOST = "https://sonarqube-sonar-prod.apps.ocpprod.otg.om"
    def SONAR_LOGIN = "${env.SONAR_LOGIN}"
    def TO_EMAIL = "yaqoob.shaaibi@omantel.om"
    //def CC_EMAIL = "Mohammed.Maqbali@omantel.om"
    def CC_EMAIL = "younis.abri@omantel.om"
    
    def FROM_EMAIL = "gitlab@gitlab.omantel.om"
    //def USER_ID = "${env.gitlabUserUsername}"
    //def USER_NAME = "${env.gitlabUserName}"
    //def FROM_EMAIL = "yaqoob.shaaibi@omantel.om"

    def nodeLabel = "${APP_NAME}-${UUID.randomUUID().toString()}"

	def commitId =""
	def commitMsg=""
	def gitAuthor=""
	def authorEmail=""
	def qualityCheck=false
	def taskStatusResult=""
	def projectStatusResult=""


    //Note: do not modify code after this line.

    def readPom(){
		GROUP_ID = readMavenPom().getGroupId()
        ARTIFACT_ID = readMavenPom().getArtifactId()
        VERSION = readMavenPom().getVersion()
        PACKAGING = readMavenPom().getPackaging()
        APP_JAR = "${ARTIFACT_ID}-${VERSION}.${PACKAGING}"
    }


    def deployApp(APP_PROFILE, CERT_PATH, APP_HOST){
            openshift.raw("new-app ${APP_NAME}:latest --as-deployment-config -e JAVA_APP_JAR=${APP_JAR} -l 'app=${APP_NAME}' -e AB_JOLOKIA_OFF=true -e SPRING_PROFILES_ACTIVE=${APP_PROFILE} -e TZ=Asia/Muscat")
            //openshift.raw("expose svc/${APP_NAME} --hostname=${APP_HOST}")
            openshift.raw("create route edge ${APP_NAME} --service=${APP_NAME} --cert=${CERT_PATH}/tls.crt --key=${CERT_PATH}/tls.key --insecure-policy=Redirect --hostname=${APP_HOST}")
            openshift.raw("annotate routes ${APP_NAME} haproxy.router.openshift.io/disable_cookies='true'")
            openshift.raw("annotate routes ${APP_NAME} haproxy.router.openshift.io/balance='roundrobin'")

            openshift.raw("set probe dc/${APP_NAME} --readiness --get-url=http://localhost:8080/health --initial-delay-seconds=30 --timeout-seconds=20 --failure-threshold=3 --period-seconds=10")
            openshift.raw("set probe dc/${APP_NAME} --liveness  --get-url=http://localhost:8080/health --initial-delay-seconds=180 --timeout-seconds=20 --failure-threshold=3 --period-seconds=30")

            def dc = openshift.selector("dc", "${APP_NAME}")
            timeout(5) {
                while (dc.object().spec.replicas != dc.object().status.availableReplicas) {
                    sleep 5
                }
            }
    }

  podTemplate(label: nodeLabel, cloud: "openshift", serviceAccount: "jenkins",
  nodeSelector: 'kubernetes.io/hostname=localhost',
			containers: [
				containerTemplate(name: "jnlp", image: "jenkins/inbound-agent:3148.v532a_7e715ee3-1", alwaysPullImage: false, args: '${computer.jnlpmac} ${computer.name}'),
				containerTemplate(
					name: "maven",
					image: "10.1.4.129:8082/repository/docker-repo/custom-maven:latest",
					alwaysPullImage: false,
					ttyEnabled: true,
					command: 'sleep',
					args: '99d',
					envVars: [
						containerEnvVar(key: 'MAVEN_MIRROR_URL', value: "${MAVEN_MIRROR_URL}"),
						containerEnvVar(key: 'MAVEN_ARGS', value: "${MAVEN_ARGS}")
					]
				)
            ]){
            node(nodeLabel) {
                try{

                    stage('Download') {

                        def scmVars = git(url: GIT_REPO_URL, branch: GIT_REPO_BRANCH, credentialsId: GIT_REPO_SECRET)
                        readPom()

                        commitId = scmVars.GIT_COMMIT
                        commitMsg = sh (script: 'git log -1 --pretty=%B ${GIT_COMMIT}', returnStdout: true).trim()
                        gitAuthor = sh (script: 'git log -1 --pretty=%cn ${GIT_COMMIT}', returnStdout: true).trim()
                        gitAuthor = sh(returnStdout: true, script: 'git log --format="%an" | head -1')
                        authorEmail = sh(returnStdout: true, script: 'git log --format="%ae" | head -1')
                        //Gitlab API will skip build when adding [ci-skip] as commit message
                    }

                    stage ('Notify: START') {
                        echo "Sending pipeline start notification..."
                        mail (from: "${FROM_EMAIL}", to: "${TO_EMAIL}", cc: "${CC_EMAIL}", subject: "STARTED: ${APP_NAME} - Pipeline ('${env.BUILD_NUMBER}')", mimeType: "text/html", body: " Project Name: ${APP_NAME} <br>Branch Name: ${GIT_REPO_BRANCH} <br>Commit Message: ${commitMsg} <br>Commit Author: ${gitAuthor} / ${authorEmail} <br>Job name: '${env.JOB_NAME}' <br>See '${env.BUILD_URL}' for details");
                    }
                   
                    if (env.gitlabTargetBranch == "test") {

                        stage('Build The App') {
                            container('maven') {
                                stage("Maven Build"){
                                    sh "mvn --version"
                                    sh "mvn ${MAVEN_ARGS} clean package -DskipTests"
                                }
                            }
                        }

                        stage('Test The App') {
                            container('maven') {
                                stage("Maven Test"){
                                    sh "mvn ${MAVEN_ARGS} -Doracle.jdbc.timezoneAsRegion=false -Dspring.profiles.active=test test"
                                }
                            }
                            junit allowEmptyResults: true, skipPublishingChecks: true, testResults: 'target/surefire-reports/*.xml'
                            publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')]
                        }

                        stage('Code Quality Analysis') {
                            container('maven') {
                                stage("SonarQube Analysis"){
                                    withSonarQubeEnv(installationName: 'sonar-prod') {
                                        sh "mvn ${MAVEN_ARGS} org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar -Dsonar.host.url=${SONAR_HOST}/  -Dsonar.login=${SONAR_LOGIN} -Dsonar.coverage.exclusions=** -Djavax.net.ssl.trustStore=sonarstore.jks -Djavax.net.ssl.trustStorePassword=changeit -DskipTests=true"
                                    }
                                }
                            }
                        }

                        stage("Quality Gate"){
                            container('maven') {
                                timeout(time: 5, unit: 'MINUTES') {
                                    def reportFilePath = "target/sonar/report-task.txt"
                                    def reportTaskFileExists = fileExists "${reportFilePath}"
                                    if (reportTaskFileExists) {
                                        echo "Found report task file"
                                        def taskProps = readProperties file: "${reportFilePath}"
                                        echo "taskId[${taskProps['ceTaskId']}]"
                                        while (true) {
                                            sleep 10
                                            taskStatusResult =
                                                sh(returnStdout: true,
                                                script: "curl -X GET -u ${SONAR_LOGIN}: --insecure \'${SONAR_HOST}/api/ce/task?id=${taskProps['ceTaskId']}\'")
                                            echo "taskStatusResult[${taskStatusResult}]"
                                            def taskStatus  = new JsonSlurper().parseText(taskStatusResult).task.status
                                            echo "taskStatus[${taskStatus}]"

                                            if (taskStatus == "SUCCESS") {
                                                projectStatusResult =
                                                sh(returnStdout: true,
                                                script: "curl -X GET -u ${SONAR_LOGIN}: --insecure \'${SONAR_HOST}/api/qualitygates/project_status?projectKey=${GROUP_ID}:${ARTIFACT_ID}\'")
                                                echo "projectStatusResult[${projectStatusResult}]"
                                                def projectStatus  = new JsonSlurper().parseText(projectStatusResult).projectStatus.status
                                                echo "projectStatus[${projectStatus}]"

                                                if (projectStatus != "SUCCESS" && projectStatus != "OK") {
                                                    qualityCheck=true
                                                    error "Pipeline aborted due to quality gate failure"
                                                }
                                                break
                                            } else if (taskStatus != "IN_PROGRESS" && taskStatus != "PENDING") {
                                                qualityCheck=true
                                                error "Pipeline aborted due to quality gate failure"
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        stage('Build App Image') {// Build Container Image
                            script {
                                openshift.withCluster() {
                                    openshift.withProject(TEST_NAMESPACE) {
                                        echo "Creating image builder..."
                                        def bcExists = openshift.selector("bc", "${APP_NAME}").exists()
                                        if (!bcExists) {
                                            //openshift.newBuild("--name=${APP_NAME} -l 'app=${APP_NAME}' ", "--image-stream=redhat-openjdk18-openshift:1.8", "--binary")
                                            openshift.newBuild("--name=${APP_NAME} -l 'app=${APP_NAME}' ", "--image-stream=java:openjdk-8-ubi8", "--binary")
                                        }else{
                                            echo "An Image builder for this app already exists, skipped..."
                                        }
                                        def buildExists = openshift.selector("builds", [ app : "${APP_NAME}" ]).exists()
                                        if(buildExists){
                                            echo "Deleting existing builds for app ${APP_NAME}"
                                            openshift.selector("builds", [ app : "${APP_NAME}" ]).delete()
                                        }
                                        echo "Starting new build for app ${APP_NAME}"
                                        openshift.selector("bc", "${APP_NAME}").startBuild("--from-file=target/${APP_JAR}","--wait")
                                    }
                                }
                            }
                        }

                        stage('Deploy to Test') {
                            script {
                                openshift.withCluster() {
                                    openshift.withProject(TEST_NAMESPACE) {
                                        if(!openshift.selector("dc", "${APP_NAME}").exists()){
                                            deployApp(APP_TEST_PROFILE, TEST_CERT_PATH, APP_TEST_HOST)
                                        } else {
                                            openshift.raw("rollout status dc/${APP_NAME} --watch --timeout=5m")
                                        }
                                    }
                                }
                            }
                        }

                        stage ('Notify: TEST') {
                            echo "Sending test deployment notification..."
                            mail (from: "${FROM_EMAIL}", to: "${TO_EMAIL}", cc: "${CC_EMAIL}", subject: "DEPLOYED IN TEST: ${APP_NAME} - Pipeline ('${env.BUILD_NUMBER}')", mimeType: "text/html", body: " Project Name: ${APP_NAME} <br>Branch Name: ${GIT_REPO_BRANCH} <br>Commit Message: ${commitMsg} <br>Commit Author: ${gitAuthor} / ${authorEmail} <br>Job name: '${env.JOB_NAME}' <br>See '${env.BUILD_URL}' for details");
                        }

                    }
                    // #### Production deployment:
                    if (env.gitlabTargetBranch == "master") {
                        def proceed = true
                        stage('Promote to Production?') {
                            try {
                                timeout(time: 30, unit: 'SECONDS') {
                                    input message: "Promote to Production ?", ok: "YES"
                                }
                            } catch (err) {
                                proceed = false
                            }
                        }

                        if(proceed) {

                            stage('Tag image') {
                                script {
                                    openshift.withCluster() {
                                        openshift.tag("${TEST_NAMESPACE}/${APP_NAME}:latest", "${PROD_NAMESPACE}/${APP_NAME}:latest")
                                    }
                                }
                            }

                            stage('Deploy to Prod') {
                                script {
                                    openshift.withCluster() {
                                        openshift.withProject(PROD_NAMESPACE) {
                                            if(!openshift.selector("dc", "${APP_NAME}").exists()){
                                                deployApp(APP_PROD_PROFILE, PROD_CERT_PATH, APP_PROD_HOST)
                                            } else {
                                                openshift.raw("rollout status dc/${APP_NAME} --watch --timeout=5m")
                                            }
                                        }
                                    }
                                }
                            }

                            stage ('Notify: PROD') {
                                echo "Sending prod deployment notification..."
                                mail (from: "${FROM_EMAIL}", to: "${TO_EMAIL}", cc: "${CC_EMAIL}", subject: "COMPLETED IN PROD: ${APP_NAME} - Pipeline ('${env.BUILD_NUMBER}')", mimeType: "text/html", body: " Project Name: ${APP_NAME} <br>Branch Name: ${GIT_REPO_BRANCH} <br>Commit Message: ${commitMsg} <br>Commit Author: ${gitAuthor} / ${authorEmail} <br>Job name: '${env.JOB_NAME}' <br>See '${env.BUILD_URL}' for details");
                            }
                        }
                    }

                }catch(e){

                    if (qualityCheck) {
                        currentBuild.result="QUALITY FAILED"
                        mail (from: "${FROM_EMAIL}", to: "${TO_EMAIL}", cc: "${CC_EMAIL}", subject: "FAILED: ${APP_NAME} - Pipeline ('${env.BUILD_NUMBER}')", mimeType: "text/html", body: " Project Name: ${APP_NAME} <br>Branch Name: ${GIT_REPO_BRANCH} <br>Commit Message: ${commitMsg} <br>Commit Author: ${gitAuthor} / ${authorEmail} <br>Job name: '${env.JOB_NAME}' <br>See '${env.BUILD_URL}' for details <br><br> Quality Gate Result: ${taskStatusResult} <br><br> ${projectStatusResult}");
                    } else {
                        currentBuild.result="FAILED"
                        mail (from: "${FROM_EMAIL}", to: "${TO_EMAIL}", cc: "${CC_EMAIL}", subject: "FAILED: ${APP_NAME} - Pipeline ('${env.BUILD_NUMBER}')", mimeType: "text/html", body: " Project Name: ${APP_NAME} <br>Branch Name: ${GIT_REPO_BRANCH} <br>Commit Message: ${commitMsg} <br>Commit Author: ${gitAuthor} / ${authorEmail} <br>Job name: '${env.JOB_NAME}' <br>See '${env.BUILD_URL}' for details");
                    }
                    throw e
                }
            } // node
  }
