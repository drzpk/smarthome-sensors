# Sensors

Smart-Home module responsible for collecting data from IoT sensors. Works
in conjunction with [data-logger](https://gitlab.com/smart-home-dr/data-logger).

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
mapped in the container.

## Configuration guide

*To be continued...*
