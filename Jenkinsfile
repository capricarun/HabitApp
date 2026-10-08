pipeline {
    agent any

    tools {
        maven 'maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        githubPush()
    }

    environment {
        DOCKERHUB_USER = 'capricarun'
        GITOPS_REPO = 'github.com/capricarun/habitapp-gitops.git'
        GIT_EMAIL = 'jenkins@example.com'
        IMAGE_NAME = "${DOCKERHUB_USER}/habit-tracker"
        VALUES_FILE = 'charts/habit-tracker/values.yaml'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT_SHA = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: false,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/habit-tracker.jar',
                                 fingerprint: true
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        mvn -B sonar:sonar \
                          -Dsonar.projectKey=habit-tracker \
                          -Dsonar.projectName="Habit Tracker" \
                          -Dsonar.host.url=$SONAR_HOST_URL \
                          -Dsonar.token=$SONAR_AUTH_TOKEN
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Image Versioning') {
            steps {
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT_SHA}"
                }
                echo "Image version: ${IMAGE_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE_NAME:$IMAGE_TAG -t $IMAGE_NAME:latest .'
            }
        }

        stage('Push Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DH_USER',
                        passwordVariable: 'DH_TOKEN'
                    )
                ]) {
                    sh '''
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        docker push $IMAGE_NAME:$IMAGE_TAG
                        docker push $IMAGE_NAME:latest
                    '''
                }
            }
        }

        stage('Update Helm Configuration') {
            steps {
                sh '''
                    rm -rf gitops
                    git clone --depth 1 https://$GITOPS_REPO gitops
                    cd gitops
                    python3 - <<'PYTHON'
from pathlib import Path
import os

path = Path(os.environ['VALUES_FILE'])
lines = path.read_text().splitlines()

for i, line in enumerate(lines):
    if line.startswith('  tag:'):
        lines[i] = '  tag: "' + os.environ['IMAGE_TAG'] + '"'

path.write_text('\n'.join(lines) + '\n')
PYTHON
                    echo "---- updated image section ----"
                    grep -A3 '^image:' "$VALUES_FILE"
                '''
            }
        }

        stage('Commit Deployment Change') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'github-creds',
                        usernameVariable: 'GH_USER',
                        passwordVariable: 'GH_TOKEN'
                    )
                ]) {
                    sh '''
                        cd gitops
                        git config user.name "jenkins-ci"
                        git config user.email "$GIT_EMAIL"
                        git add $VALUES_FILE

                        git diff --cached --quiet && {
                            echo "No change to commit"
                            exit 0
                        }

                        git commit -m "ci: deploy habit-tracker $IMAGE_TAG (build #$BUILD_NUMBER)"
                        git push https://$GH_USER:$GH_TOKEN@$GITOPS_REPO HEAD:main
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "CI done. Argo CD will deploy ${IMAGE_NAME}:${IMAGE_TAG} from the GitOps repo."
        }

        failure {
            echo 'Pipeline failed - check the stage logs above.'
        }

        always {
            sh 'docker logout || true'
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}
