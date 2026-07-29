# Local development

This stack runs the PostgreSQL instance used by the `objs-app` `postgres` profile.
Database files are retained under `.data/local-dev/postgres`.

From the repository root:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml up -d
docker compose -f deploy/local-dev/docker-compose.yml ps
./gradlew :objs-app:run --args="--spring.profiles.active=postgres"
```

Stop PostgreSQL without deleting its data:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml down
```

Reset the local database:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml down
Remove-Item -Recurse -Force .data/local-dev/postgres
docker compose -f deploy/local-dev/docker-compose.yml up -d
```

Defaults match `objs-app/src/main/resources/application.yml`:

- database: `objs`
- host port: `5432`
- username: `postgres`
- password: `postgres`

Override Compose values with `OBJS_DB_NAME`, `OBJS_DB_PORT`,
`OBJS_DB_USERNAME`, and `OBJS_DB_PASSWORD`. When changing the database name
or port, also set `OBJS_DB_URL` before starting `objs-app`, for example:

```powershell
$env:OBJS_DB_URL = "jdbc:postgresql://localhost:55432/custom_objs"
```
