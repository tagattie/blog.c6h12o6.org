#! /usr/bin/env groovy

pipeline {
    agent any
    stages {
        stage('Checkout Jenkinsfile and save.') {
            steps {
                timestamps {
                    checkout scm
                    archiveArtifacts 'Jenkinsfile.groovy'
                }
            }
        }

        stage('Build blog posts.') {
            steps {
                timestamps {
                    sh "hugo"
                }
            }
        }

        stage('Sync to blog server.') {
            steps {
                timestamps {
                    sshagent (credentials: [sshCredential]) {
                        sh """
rsync -va --delete --stats public/ ${sshUser}@${sshHost}:/usr/local/www/blog/
"""
                    }
                }
            }
        }
    }
}
