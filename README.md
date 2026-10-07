# Submission Details

## Application & Deployment

- **Application URL:** TBD — will be updated after deployment
- **GitOps Repository:** https://github.com/capricarun/habitapp-gitops
- **Docker Hub:** https://hub.docker.com/r/capricarun/habit-tracker
- **Image Tag Format:** `capricarun/habit-tracker:<git-commit-sha>`

### Security Notes

- GitHub authentication uses a fine-grained Personal Access Token limited to the `habitapp-gitops` repository.
- Docker Hub authentication uses an access token rather than a password.
- Secrets and access tokens are not committed to the Git repository.
- Kubernetes configuration should use Kubernetes Secrets for sensitive values.
- SSH access to EC2 is restricted to the administrator's IP address.

### Rollback Method

Rollback is performed through GitOps by reverting the application image tag in the `habitapp-gitops` repository to a previously known-good image tag. Argo CD detects the Git change and synchronizes the previous application version to the Kubernetes cluster.

### Build Verification

- Java: 21
- Maven: 3.9.12
- Spring Boot: 3.3.4
- Tests: **13 passed, 0 failures**
- Build: **`mvn -B clean verify` — BUILD SUCCESS**
- Artifact: `target/habit-tracker.jar`
- JaCoCo coverage report generated successfully.

---

# Habit Tracker

A self-contained Spring Boot REST API + dashboard for tracking daily/weekly habits and streaks.
**No external database** — all data is held in memory (`ConcurrentHashMap`) for the life of the process.

## Tech stack
- Java 21 (or higher — LTS)
- Spring Boot 3.3.4 (`spring-boot-starter-web`, `spring-boot-starter-validation`)
- Maven (packaged as an executable jar)
- Vanilla HTML/CSS/JS dashboard (served as a static resource, no frontend build step)

## Requirements
- **JDK 21+** installed and set as `JAVA_HOME` (verify with `java -version`)
- Maven 3.6+ (verify with `mvn -version`)

## Build

```bash
mvn clean package
```

This produces `target/habit-tracker.jar`.

## Run

```bash
java -jar target/habit-tracker.jar
```

Then open **http://localhost:8080** in your browser for the dashboard, or call the API directly.

## REST API

| Method | Endpoint                        | Description                          |
|--------|----------------------------------|---------------------------------------|
| GET    | `/api/habits`                    | List all habits                      |
| GET    | `/api/habits/{id}`               | Get a single habit                   |
| POST   | `/api/habits`                    | Create a habit                       |
| PUT    | `/api/habits/{id}`               | Update a habit's name/description    |
| DELETE | `/api/habits/{id}`               | Delete a habit                       |
| PATCH  | `/api/habits/{id}/active`        | Activate/deactivate a habit          |
| POST   | `/api/habits/{id}/complete`      | Mark habit complete for today (or `?date=YYYY-MM-DD`) |
| DELETE | `/api/habits/{id}/complete`      | Unmark completion for a date         |
| GET    | `/api/habits/{id}/stats`         | Streaks + last-7-day completion rate |

### Example: create a habit

```bash
curl -X POST http://localhost:8080/api/habits \
  -H "Content-Type: application/json" \
  -d '{"name":"Drink water","description":"8 glasses a day","frequency":"DAILY"}'
```

### Example: mark it done today

```bash
curl -X POST http://localhost:8080/api/habits/1/complete
```

## SonarQube code scan

### Requirements
- A running SonarQube server (Community Edition is fine) — locally via Docker is easiest, or use SonarCloud
- Java 21+ (same JDK used to build the project)
- Maven 3.6+
- A Sonar authentication token (generate from the SonarQube UI: **My Account → Security → Generate Token**)
- Network access from your machine to the SonarQube server (default port `9000`)

### 1. Start a local SonarQube server (skip if you already have one, or are using SonarCloud)
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```
Wait ~1 minute, then open http://localhost:9000 (default login `admin` / `admin`, it'll prompt you to change the password). Generate a token as noted above.

### 2. Run the scan via Maven (recommended — no separate scanner install needed)
```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_GENERATED_TOKEN
```
- `clean verify` compiles, runs tests, and generates the JaCoCo coverage report the `pom.xml` is already configured to produce.
- `sonar:sonar` (from the `sonar-maven-plugin` already added to `pom.xml`) runs the actual analysis and uploads results to the server.

### 3. Alternative: standalone `sonar-scanner` CLI
If you'd rather not use the Maven plugin, install the [SonarScanner CLI](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner/), then from the project root:
```bash
mvn clean verify        # still needed, to produce target/classes + jacoco.xml
sonar-scanner -Dsonar.token=YOUR_GENERATED_TOKEN
```
This picks up settings from `sonar-project.properties`, already included in the project.

### 4. Using SonarCloud instead of a local server
Replace `-Dsonar.host.url` with SonarCloud's URL and add your organization key:
```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=YOUR_ORG_KEY \
  -Dsonar.token=YOUR_SONARCLOUD_TOKEN
```

### Where results show up
After the scan finishes, open the SonarQube/SonarCloud dashboard for project key `habit-tracker` to see bugs, code smells, vulnerabilities, and test coverage (from JaCoCo).

## Notes
- Streaks (`currentStreak`, `longestStreak`) are recalculated automatically whenever a habit is marked/unmarked complete.
- Because storage is in-memory, restarting the app clears all data. Swapping in a real database later just means replacing `HabitRepository` with a Spring Data JPA repository — the service/controller layers don't need to change.
