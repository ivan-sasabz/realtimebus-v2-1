pipeline {
    agent {
        dockerfile {
            filename 'docker/dockerfile-node'
            additionalBuildArgs '--build-arg JENKINS_USER_ID=`id -u jenkins` --build-arg JENKINS_GROUP_ID=`id -g jenkins`'
        }
    }

    environment {
        CONFIG_FILE=credential('realtimebus_config')
        PRODUCTION_SERVER=credential('realtimebusV2-prod-ip')
    }

    stages {
        stage('Dependencies & Build') {
            steps {
                sh 'npm install'
            }
        }
        /*TODO write tests before you execute them
        stage('Test') {
            steps {
                sh ''
            }
        }*/
        stage('Configure') {
            steps {
                sh 'cat ${CONFIG_FILE} > local-config.js'
            }
        }
        stage('Archive') {
            steps {
                sh 'tar -czf build.tar.gz --exclude=build.tar.gz --exclude "./.*" .'
            }
        }
        stage('Deploy'){
            steps{
                sh 'scp build.tar.gz ${PRODUCTION_SERVER}:./'
                //backup last deployment and deploy new one
                sh '''
                    ssh ${PRODUCTION_SERVER} tar -czf $(date '+%Y-%m-%d').realtimebus.tar.gz realtimebus-v2/ &&  mkdir tmp && tar -C tmp -xzvf build.tar.gz && mv tmp realtimebus-v2
                '''
                sh 'sudo systemctl restart realtimebus.service'
            }
        }
    }
}
