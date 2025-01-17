### Solution instructions

##### Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

##### Step 2: Launch the Modbus Pool Emulator

To simulate a comprehensive swimming pool system, this Docker command launches a Modbus pool emulator containing 14 separate devices that function as a unified system and communicate via ModBus. 
Execute the following command in your terminal: 

```bash
docker run --pull always --rm -d --name tb-modbus-pool-emulator -p 5021-5034:5021-5034 thingsboard/tb-modbus-pool-emulator:latest && docker logs -f tb-modbus-pool-emulator{:copy-code}
```

##### Step 3: Launch the IoT Gateway

Create a `docker-compose.yml` file with the necessary configurations:

```bash 
${DOCKER_CONFIG}
{:copy-code}
```

Use Docker Compose to pull and run the IoT Gateway:

```bash
docker compose up{:copy-code}
```

##### Interacting with the Swimming Pool SCADA System

As part of this solution, we have provided a <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard to visualize and interact with the data from multiple devices.
This dashboard allows you to:

- View sensor data and their real-time states.
- Analyze historical data on outdoor and pool temperatures and power consumption.
- Monitor system alarms.
- Control valves, heating and motor pumps.

For further customization of the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected devices.