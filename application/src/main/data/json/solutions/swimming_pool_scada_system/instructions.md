## Solution instructions

* Pull and run the Modbus pool emulator:
    ```bash
    docker run --rm -d --name tb-modbus-pool-emulator -p 5021-5034:5021-5034 thingsboard/tb-modbus-pool-emulator:latest{:copy-code}
    ```

* Install Docker Compose by following the official [Docker Compose installation guide](https://docs.docker.com/compose/install/).

* **Create a docker-compose.yml file** with the following configuration:
```bash 
${DOCKER_CONFIG}{:copy-code}
```

* Pull and run the IoT Gateway using Docker Compose:
    ```bash
    docker-compose up{:copy-code}
    ```

As part of this solution, we have provided a <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard  to visualize and interact with the data from multiple devices. 
This dashboard allows you to:

* view sensor data and their real-time states
* analyze historical outdoor and pool temperature and power consumption trends
* monitor system alarms
* control heating and motor pumps

You may always customize the  <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard using <a href="https://thingsboard.io/docs/pe/user-guide/dashboards/" target="_blank">dashboard development guide</a>.
