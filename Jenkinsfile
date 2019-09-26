pipeline {
    agent {
        kubernetes {
            label 'jenkins-builder'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
labels:
  component: ci
spec:
  # Use service account that can deploy to all namespaces
  serviceAccountName: cd-jenkins
  containers:
  - name: base-node-builder
    image: gcr.io/bitclave-jenkins-ci/base-node-builder
    command:
    - cat
    tty: true
  - name: gcloud
    image: gcr.io/cloud-builders/gcloud
    command:
    - cat
    tty: true
  - name: kubectl
    image: gcr.io/cloud-builders/kubectl
    command:
    - cat
    tty: true
"""
        }
    }
    environment {
        // CI = 'true' 
        // PROJECT = "bitclave-jenkins-ci"
        PROJECT = "bitclave-base"
        APP_NAME = "base-node"
        FE_SVC_NAME = "${APP_NAME}-frontend"
        CLUSTER = "base-first"
        CLUSTER_ZONE = "us-central1-f"
        BRANCH_NAME = "develop"
        IMAGE_TAG = "gcr.io/bitclave-jenkins-ci/${APP_NAME}:${env.BRANCH_NAME}.${env.BUILD_NUMBER}"
        JENKINS_CRED = "bitclave-jenkins-ci"
    }
    triggers {
        upstream(upstreamProjects: 'base-node-builder/master', threshold: hudson.model.Result.SUCCESS)
    }
    stages {
        stage('Install') {
            steps {
                sh 'echo hello'
                container('base-node-builder') {
                    sh "node --version"
                    sh "npm --version"
                    sh "java -version"
                    sh "ganache-cli --version"
                    sh "./gradlew -v"

                    sh './start-ganache.sh > /dev/null &'
                    // sh 'export PATH=$PATH:./node_modules/.bin/ganache-cli && echo $PATH && source ganache-cli --version > /dev/null &'
                    sh "sleep 5"

                }
            }
        }

//        stage('Test') {
//            steps {
//                container('base-node-builder') {
//                    sh './gradlew check --stacktrace'
//                }
//            }
//        }

        stage('Build JAR') {
            steps {
                container('base-node-builder') {
                    // sh "ls -l"
                    // sh "mkdir -p build/libs"
                    // sh "echo aaa > build/libs/base-node.jar"
                    sh './gradlew build --exclude-task test'
                    // this is required since we need to upload base-node.jar to the cloud for build but
                    // build directory is excluded from the upload by .gcloadignore file
                    sh 'cp build/libs/base-node.jar .'
                }
            }
        }

        stage('Build Container') {
            steps {
                sh 'printenv | grep -i branch'
                sh 'echo ${IMAGE_TAG}'

                container('gcloud') {
                    sh "ls -l"
                    sh "PYTHONUNBUFFERED=1 gcloud builds submit -t ${IMAGE_TAG} ."
                }
            }
        }

        stage('Deploy Staging') {
            // Production branch
            steps {
                container('kubectl') {
                    // Change deployed image in production to the one we just built
                    sh("gcloud config get-value account")
                    step([$class: 'KubernetesEngineBuilder', namespace: 'staging', projectId: env.PROJECT, clusterName: env.CLUSTER, zone: env.CLUSTER_ZONE, manifestPattern: 'k8s/services', credentialsId: env.JENKINS_CRED, verifyDeployments: false])
                    step([$class: 'KubernetesEngineBuilder', namespace: 'staging', projectId: env.PROJECT, clusterName: env.CLUSTER, zone: env.CLUSTER_ZONE, manifestPattern: 'k8s/staging', credentialsId: env.JENKINS_CRED, verifyDeployments: false])
                    sleep 10 // seconds
                    sh("gcloud container clusters get-credentials base-first --zone us-central1-f --project bitclave-base")
                    sh("echo `kubectl --namespace=staging get service/${FE_SVC_NAME} -o jsonpath='{.status.loadBalancer.ingress[0].ip}'`")
                }
            }
        }
        stage('Time to access the app') {
            steps {
                echo 'Waiting 3 minutes for deployment to complete prior starting smoke testing'
                sleep 100 // seconds
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'Dockerfile', fingerprint: true
            archiveArtifacts artifacts: 'Jenkinsfile', fingerprint: true
        }
    }
}
