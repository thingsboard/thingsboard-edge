# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThingsBoard Edge CE — an open-source edge computing extension of the ThingsBoard IoT platform. It processes data locally on edge devices and synchronizes with the ThingsBoard Cloud via gRPC streaming. Version 4.3.1EDGE-SNAPSHOT, Java 17, Spring Boot 3.4.x.

## Build Commands

```bash
# Full build (skip tests)
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=4096" mvn clean install -DskipTests

# Build specific module(s) and dependencies
mvn clean install -DskipTests --projects application --also-make

# Build only backend (skip UI)
mvn clean install -DskipTests -pl '!ui-ngx'

# Run all tests for a module
mvn test -pl dao

# Run a single test class
mvn test -pl application -Dtest=org.thingsboard.server.controller.DeviceControllerTest

# Run a single test method
mvn test -pl application -Dtest="DeviceControllerTest#testGetDevice"

# Tests with parallel execution and flake tolerance
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"
mvn test -pl application -Dtest='org.thingsboard.server.edge.**' -DforkCount=4 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2

# Regenerate protobuf sources
./build_proto.sh

# Format license headers
mvn license:format
```

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `application` | Main Spring Boot app. Entry point: `TbEdgeApplication.java`. Contains actors, controllers, service layer, and edge processors |
| `common/edge-api` | gRPC client for Edge↔Cloud sync (`EdgeGrpcClient`, `EdgeRpcClient` interface) |
| `common/proto` | Protobuf definitions for transport, queue, and JS invocation messages |
| `common/data` | Shared domain model POJOs (Device, Asset, Tenant, etc.) |
| `common/dao-api` | DAO interfaces (service contracts for persistence) |
| `common/transport/*` | Transport protocol implementations: MQTT, CoAP, HTTP, LWM2M, SNMP |
| `common/queue` | Message queue abstraction (Kafka, in-memory, AWS SQS, GCP Pub/Sub) |
| `common/actor` | Actor system framework for concurrent message processing |
| `common/cache` | Caching abstraction (Caffeine or Redis) |
| `common/script` | Rule engine script execution (TBEL, remote JS) |
| `dao` | Data access layer — JPA/Hibernate repositories for PostgreSQL, TimescaleDB, Cassandra |
| `rule-engine/rule-engine-api` | Rule node interfaces and `@RuleNode` annotation |
| `rule-engine/rule-engine-components` | Built-in rule node implementations |
| `rest-client` | Java REST client library for the TB Edge API |
| `ui-ngx` | Angular 20 web UI with Angular Material, NgRx, Leaflet maps |
| `msa` | Microservice packaging (Docker images for tb-edge-node, tb-edge) |
| `netty-mqtt` | Custom Netty-based MQTT protocol codec |

### Key Architectural Patterns

**Actor System**: Core concurrency model. `AppActor` → `TenantActor` → `DeviceActor` / `RuleChainActor` → `RuleNodeActor`. Messages flow from transport → device actor → rule chain.

**Edge-Cloud Sync**: Bidirectional gRPC streaming (`edge.proto`). Edge sends `UplinkMsg` to cloud, receives `DownlinkMsg`. Entity changes are captured by `EdgeEventSourcingListener` and processed by ~23 domain-specific edge processors in `application/src/main/java/org/thingsboard/server/service/edge/`.

**Database Strategy**: SQL (PostgreSQL) for entities, configurable time-series backend (`sql` | `timescale` | `cassandra`). DAO layer uses Spring Data JPA repositories. Time-series data uses batch inserts with partitioning.

### Key Configuration

Main config: `application/src/main/resources/tb-edge.yml`

Critical settings:
- `cloud.rpc.host/port` — Cloud server connection (default gRPC port: 7070)
- `database.ts.type` — Time-series DB: `sql`, `timescale`, or `cassandra`
- `cache.type` — `caffeine` (local) or `redis`
- `actors.system.*_pool_size` — Actor thread pool sizing
- Transport ports: MQTT (1883), CoAP (5683), HTTP (8080)

### Protobuf / gRPC

Proto definitions live in `common/proto/src/main/proto/` and `common/edge-api/src/main/proto/`. The edge-cloud RPC service is defined in `edge.proto`:
```protobuf
service EdgeRpcService {
  rpc handleMsgs(stream RequestMsg) returns (stream ResponseMsg) {}
}
```
Edge protocol versioning via `EdgeVersion` enum (current: `V_4_3_1`).

### Testing Notes

- Tests use **Testcontainers** for PostgreSQL. If Docker API version is incompatible, set `"min-api-version": "1.32"` in `/etc/docker/daemon.json`.
- Application tests are split by package for parallel CI execution (controller, edge, service, transport).
- NoSQL tests are excluded by default (`!**/nosql/**`).
