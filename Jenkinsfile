def podTemplate = """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: k8s
    image: 10.1.4.129:8082/repository/docker-repo/ose-jenkins-agent-maven:v4.10.0
    command:
    - sleep
    args:
    - infinity
  nodeSelector:
    kubernetes.io/hostname: localhost
  volumeMounts:
  - name: workspace-volume
    mountPath: /home/jenkins/agent
  workingDir: "/home/jenkins/agent"
volumes:
- name: "workspace-volume"
  persistentVolumeClaim:
    claimName: "jenkins-worker-pvc"
    readOnly: false
"""

pipeline {
    agent none
    stages {
        stage("Parallel") {
            parallel {
                stage("1.k8s") {
                    agent {
                        kubernetes {
                            yaml podTemplate
                            defaultContainer 'k8s'
                        }
                    }
                    steps {
                        sh """
                            /home/jenkins/agent/workspace/github-lohith/date.sh
                        """
                    }
                }
                stage("2.k8s") {
                    agent { label 'openshift' }
                    steps {
                        sh """
                            /home/jenkins/agent/workspace/github-lohith/date.sh
                        """
                    }
                }
                
            }
        }
    }
}
