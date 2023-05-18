## Solution instructions

As part of this solution, we have created the Fuel Level Monitoring dashboard. We will review and describe each solution part below.

#### Fuel Level Monitoring dashboard

This dashboard is intended to monitor the remaining fuel in the tanks, view consumption statistics, manage devices, and respond to changes in defined conditions using the alarm system.

- The **main state** is designed to monitor the remaining fuel and control the placement of tanks, device management, and the alarm system. This page contains the following elements:



  - the section with an interactive map displays the location of the tanks with the help of markers. The marker also informs about the current status of the sensor, namely: green - the sensor is in a normal state, and the rules for triggering alarms are not applied; the red - at least one of the conditions for starting a warning, for example, a low level of fuel remaining, is used; the gray - the sensor is in mode offline To get more information, click on the tank marker - a popup with detailed information will appear.


    The user can use the map filter - map switches that will help sort the display according to requirements.	


  - the Tanks section is a list designed to display all existing tanks. You can delete or edit existing ones. The main list of “Tanks” contains the following data: “Total label”, “Remaining, %”, “Temperature”, “Battery”, “Connection” and action buttons. The user can create/add new sensors by clicking on the "+" button. To create a sensor, the user must go through the following steps: "General info", "Tank info," and "Set location". Note that we provided the ability to add and define three types of tanks, which we can calculate based on their geometric parameters.

  - the alarms section is designed to display all alarms related to the remaining fuel level, temperature, and battery level. You can set the conditions under which alarms will be triggered by clicking the “Alarm Rules” button. You can set the conditions under which alarms will be triggered by clicking the “Alarm Rules” button. By default, the following types of alarms are defined: “Low battery level”, “Low temperature”, “High temperature” and “Low remaining level”.

<div class="img-float" style="max-width:50%;margin: 10px auto">

[comment]: <> (<img src="https://thingsboard.io/images/solutions/waste_monitoring/waste-monitoring-preview.png" alt="Fuel level monitoring">)
</div>

The user can go to the Tank state in several ways: click on the line in the Tanks section of a specific tank or click the "Details" button on the popup when clicking on the marker of the interactive map.

- **Tank state** is designed to display information about a specific tank. This page contains the following elements:

  - fuel remaining display widget;
  - section for displaying detailed tank information: “Tank Name”, “Serial number”, “Liquid type”, “Tank temperature”, “Battery level”, “Connection” and “Last Update”. Also, using the functionality of this section, you can edit the main fields - for this, click the "Edit" button, as well as change the location of the tank, and the marker on the map - by clicking on the "Edit Map" button.
  - the Consumption and remaining fuel section is a table with a list of consumption, remaining, and fuel replenishment. The ability to monitor the duration of refueling or filling the tank with fuel and the timestamp of the action has also been added.
  - the Remaining chart is designed to display the statistics of the tank's remaining and fuel consumption in the form of a graph, which is shown in terms of volume/% and time intervals;
  - the Alarm section is a list of alarms for a specific tank.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/waste-monitoring-preview.png" alt="Fuel level monitoring">
</div>

#### Rule Chains

The **"Fuel Monitoring Rule Chain"** is processing all incoming messages from tank sensors. This rule chain is responsible for counting alarms of all types (temperature, battery, fuel level) and updating the status of the tank sensor based on alarms count and connectivity of device.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/waste_monitoring/rule-chain.png" alt="Waste Monitoring">
</div>
#### Device Profiles

The device profile listed below uses predefined values for alarm thresholds. Administrators may configure alarm thresholds for all devices by navigating to alarm rules.


###### Tank Sensor

The profile by default is configured to raise alarms if:
- for Low Fuel Level the value of "fuelLevel" is equal or less than a configured. By default, the value is set to 20%;
- for Low Battery Level the value of "battery" is equal or less than a configured. By default, the value is set to 30%;
- for High Temperature Level the value of "temperature" is equal or less than a configured. By default, the value is set to 5 C;
- for Low Temperature Level the value of "temperature" is equal or greater than a configured. By default, the value is set to 10 C.



#### Alarms
Alarms are generated using four <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"Tank Sensor" <a href="/profiles/deviceProfiles" target="_blank">device profile</a>.
User may configure the alarm rules via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Fuel Level Monitoring"</a> dashboard using "Alarm rules" form.



#### Devices

We have already created ten sensors and loaded some demo data for them. See device info and credentials below:
**The solution expects that the sensor device will upload temperature, fuel and battery level. The most simple example of the expected payload is in JSON format:**

```json
{"battery": 77, "fuelLevel": 91, "temperature": 32 }{:copy-code}
```

<br>

**To emulate the data upload on behalf of device "Tank Sensor" - "Tank 1273", one should execute the following command:**

```bash
curl -v -X POST -d "{\"battery\":  77, \"fuelLevel\":  91, \"temperature"\: 32 }" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.


### Solution entities

As part of this solution, the following entities were created:
${all_entities}



#### Examples