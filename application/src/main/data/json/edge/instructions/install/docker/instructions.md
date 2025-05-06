Here is the list of commands, that can be used to quickly install ThingsBoard Edge using docker compose and connect to the server.

#### Prerequisites

Install <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> and <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>.

#### Step 1. Running ThingsBoard Edge

Here you can find ThingsBoard Edge docker image:

```bash
nano docker-compose.yml
{:copy-code}
```

Add the following lines to the yml file:

```bash
version: '3.8'
services:
  mytbedge:
    restart: always
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5683-5688:5683-5688/udp"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
      EDGE_LICENSE_INSTANCE_DATA_FILE: /data/instance-edge-license.data
      CLOUD_ROUTING_KEY: ${CLOUD_ROUTING_KEY}
      CLOUD_ROUTING_SECRET: ${CLOUD_ROUTING_SECRET}
      CLOUD_RPC_HOST: ${BASE_URL}
      CLOUD_RPC_PORT: ${CLOUD_RPC_PORT}
      CLOUD_RPC_SSL_ENABLED: ${CLOUD_RPC_SSL_ENABLED}
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
  postgres:
    restart: always
    image: "postgres:15"
    ports:
      - "5432"
    environment:
      POSTGRES_DB: tb-edge
      POSTGRES_PASSWORD: postgres
    volumes:
      - tb-edge-postgres-data:/var/lib/postgresql/data

volumes:
  tb-edge-data:
    name: tb-edge-data
  tb-edge-logs:
    name: tb-edge-logs
  tb-edge-postgres-data:
    name: tb-edge-postgres-data
{:copy-code}
```

##### [Optional] Update bind ports 
If ThingsBoard Edge is going to be running on the same machine where ThingsBoard server (cloud) is running, you'll need to update docker compose port mapping to avoid port collision between ThingsBoard server and ThingsBoard Edge.

Please update next lines of `docker-compose.yml` file:

```text
ports:
  - "18080:8080"
  - "11883:1883"
  - "15683-15688:5683-5688/udp"
```
Make sure that ports above (18080, 11883, 15683-15688) are not used by any other application.

#### Step 2. Start ThingsBoard Edge
Set the terminal in the directory which contains the docker-compose.yml file and execute the following commands to up this docker compose directly:

```bash
docker compose up -d && docker compose logs -f mytbedge
{:copy-code}
```

###### NOTE: Docker Compose V2 vs docker-compose (with a hyphen)

ThingsBoard supports Docker Compose V2 (Docker Desktop or Compose plugin) starting from **3.4.2** release, because **docker-compose** as standalone setup is no longer supported by Docker.
We **strongly** recommend to update to Docker Compose V2 and use it.
If you still rely on using Docker Compose as docker-compose (with a hyphen), then please execute the following commands to start ThingsBoard Edge:

```bash
docker-compose up -d
docker-compose logs -f mytbedge
```

#### Step 3. Open ThingsBoard Edge UI

Once the Edge service is started, open the Edge UI at http://localhost:8080.

###### NOTE: Edge HTTP bind port update 

If the Edge HTTP bind port was changed to 18080 during Edge installation, access the ThingsBoard Edge instance at http://localhost:18080.

