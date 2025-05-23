pipeline { // 파이프라인 블록 시작
    agent any // 어떤 에이전트에서도 실행 가능

    options {
        timeout(time: 1, unit: 'HOURS') // 전체 파이프라인 최대 실행 시간을 1시간으로 제한
    }

    environment { // 전역 환경 변수 선언
        TIME_ZONE = 'Asia/Seoul' // 타임존 설정

        // GitHub 관련 변수
        GIT_TARGET_BRANCH = 'develop' // 클론할 Git 브랜치 이름
        GIT_REPOSITORY_URL = 'https://github.com/Dog-Parka/WebGoat.git' // Git 리포지토리 URL
        GIT_CREDENTIALS_ID = 'Github_Dog-Parka' // Jenkins에 등록된 Git 인증 정보 ID

        // AWS ECR 관련 변수
        AWS_ECR_CREDENTIAL_ID = 'AWS_IAM(WHS_mins)' // Jenkins에 등록된 AWS 인증 정보 ID
        AWS_ECR_URI = '688567267164.dkr.ecr.ap-northeast-2.amazonaws.com' // AWS ECR 리포지토리 URI (ex: 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com)
        AWS_ECR_IMAGE_NAME = 'whs_test_repo' // ECR에 저장할 Docker 이미지 이름
        AWS_REGION = 'ap-northeast-2' // AWS 리전 (ex: ap-northeast-2)

        // ✅ [필수 수정] CodeDeploy + S3 정보
        S3_BUCKET = 'webgoat-test-bucket'
        APPLICATION_NAME = 'webgoat-deploy'               // CodeDeploy 애플리케이션 이름
        DEPLOYMENT_GROUP = 'codedeploy-group'    // CodeDeploy 배포 그룹 이름
    }

    stages { // 실제 작업이 정의되는 단계들
        stage('init') { // 초기화 단계
            steps {
                echo 'init stage' // 초기화 로그 출력
                deleteDir() // 작업 공간 전체 삭제 (클린 빌드를 위해)
            }
        }

        stage('Cloning Repository') { // Git 리포지토리 클론 단계
            steps {
                echo 'Cloning Repository' // 로그 출력
                git branch: "${GIT_TARGET_BRANCH}", // 지정된 브랜치를
                    credentialsId: "${GIT_CREDENTIALS_ID}", // 인증 정보를 사용해
                    url: "${GIT_REPOSITORY_URL}" // 지정된 리포지토리에서 클론
            }
        }

        stage('Build Maven') { // Maven 빌드 단계
            steps {
                echo 'Build Maven' // 로그 출력
                dir('.') { // 현재 디렉터리에서 쉘 명령 실행
                    sh '''
                        chmod +x ./mvnw
                        ./mvnw spotless:apply
                        ./mvnw clean install -DskipTests
                    '''
                }
            }
        }

        stage('Build Docker Image') { // Docker 이미지 빌드 단계
            steps {
                script { // 스크립트 블록 사용
                    sh '''
                        docker build -t ${AWS_ECR_IMAGE_NAME} .
                        docker tag ${AWS_ECR_IMAGE_NAME} ${AWS_ECR_URI}/${AWS_ECR_IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('Push to ECR') { // ECR에 Docker 이미지 푸시
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

        stage('Patch Task Definition') {
            steps {
                script {
                    // taskdef.json의 <IMAGE_URI>를 실제 Docker 이미지 URI로 치환
                    sh """
                        sed 's|<IMAGE_URI>|${AWS_ECR_URI}/${AWS_ECR_IMAGE_NAME}:latest|' taskdef.json > taskdef-patched.json
                    """
                }
            }
        }

        stage('Package and Upload to S3') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "${AWS_ECR_CREDENTIAL_ID}"]]) {
                    script {
                        sh """
                            zip -r deploy_bundle.zip appspec.yaml taskdef-patched.json
                            aws s3 cp deploy_bundle.zip s3://${S3_BUCKET}/deploy_bundle-${BUILD_NUMBER}.zip --region ${AWS_REGION}
                        """
                    }
                }
            }
        }

        stage('Trigger CodeDeploy') {
            steps {
                script {
                    // CodeDeploy 배포 시작 (블루그린 방식으로 ECS 서비스에 태스크 정의 적용)
                    sh """
                        aws deploy create-deployment \
                          --application-name ${APPLICATION_NAME} \
                          --deployment-group-name ${DEPLOYMENT_GROUP} \
                          --s3-location bucket=${S3_BUCKET},bundleType=zip,key=deploy_bundle-${BUILD_NUMBER}.zip \
                          --region ${AWS_REGION}
                    """
                }
            }
        }

        stage('Clean Up Docker Images on Jenkins Server') { // Jenkins 서버의 Docker 이미지 정리
            steps {
                echo 'Cleaning up unused Docker images on Jenkins server' // 로그 출력
                sh "docker image prune -f --all" // 사용되지 않는 모든 이미지 강제 삭제
            }
        }
    }

    post { // 파이프라인 완료 후 실행
        success {
            echo 'Pipeline succeeded' // 성공 메시지 출력
        }
        failure {
            echo 'Pipeline failed' // 실패 메시지 출력
        }
    }
}
