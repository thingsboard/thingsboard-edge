### Solution instructions

##### Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

##### Step 2: Launch the Modbus Pool Emulator

To simulate a comprehensive drilling system, this Docker command launches a Modbus pool emulator containing 5 separate devices that function as a unified system and communicate via ModBus. 
Execute the following command in your terminal: 

```bash
docker run --pull always --rm -d --name tb-drilling-emulator -p 5035-5039:5035-5039 thingsboard/tb-drilling-emulator:latest && docker logs -f tb-drilling-emulator{:copy-code}
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

##### Interacting with the SCADA Drilling System

As part of this solution, we have provided a <a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Oil & Gas</a> dashboard to visualize and interact with the data from multiple drilling devices.
This dashboard allows you to:

- Monitor drilling performance in real time, including rotary speed, drilling speed, and well depth.
- Analyze historical data on mud flow rates, pressure levels, and mechanical loads.
- Track key drilling parameters such as drawwork tension, motor power consumption, and rig vibrations.
- Control system components remotely, including activating pumps, adjusting the preventer, and switching drilling modes.

For further customization of the <a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Oil & Gas</a> dashboard refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected drilling devices.