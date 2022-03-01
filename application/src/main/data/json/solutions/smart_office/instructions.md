## Solution instructions

As part of this solution, we have created the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Smart office"</a> dashboard that displays
data from multiple devices. You may use the dashboard to:

* observe office sensors and their location;
* browse indoor temperature and power consumption history;
* monitor temperature alarms;
* control HVAC (requires connected device);
* observe specific details for each sensor.

The dashboard has multiple states. The main state displays the list of the devices, their location on the office map as well as the list of their alarms.
You may drill down to the device details state by clicking on the table row. The device details are specific to the device type.

You may always customize the  <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Smart office"</a> dashboard using dashboard development <a href="https://thingsboard.io/docs/user-guide/dashboards/" target="_blank">guide</a>.

### Devices

We have already created "Office" asset and 4 devices related to it. We have also loaded demo data for those devices. See device info and credentials below:

${device_list_and_credentials}

Solution expects specific telemetry from each device based on its type. 
You may find payload examples and commands to send the data on behalf of the devices below.
The examples below use <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.


**Energy meter**


Payload example:

```json
{"voltage":  220, "frequency":  60, "amperage": 16, "power": 3000, "energy": 300 }{:copy-code}
```

To emulate the data upload on behalf of device "Energy meter", one should execute the following command:

```bash
curl -v -X POST -d "{\"voltage\":  220, \"frequency\":  60, \"amperage\": 16, \"power\": 3000, \"energy\": 300}" ${BASE_URL}/api/v1/${Energy meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

**Water meter**


Payload example:

```json
{"water": 2.3, "voltage": 3.9 }{:copy-code}
```

To emulate the data upload on behalf of device "Water meter", one should execute the following command:

```bash
curl -v -X POST -d "{\"water\": 2.3, \"voltage\": 3.9 }" ${BASE_URL}/api/v1/${Water meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

**Smart sensor**


Payload example:

```json
{"co2": 500, "tvoc": 0.3, "temperature": 22.5, "humidity": 50, "occupancy": true}{:copy-code}
```

To emulate the data upload on behalf of device "Smart sensor", one should execute the following command:

```bash
curl -v -X POST -d "{\"co2\": 500, \"tvoc\": 0.3, \"temperature\": 22.5, \"humidity\": 50, \"occupancy\": true}" ${BASE_URL}/api/v1/${Smart sensorACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

**HVAC**


Payload example:

```json
{"airFlow": 300, "targetTemperature": 21.5, "enabled": true}{:copy-code}
```

To emulate the data upload on behalf of device "HVAC", one should execute the following command:

```bash
curl -v -X POST -d "{\"airFlow\": 300, \"targetTemperature\": 21.5, \"enabled\": true}" ${BASE_URL}/api/v1/${HVACACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
``` 

HVAC device also accepts commands from the dashboard to enable/disable air conditioning as well as set target temperature.
The commands are sent using the platform <a href="https://thingsboard.io/docs/user-guide/rpc/" target="_blank">RPC API</a>.

### Alarms

Alarms are generated using <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"smart-sensor" <a href="/deviceProfiles" target="_blank">device profile</a>.

### Solution entities

As part of this solution, the following entities were created:

${all_entities}
