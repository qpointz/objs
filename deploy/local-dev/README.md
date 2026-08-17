# Local development

This stack runs the PostgreSQL instance used by the `objs-service-app` `postgres` profile
(workbench runner on port **8081**). Database files are retained under
`.data/local-dev/postgres`.

From the repository root:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml up -d --wait
docker compose -f deploy/local-dev/docker-compose.yml ps
./gradlew :objs-service-app:run --args="--spring.profiles.active=postgres"
# → http://localhost:8081/workbench/  and  /api/v1/objs/**
```

Stop PostgreSQL without deleting its data:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml down
```

Reset the local database:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml down
Remove-Item -Recurse -Force .data/local-dev/postgres
docker compose -f deploy/local-dev/docker-compose.yml up -d --wait
```

Defaults match `objs-service-app/src/main/resources/application.yml`:

- database: `objs`
- host port: `5432`
- username: `postgres`
- password: `postgres`

Override Compose values with `OBJS_DB_NAME`, `OBJS_DB_PORT`,
`OBJS_DB_USERNAME`, and `OBJS_DB_PASSWORD`. When changing the database name
or port, also set `OBJS_DB_URL` before starting `objs-service-app`, for example:

```powershell
$env:OBJS_DB_URL = "jdbc:postgresql://localhost:55432/custom_objs"
```
