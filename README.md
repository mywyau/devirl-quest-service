# devirl-quest-service

This Backend service is responsible for business domain data e.g. businesses, offices and desks.

### Order of setup scripts:

To set up the postgres db please run scripts from repo:

### dev-irl-database-setup

1. ./setup_postgres.sh
2. ./setup_flyway_migrations.sh

Then (this can be ran whenever):

```
 ./setup_app.sh
```

### connect to redis

### run redis test container for integration testing, port: 6380

```
docker run --name redis-test-container -p 6380:6379 -d redis
```

#### Run redis-server on port 6379:

```
redis-server
```

#### Run redis-cli to enter cli

```
redis-cli
```

```
keys *
```

```
get <keyId>
```

```
del <keyId>
```

### To run the app locally

```
./run.sh
```

### To run the tests locally

```
./run_tests.sh
```

### To run only a single test suite in the integration tests:

Please remember to include the package/path for the shared resources,
the shared resources is needed to help WeaverTests locate the shared resources needed for the tests

```
./itTestOnly QuestRegistrationControllerISpec controllers.ControllerSharedResource
```

---

### To clear down docker container for app and orphans

```
docker-compose down --volumes --remove-orphans
```

---

### To connect to postgresql database

```
psql -h localhost -p 5432 -U dev_quest_user -d dev_quest_db
```

#### App Database Password:

```
turnip
```

### To connect to TEST postgresql Database

```
psql -h localhost -p 5431 -U dev_quest_test_user -d dev_quest_test_db
```

#### TEST Database Password:

```
turnip
```

---

### Mermaid Wireframe Diagrams

this is for mermaid diagrams

```
command+shift+v
```

## ✅ Option 3: Export to images (for READMEs, docs, or Confluence)

This can be in a separate repo so we do not install the dependency here.

```
npm install -g @mermaid-js/mermaid-cli
```


### Kafka topic commands

```
rpk topic list --brokers localhost:9092  
```

```
rpk topic delete quest.updated.v1.test --brokers localhost:9092
```

```
rpk topic consume quest.events.test1.v1 --brokers localhost:9092 --offset oldest
```