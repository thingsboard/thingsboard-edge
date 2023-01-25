## Solution instructions

As part of this solution, we have created the "Irrigation Management" dashboard, 2 field assets, and 12 devices with simulated data. 
We will review and describe each solution part below:

### Dashboards

##### Irrigation Management

The <a href="${Irrigation ManagementDASHBOARD_URL}" target="_blank">"Irrigation Management"</a> dashboard
is designed to provision fields and related devices. It has multiple states:

* **Main** state allows you to list the fields and display them on the map. 
  We assume that you might have multiple fields with various sensors for each field.
  We have provisioned two "fake" fields with number of devices for demonstration purposes.
    * Click the "+" button in the top right corner of the fields table to create a new field. 
      You may input the crop type and soil moisture thresholds;
    * Click the field polygon on the map to open the field state;
* **Field** state allows you to manage irrigation schedule and devices.
  You may provision new schedule items. The schedule dialog will create scheduler events on the background.
  Click on the "Alarms" button to browse all alarms. You may also add sensors to the field if needed. 
      
### Rule Chains

* "SI Soil Moisture" Rule Chain is responsible for processing of telemetry from Soil Moisture sensors. 
* "SI Water Meter" Rule Chain is processing data from the water meter and calculate the water consumption.
* "SI Field" Rule Chain is responsible for start/stop of the irrigation based on the water consumption or irrigation duration;
* "SI Count Alarms" Rule Chain helps to count alarms for particular entity: device or asset. It is referenced from other rule chains;
* "SI Smart Valve" Rule Chain helps to send RPC commands to the smart valve device to stop the irrigation;

### Device & Asset Profiles

The device & asset profiles listed below use pre-defined values for alarm thresholds. This values are common for all devices that share same device profile.

##### SI Field

The field asset profile is configured to forward all incoming events to the "SI Field" rule chain.

##### SI Water Meter

The profile is configured to raise alarms if the value of "battery" telemetry is below a configurable threshold. 
Warning alarm is raised when the value is below 30.

The device also uploads the "pulseCounter" which is used to calculate water consumption. Sample device payload:

```json
{"battery": 99, "pulseCounter": 123000}{:copy-code}
```

##### SI Soil Moisture Sensor

The profile is configured to raise alarms if the value of "battery" telemetry is below a configurable threshold. 
Warning alarm is raised when the value is below 30.

The device also uploads the "moisture" level. Sample device payload:

```json
{"battery": 99, "moisture": 57}{:copy-code}
```

##### SI Smart Valve

The profile is configured to raise alarms if the value of "battery" telemetry is below a configurable threshold. 
Warning alarm is raised when the value is below 30.
Sample device payload:

```json
{"battery": 99}{:copy-code}
```

The device also accepts the RPC command to enable or disable the water flow. Sample RPC command:

```json
{"method": "TURN_ON", "params": {}}{:copy-code}
```

### Devices

We have already created 12+ devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}

Solution expects that the device telemetry will correspond to the samples provided in device profile section of the instruction.
The most simple example of the moisture sensor payload is in JSON format:

```json
{"moisture": 57}{:copy-code}
```

To emulate the data upload on behalf of device "SI Soil Moisture 1" located inside field "Field 1", one should execute the following command to raise the Critical Alarm for Field 1:

```bash
curl -v -X POST -d "{\"moisture\":  77}" ${BASE_URL}/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a> for simplicity of demonstration.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.
      
### Solution entities

As part of this solution, the following entities were created:

${all_entities}

### [Optional] Edge computing

Optionally, this solution can be deployed to the edge.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

In the scope of this solution, new edge entity <a href="${Remote Farm R1EDGE_DETAILS_URL}" target="_blank">Remote Farm R1</a> was created.

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Smart Irrigation"** *ASSET* group;
* **"Smart Irrigation"** *DEVICE* group;
* **"Smart Irrigation"** *DASHBOARD* group.

ThingsBoard Edge is a separate service that must be installed, configured and connected to the cloud.
The easiest way to install ThingsBoard Edge is to use <a href="https://docs.docker.com/compose/install/" target="_blank">Docker Compose</a>.

Docker compose installation instructions are available on the edge details page.
Please navigate to <a href="${Remote Farm R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button to see the instructions.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### Push data to device on edge

**"Smart Irrigation"** *DEVICE* group was assigned to the edge entity "Remote Farm R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Device groups** page.

To emulate the data upload on behalf of device "SI Soil Moisture 1" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"moisture\":  77}" http://localhost:8080/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"moisture\":  77}" http://localhost:18080/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "SI Soil Moisture 1" on edge, you'll be able to see telemetry update on the cloud for this device as well.