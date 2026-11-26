# Mise en place de la CI : Jenkins + SonarQube + Nexus

```
GitHub (main) ──poll 5 min──► Jenkins (WSL, :8081)
   Checkout → Build (mvn clean package → backend/target/*.jar) → Tests (mvn test + JaCoCo)
   → SonarQube (:9000) → Quality Gate → Nexus (:8082, version 1.0.<BUILD_NUMBER>)
```

| Outil | Où il tourne | URL |
|---|---|---|
| Jenkins | Ubuntu (WSL), installé avec apt | http://localhost:8081 |
| SonarQube | Docker (`ci/docker-compose.yml`) | http://localhost:9000 |
| Nexus | Docker (`ci/docker-compose.yml`) | http://localhost:8082 |

## 0. Vérifier en local (sans Jenkins)

```bash
cd backend
./mvnw clean package -DskipTests   # → target/backend-1.0.0-SNAPSHOT.jar
./mvnw test                        # → 25 tests, base H2 en mémoire (pas de MySQL)
```

## 1. Démarrer SonarQube et Nexus

Avec Docker installé dans Ubuntu (WSL), depuis un terminal Ubuntu :

```bash
sudo docker start sonarqube || sudo docker run -d --name sonarqube -p 9000:9000 sonarqube:community
sudo docker start nexus     || sudo docker run -d --name nexus -p 8082:8081 -v nexus_data:/nexus-data sonatype/nexus3
```

(Avec Docker Desktop + Compose, on peut à la place lancer `cd ci && docker compose up -d`.)

Attendre 1 à 2 minutes, puis vérifier depuis Ubuntu :

```bash
curl -s http://localhost:9000/api/system/status   # → "status":"UP"
curl -sI http://localhost:8082 | head -1           # → HTTP/1.1 200
```

## 2. Nexus (http://localhost:8082)

1. Mot de passe admin initial : `sudo docker exec nexus cat /nexus-data/admin.password`
2. Se connecter en `admin` et choisir un nouveau mot de passe.
3. Les dépôts `maven-releases` et `maven-snapshots` existent déjà.

## 3. SonarQube (http://localhost:9000)

1. Se connecter (`admin` / `admin`) et changer le mot de passe.
2. **My Account → Security → Generate Token** (*Global Analysis Token*) : copier le token.
3. **Administration → Configuration → Webhooks → Create**
   - Name : `jenkins`
   - URL : `http://172.17.0.1:8081/sonarqube-webhook/`

   `172.17.0.1` = la machine Ubuntu vue depuis un conteneur Docker (passerelle du réseau `bridge`).
   `host.docker.internal` ne fonctionne **pas** avec Docker installé dans Ubuntu.
   Vérifier l'adresse : `sudo docker network inspect bridge -f '{{(index .IPAM.Config 0).Gateway}}'`

## 4. Jenkins : plugins

**Manage Jenkins → Plugins → Available plugins**, installer :

- `Pipeline` (workflow-aggregator)
- `Git`
- `Pipeline: Stage View`
- `SonarQube Scanner`
- `JUnit`
- `Credentials Binding`
- `Timestamper`

Cocher *Restart Jenkins when installation is complete*.

## 5. Jenkins : credentials et SonarQube

1. **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**
   - *Secret text* : Secret = token SonarQube, ID = `sonar-token`
   - *Username with password* : `admin` / mot de passe Nexus, ID = `nexus-credentials`
2. **Manage Jenkins → System → SonarQube servers → Add SonarQube**
   - Name : `SonarQube` (identique à `SONAR_SERVER` dans le Jenkinsfile)
   - Server URL : `http://localhost:9000`
   - Server authentication token : `sonar-token`
   - **Save**

## 6. Connecter GitHub à Jenkins (job Pipeline)

Le job `devops_khaled` existant est un projet *Freestyle* vide : il ne peut pas lire un Jenkinsfile.
Le supprimer (**devops_khaled → Delete Project**), puis :

1. **New Item** → nom `devops_khaled` → **Pipeline** → OK
2. Section **Pipeline** :
   - Definition : **Pipeline script from SCM**
   - SCM : **Git**
   - Repository URL : `https://github.com/KhaledBakhtriIA/devops_khaled.git`
   - Credentials : *none* si le dépôt est public (sinon : Username = compte GitHub, Password = token GitHub)
   - Branch Specifier : `*/main`
   - Script Path : `Jenkinsfile`
3. **Save** → **Build Now**

Après le premier build, Jenkins vérifie GitHub toutes les ~5 minutes (`pollSCM` dans le Jenkinsfile)
et relance le pipeline à chaque nouveau `git push`.

## 7. Résultat attendu

- Jenkins : 6 étapes vertes, onglet *Test Result* (25 tests), le `.jar` dans *Artifacts*.
- SonarQube : projet `AppGestionDesProjets - backend` avec Quality Gate *Passed*.
- Nexus : **Browse → maven-releases → tn/esprit/backend/1.0.<N>/backend-1.0.<N>.jar**.

## Dépannage

**Jenkins n'arrive pas à joindre SonarQube ou Nexus** (`Connection refused` sur `localhost:9000` / `localhost:8082`).
WSL et Windows n'ont pas le même `localhost` en mode réseau NAT. Tester depuis Ubuntu :

```bash
curl -I http://localhost:9000
curl -I http://localhost:8082
```

Si ça échoue, activer le mode réseau *mirrored* (localhost partagé entre Windows et WSL) :
créer `C:\Users\<vous>\.wslconfig` avec

```ini
[wsl2]
networkingMode=mirrored
```

puis `wsl --shutdown` dans PowerShell et relancer Ubuntu et Jenkins.

**`SonarQube installation defined in this job (SonarQube) does not match any configured installation. Number of installations that can be configured: 0`**
Le serveur SonarQube n'est pas déclaré dans Jenkins (le credential `sonar-token` seul ne suffit pas).
Faire l'étape 5.2 : **Manage Jenkins → System → SonarQube servers → Add SonarQube**, Name = `SonarQube`.

**L'étape Quality Gate attend 5 minutes puis échoue** : le webhook SonarQube → Jenkins (étape 3.3) n'arrive pas.
Vérifier dans SonarQube **Administration → Webhooks → (dernier envoi)**.
