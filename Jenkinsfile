// Pipeline CI du backend Spring Boot
// Checkout -> Build (.jar) -> Tests -> SonarQube -> Quality Gate -> Nexus
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    // Lien Git -> Jenkins : Jenkins vérifie GitHub toutes les ~5 min
    // et lance un build automatiquement s'il y a un nouveau commit.
    // (Un webhook GitHub ne peut pas joindre un Jenkins en localhost.)
    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        // Versionning du livrable : 1.0.1, 1.0.2, ... (un numéro par build Jenkins)
        VERSION_BASE = '1.0'
        APP_VERSION  = "${VERSION_BASE}.${BUILD_NUMBER}"

        // Nom du serveur déclaré dans  Manage Jenkins > System > SonarQube servers
        SONAR_SERVER = 'SonarQube'

        // URL de Nexus vue depuis Jenkins (Jenkins dans WSL, Nexus dans Docker sur le port 8082)
        NEXUS_URL = 'http://localhost:8082'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Version du livrable : ${APP_VERSION}"
            }
        }

        stage('Build - mvn clean package') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw'
                    sh './mvnw -B clean package -DskipTests -Drevision=$APP_VERSION'
                }
            }
            post {
                success {
                    // Le .jar généré dans backend/target est attaché au build Jenkins
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Tests - mvn test') {
            steps {
                dir('backend') {
                    sh './mvnw -B test -Drevision=$APP_VERSION'
                }
            }
            post {
                always {
                    // Résultats JUnit (onglet "Test Result" + graphique de tendance)
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                    // Rapport de couverture HTML JaCoCo téléchargeable
                    archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Qualité - SonarQube') {
            steps {
                dir('backend') {
                    // Injecte l'URL et le token SonarQube configurés dans Jenkins
                    withSonarQubeEnv("${SONAR_SERVER}") {
                        sh './mvnw -B sonar:sonar -Drevision=$APP_VERSION'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // Attend le verdict de SonarQube (webhook) ; échec du pipeline si "Failed"
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Livraison - Nexus') {
            steps {
                dir('backend') {
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                                      usernameVariable: 'NEXUS_USER',
                                                      passwordVariable: 'NEXUS_PASS')]) {
                        sh './mvnw -B -s ../ci/maven-settings.xml deploy -DskipTests -Drevision=$APP_VERSION -Dnexus.url=$NEXUS_URL'
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Livrable tn.esprit:backend:${APP_VERSION} publié dans Nexus (maven-releases)."
        }
        failure {
            echo 'Le pipeline a échoué : consulter les logs de l\'étape en rouge.'
        }
    }
}
