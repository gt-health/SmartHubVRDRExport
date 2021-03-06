#!/usr/bin/env groovy
pipeline{
    agent any

    environment {
      GTRI_IMAGE_REGISTRY = credentials('gtri-image-registry')
      GTRI_RANCHER_API_ENDPOINT = credentials('gtri-rancher-api-endpoint')
      GTRI_HDAP_ENV_ID = credentials('hdap-aws-rancher-env')
      CLARITYNLP_DOCKERHUB_CREDS = 'claritynlp-dockerhub'
      paasImage = ''
    }

    stages{
      stage('Building image') {
        steps{
          script {
            paasImage = docker.build("claritynlp/vrdrexport:1.0", ".")
          }
        }
      }
      stage('Push image to private registry'){
        steps{
          script{
            docker.withRegistry("https://${GTRI_IMAGE_REGISTRY}"){
              paasImage.push("latest")
            }
          }
        }
      }
      stage('Push image to public registry'){
        steps{
          script{
            docker.withRegistry('', CLARITYNLP_DOCKERHUB_CREDS){
              paasImage.push("latest")
            }
          }
        }
      }
      stage('Notify orchestrator'){
        steps{
          script{
            rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: "${GTRI_IMAGE_REGISTRY}/claritynlp/vrdrexport:latest", ports: '', service: 'SmartHub/VRDRExport', timeout: 600
          }
        }
      }
    }
}