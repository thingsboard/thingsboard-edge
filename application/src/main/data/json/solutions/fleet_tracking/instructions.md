## Solution instructions

As part of this solution, we have created the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Fleet Tracking"</a> dashboard that displays
data from multiple buses. You may use the dashboard to:

* observe location and status of the buses;
* monitor bus tracking events (alarms);  
* browse individual bus route, speed and fuel level history;

The dashboard has two states. The main state displays the list of the buses, their location on the map as well as the list of their alarms.
You may browse bus location history popup by clicking on the "Route history" icon located on the right side of the bus table row.  
You may drill down to the bus details state by clicking on the table row. The bus details state allows to browse alarms, location, speed, and fuel level history.

You may always customize the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Fleet Tracking"</a> dashboard using dashboard development <a href="https://thingsboard.io/docs/user-guide/dashboards/" target="_blank">guide</a>.

### Devices

We have already created four bus tracking devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}

Solution expects that the bus tracking device will upload "latitude", "longitude", "speed", "fuel" and "status" values.
The most simple example of the expected payload is in JSON format:

```json
{"latitude":  37.764702, "longitude":  -122.476071, "speed":  50, "fuel":  5, "status": "On route"}{:copy-code}
```

To emulate the data upload on behalf of device "Sensor T1", one should execute the following command:

```bash
curl -v -X POST -d "{\"latitude\":  37.764702, \"longitude\":  -122.476071, \"speed\":  50, \"fuel\":  5, \"status\": \"On route\"}" ${BASE_URL}/api/v1/${Bus CACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Alarms

Alarms are generated using three <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"bus" <a href="/profiles/deviceProfiles" target="_blank">device profile</a>: "Speed limit", "Stopped" and "Low fuel" alarm types.

### Solution entities

As part of this solution, the following entities were created:

${all_entities}
