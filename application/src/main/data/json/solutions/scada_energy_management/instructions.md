### Solution instructions

##### Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

##### Step 2: Launch the Modbus Drilling Emulator

To simulate a comprehensive energy management system, this Docker command launches a Modbus energy emulator containing 7 separate devices that function as a unified system and communicate via Modbus. 
Execute the following command in your terminal:

```bash
docker run --pull always --rm -d --name tb-modbus-energy-emulator -p 5040-5046:5040-5046 thingsboard/tb-energy-emulator:latest && docker logs -f tb-modbus-energy-emulator{:copy-code}
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

##### Interacting with the SCADA Energy management

As part of this solution, we have provided a <a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Energy management</a> dashboard to visualize and interact with the data from multiple energy devices.
This dashboard allows you to:

- Monitor real-time data from solar, wind, battery, and generator devices.
- View energy storage levels, inverter states, and consumption values.
- Visualize energy flow between generation, storage, and consumption components.
- Remotely control devices such as toggling the generator, inverter, or wind turbine.
- Gain insights from voltage readings and transformer outputs.

For further customization of the <a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Energy management</a> dashboard refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected devices.
