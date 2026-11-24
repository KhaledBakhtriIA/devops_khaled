# Jenkins LTS avec JDK 17 (même version Java que le backend).
# Maven n'est pas installé : le pipeline utilise le wrapper ./mvnw du projet.
FROM jenkins/jenkins:lts-jdk17

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
