# Sensors

HTTP server that collects measurement data from IoT sensors and persists it to InfluxDB.
Sensor devices and groups are managed through a REST API backed by a MariaDB database.
Works in conjunction with [data-logger](https://gitlab.com/smart-home-dr/data-logger),
which is responsible for forwarding measurements from physical devices.

**Tech stack:** Kotlin, Ktor, Koin, Exposed, InfluxDB, MariaDB

## Deployment guide

The following list of steps allows to start the sensors application.

1. Create MySQL/MariaDB database and user.
   ```mariadb
   CREATE SCHEMA sensors DEFAULT CHARACTER SET 'UTF8';
   CREATE USER sensors IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON sensors.* to sensors@`%`;
   FLUSH PRIVILEGES;
   ```

2. Create InfluxDB [bucket](https://docs.influxdata.com/influxdb/v2.0/organizations/buckets/create-bucket/)
   and [token](https://docs.influxdata.com/influxdb/cloud/security/tokens/create-token/).

3. Use the [example Docker configuration](examples/docker). The `config` directory must be
   mapped into the container.

## Configuration guide

### Environment variables

| Variable              | Default               | Description                              |
|-----------------------|-----------------------|------------------------------------------|
| `CONFIG_FILE`         | _(empty)_             | Path to the application config file      |
| `LOGBACK_CONFIG_FILE` | bundled `logback.xml` | Path to the Logback config file          |
| `JAVA_TOOL_OPTIONS`   | _(empty)_             | Additional JVM options (e.g. `-Xmx512m`) |

### Application config

The application config uses [HOCON](https://github.com/lightbend/config/blob/main/HOCON.md) format.
See [examples/docker/config/application.conf](examples/docker/config/application.conf) for a reference.

### Logger config

The logger config uses the standard [Logback XML](https://logback.qos.ch/manual/configuration.html) format.
See [examples/docker/config/logback.xml](examples/docker/config/logback.xml) for a reference.
