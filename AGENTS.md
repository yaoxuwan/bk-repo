# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

BK-REPO (蓝鲸制品库) is a microservice-based artifact repository platform. The codebase lives under `src/` with four main components: **backend** (Kotlin/Spring Boot), **frontend** (Vue 2), **gateway** (OpenResty/Lua), and **proxy** (Kotlin).

### Prerequisites (already installed in VM snapshot)

- **JDK 17** — set as default; `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- **Node.js 18** via nvm (frontend requires Node <= 19; run `nvm use 18` before frontend commands)
- **MongoDB 7.0** and **Redis** — installed as system packages
- **Gradle 8.10.2** — managed by wrapper (`src/backend/gradlew`)

### Starting Services

Before running backend or integration tests, MongoDB and Redis must be running:

```bash
# MongoDB (must run as root for data dir access; increase ulimit to avoid WiredTiger "Too many open files" crash)
sudo bash -c 'ulimit -n 524288 && mongod --dbpath /data/db --fork --logpath /tmp/mongod.log'

# Redis
redis-server --daemonize yes

# Seed initial data (admin user, default project, etc.)
mongosh bkrepo /workspace/support-files/sql/init-data.js
```

**Important**: MongoDB WILL crash with "Too many open files" during index creation if not started with a high `ulimit -n`. The backend creates 256+ sharded `node_*` collections on startup. Always start MongoDB via `sudo bash -c 'ulimit -n 524288 && mongod ...'`.

### Backend

```bash
cd src/backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Build (skip tests for speed)
./gradlew boot-assembly:build -x test

# Run standalone (all services in one process)
java -Xmx2g -jar boot-assembly/build/libs/boot-assembly-1.0.0-RELEASE.jar --server.port=8080

# Lint
./gradlew ktlintCheck

# Tests (subset example)
./gradlew :common:common-api:test
```

- In boot-assembly (standalone) mode, API URLs are prefixed with the service name extracted from the package: `/{serviceName}/api/...`. For example, repository APIs are at `/repository/api/project/...`.
- Auth: Use Basic Auth with `admin:password` (seeded via init-data.js). Header: `Authorization: Basic $(echo -n admin:password | base64)`.
- Artifact storage path: `/data/store` — must be writable by the user running the backend (`sudo chown -R ubuntu:ubuntu /data/store`).

### Frontend

```bash
cd src/frontend
source ~/.nvm/nvm.sh && nvm use 18

# Install
yarn install

# Dev server (port 8086 for devops-repository)
cd core/devops-repository && yarn dev

# Production build
cd src/frontend && yarn public -m standalone

# Lint
npx eslint --ext .js,.vue core/devops-repository/src
```

- The frontend dev server serves at `http://localhost:8086/`. The page title shows "制品库 | 腾讯蓝鲸智云" but the full UI rendering requires the OpenResty gateway layer to serve external JS libraries (Vue, VueRouter, etc.).

### Key API Examples (standalone boot-assembly on port 8080)

```bash
AUTH=$(echo -n "admin:password" | base64)

# Health check
curl http://localhost:8080/actuator/health

# Create project
curl -X POST http://localhost:8080/repository/api/project/create \
  -H "Content-Type: application/json" -H "Authorization: Basic $AUTH" \
  -d '{"name":"myproject","displayName":"My Project","description":""}'

# Create generic repo
curl -X POST http://localhost:8080/repository/api/repo/create \
  -H "Content-Type: application/json" -H "Authorization: Basic $AUTH" \
  -d '{"projectId":"myproject","name":"generic-local","type":"GENERIC","category":"LOCAL"}'

# Upload artifact
curl -X PUT http://localhost:8080/generic/myproject/generic-local/path/to/file.txt \
  -H "Authorization: Basic $AUTH" -T localfile.txt

# Download artifact
curl http://localhost:8080/generic/myproject/generic-local/path/to/file.txt \
  -H "Authorization: Basic $AUTH"
```
