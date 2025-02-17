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
