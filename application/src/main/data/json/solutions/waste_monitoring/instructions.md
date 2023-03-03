## Solution instructions

As part of this solution, we have created the "Waste Monitoring Administration" dashboard. We will review and describe each solution part below.

#### Waste Monitoring Administration Dashboard

This dashboard is intended for monitoring the fullness of trash bins, viewing fullness statistics, and managing devices. It has multiple states - Main state and Bin state.

- **Main state** is intended for monitoring the placement and filling of garbage bins, management devices, and their status. The Main state contains:

  - A section with an interactive map that displays the placement of garbage bins and their fullness in real-time. The fullness status is displayed as an indicator (red indicates that the tank is critically full or is already completely full). To get more information, click on the garbage bin marker - a popup with detailed information will appear.
  - The Bins section displays a list of all existing bins. You can delete or edit existing ones. The main list of trash bins contains the following data: “Serial Number”, “Address”, “Connection”, “Fullness”, “Battery Level”, and action buttons.
    The user can create/add new sensors by clicking on the "+" button. Creation takes place by uploading a CSV file - in this way, you can download unlimited devices simultaneously.
    You can also filter the display of bins in the list using the buttons/tabs according to the following parameters: “Total bins”, “Fullness”, “Low Battery,” and “Offline”. By default, the applied parameter is “Total bins”.

  - The alarms section is designed to display all alarms related to the fullness sensors and their battery level. You can set the conditions under which alarms will be triggered by clicking the “Alarm Rules” button.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/waste-monitoring-1.png" alt="Waste Monitoring">
</div>

The user can go to the bin state in several ways: click on the line in the Bins section of a specific bin or click the "Edit" icon/button, as well as on the popup when clicking on the marker of the interactive map - click on the "Details" button.

<br>

- **Bin state** is designed to edit basic information and location relative to a specific bin and monitor fullness, battery level, and alert statistics. The Bin state contains:

  - Sensor's section with detailed info. Contains the following data: "Serial number", "Address", "Latitude", "Longitude", "Fullness level", "Battery level", "Connection" status, and "Last update".
  - By clicking on the "Edit" button of the sensor section, the user can edit the sensor's main fields.
  - The map section is designed to track the sensor's placement and can manually edit the placement.
  - Fullness section designed for monitoring and maintaining bin fullness statistics in real time.
  - Battery level section is designed for monitoring and keeping statistics of the battery level of the sensor in real-time.
  - The Alarms section is designed to display and monitor the main alarms that occur.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/waste-monitoring-2.png" alt="Waste Monitoring">
</div>


#### Rule Chains

The **"Waste Sensor Rule Chain"** is processing all incoming messages from waste sensors. This rule chain is responsible for counting alarms of both types and updating the status of the garbage bin by fullness and battery levels.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/rule-chain.png" alt="Waste Monitoring">
</div>

#### Device Profiles

The device profile listed below uses predefined values for alarm thresholds. Administrators may configure alarm thresholds for all devices by navigating to alarm rules.

###### Waste Sensor

The profile by default is configured to raise alarms if:
- the value of "batteryLevel" is equal or less than a configured. By default, the value is set to 30%.
- the value of "fullLevel" is equal or greater than a configured. By default, the value is set to 90%.


#### Alarms
Alarms are generated using two <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"Waste Sensor" <a href="/deviceProfiles" target="_blank">device profile</a>.
User may turn alarms on and off as well as configure the alarm thresholds via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Waste Monitoring"</a> dashboard using "Edit Sensor" form.


#### Devices

We have already created ten sensors and loaded some demo data for them. See device info and credentials below:
**The solution expects that the sensor device will upload fullness and battery level. The most simple example of the expected payload is in JSON format:**

```json
{"batteryLevel": 77, "fullLevel": 91 }{:copy-code}
```

<br>

**To emulate the data upload on behalf of device "Waste Sensor" - "389021001264", one should execute the following command:**

```bash
curl -v -X POST -d "{\"batteryLevel\":  77, \"fullLevel\":  91 }" ${BASE_URL}/api/v1/${389021001264ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Solution entities

As part of this solution, the following entities were created:
${all_entities}


#### Examples

##### Raise the alarm if the bin is full

Let's recreate an event in which the garbage bin will be filled to 100% and require the responsible persons' fastest response.

To do this, let's take, for example, "Waste Sensor" - "389021001241", which currently has 18% capacity.

<div class="img-float" style="width:40%;margin: 20px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/example-1-1.png" alt="Waste Monitoring">
</div>

Also, the alarm is currently configured and will trigger if the FULLNESS value is greater than or equal to 90%.

<br>

<div class="img-float" style="max-width:20%;margin: 20px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/example-1-2.png" alt="Waste Monitoring">
</div>

<br>

Then to emulate the fullness - “fullLevel” data of device "389021001241" execute the following command:

```bash
curl -v -X POST -d "{\"fullLevel\": 100 }" ${BASE_URL}/api/v1/${389021001241ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/example-1-3.png" alt="Waste Monitoring">
</div>

After the data has been sent, we can see that the fullness is 100% - accordingly, an alarm has been displayed, which will inform the appropriate person about the need to service the bin.




