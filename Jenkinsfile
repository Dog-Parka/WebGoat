pipeline {
    agent any

    options {
        timeout(time: 1, unit: 'HOURS') 
    }

    environment { 
        TIME_ZONE = 'Asia/Seoul' 

        // GitHub 관련 변수
        GIT_TARGET_BRANCH = 'develop' 
        GIT_REPOSITORY_URL = 'https://github.com/Dog-Parka/WebGoat.git' 
        GIT_CREDENTIALS_ID = 'Github_Dog-Parka' 

        // AWS ECR 관련 변수
        AWS_ECR_CREDENTIAL_ID = 'AWS_IAM(WHS_mins)' 
        AWS_ECR_URI = '688567267164.dkr.ecr.ap-northeast-2.amazonaws.com' 
        AWS_ECR_IMAGE_NAME = 'whs_test_repo' 
        AWS_REGION = 'ap-northeast-2' 
    }

    stages { 
        stage('init') { 
            steps {
                echo 'init stage' 
                deleteDir() 
            }
        }

        stage('Cloning Repository') { 
            steps {
                echo 'Cloning Repository' 
                git branch: "${GIT_TARGET_BRANCH}", 
                    credentialsId: "${GIT_CREDENTIALS_ID}",
                    url: "${GIT_REPOSITORY_URL}" 
            }
        }

        stage('Build Maven') {
            steps {
                echo 'Build Maven' 
                dir('.') { 
                    sh '''
                        chmod +x ./mvnw
                        ./mvnw spotless:apply
                        ./mvnw clean install -DskipTests
                    '''
                }
            }
        }

        stage('Build Docker Image') { 
            steps {
                script { 
                    sh '''
                        docker build -t ${AWS_ECR_IMAGE_NAME} .
                        docker tag ${AWS_ECR_IMAGE_NAME} ${AWS_ECR_URI}/${AWS_ECR_IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('Push to ECR') {
            steps {
              withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "${AWS_ECR_CREDENTIAL_ID}"]]) {
                    script {
                        sh '''
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_ECR_URI}
                        docker push ${AWS_ECR_URI}/${AWS_ECR_IMAGE_NAME}:latest
                        '''
                    }
                }
            }
        }

        stage('Clean Up Docker Images on Jenkins Server') { 
            steps {
                echo 'Cleaning up unused Docker images on Jenkins server'
                sh "docker image prune -f --all" 
            }
        }
    }

    post { 
        success {
            echo 'Pipeline succeeded' 
        }
        failure {
            echo 'Pipeline failed' 
        }
    }
}
