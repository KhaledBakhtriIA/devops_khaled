# Mise en place de la CI : Jenkins + SonarQube + Nexus

Le pipeline exécute les étapes suivantes :

```
Checkout → Build (mvn clean package → backend/target/*.jar) → Tests (mvn test + JaCoCo)
         → SonarQube (mvn sonar:sonar) → Quality Gate → Nexus (mvn deploy, version 1.0.<BUILD_NUMBER>)
```

## 0. Vérifier en local (sans Jenkins)

```bash
cd backend
./mvnw clean package -DskipTests   # → target/backend-1.0.0-SNAPSHOT.jar
./mvnw test                        # → 25 tests, base H2 en mémoire (pas de MySQL)
                                   # → rapport de couverture : target/site/jacoco/index.html
```

## 1. Démarrer les outils

Docker Desktop doit être lancé.

```bash
cd ci
docker compose up -d --build
```

| Outil | URL navigateur | URL entre conteneurs |
|---|---|---|
| Jenkins | http://localhost:8088 | http://jenkins:8080 |
| SonarQube | http://localhost:9000 | http://sonarqube:9000 |
| Nexus | http://localhost:8081 | http://nexus:8081 |

SonarQube et Nexus mettent 1 à 2 minutes à démarrer.

## 2. Nexus

1. Récupérer le mot de passe admin initial :
   `docker exec nexus cat /nexus-data/admin.password`
2. Se connecter sur http://localhost:8081 (`admin`), choisir un nouveau mot de passe.
3. Les dépôts `maven-releases` et `maven-snapshots` existent déjà par défaut : rien à créer.

## 3. SonarQube

1. Se connecter sur http://localhost:9000 (`admin` / `admin`), changer le mot de passe.
2. **My Account → Security → Generate Token** (type *Global Analysis Token*) et copier le token.
3. **Administration → Configuration → Webhooks → Create**
   - Name : `jenkins`
   - URL : `http://jenkins:8080/sonarqube-webhook/`

   Sans ce webhook, l'étape *Quality Gate* attend jusqu'au timeout.

## 4. Jenkins

1. Mot de passe initial : `docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`
2. Ouvrir http://localhost:8088, installer les plugins suggérés et créer l'utilisateur admin.
   (Les plugins du pipeline sont déjà installés par `ci/jenkins/plugins.txt`.)
3. **Manage Jenkins → Credentials → System → Global → Add Credentials**
   - *Secret text* : Secret = token SonarQube, ID = `sonar-token`
   - *Username with password* : `admin` / mot de passe Nexus, ID = `nexus-credentials`
4. **Manage Jenkins → System → SonarQube servers → Add SonarQube**
   - Name : `SonarQube` (doit être identique à `SONAR_SERVER` dans le Jenkinsfile)
   - Server URL : `http://sonarqube:9000`
   - Server authentication token : `sonar-token`
5. **New Item → Pipeline**
   - *Pipeline script from SCM* → Git → URL du dépôt GitHub → branche `*/main`
   - Script Path : `Jenkinsfile`
6. **Build Now**.

## 5. Résultat attendu

- Jenkins : toutes les étapes en vert, onglet *Test Result* (25 tests), le `.jar` dans *Artifacts*.
- SonarQube : projet `AppGestionDesProjets - backend` avec bugs, code smells, couverture et Quality Gate *Passed*.
- Nexus : **Browse → maven-releases → tn/esprit/backend/1.0.<N>/backend-1.0.<N>.jar**.

## Fichiers concernés

| Fichier | Rôle |
|---|---|
| `Jenkinsfile` | Définition du pipeline |
| `backend/pom.xml` | Version `${revision}`, JaCoCo, Sonar, `distributionManagement` Nexus |
| `backend/src/test/resources/application.properties` | Base H2 pour les tests |
| `backend/src/test/java/...` | Tests unitaires (services + contrôleur) |
| `ci/maven-settings.xml` | Identifiants Nexus (lus depuis les variables d'environnement) |
| `ci/docker-compose.yml` | Jenkins + SonarQube + Nexus |
| `ci/jenkins/Dockerfile`, `plugins.txt` | Image Jenkins JDK 17 avec les plugins |
