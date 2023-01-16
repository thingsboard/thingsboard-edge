## Solution instructions

As part of this solution, we have created 2 dashboards that display data from multiple sensors.
The standards for AQI calculations are specific to the region. We use US AQI for simplicity.  You may reconfigure the calculations in the Rule Engine.

We will review and describe each solution part below:

#### Public Air Quality Monitoring Dashboard

This dashboard is designed for end-users. It is configured to be "public", meaning the end-user does NOT need to log in to access the dashboard. 
Making the dashboard public is also useful when you plan to embed the page into an external website. You may embed the current dashboard using the code below:

```html
<iframe src="${BASE_URL}${MAIN_DASHBOARD_PUBLIC_URL}" style="position:fixed; inset:0; width:100%; height:100%; border:none;"></iframe>{:copy-code}
```

The dashboard has multiple states:

<div class="img-float" style="max-width:50%;margin: 20px auto;float:right">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-city-state.png" alt="AQI Public Dashboard - City State">
</div>

- **City state** represents the air pollution monitoring of a specific city (in our case Los Angeles) and calculates value based on the maximum AQI received from city sensors. Also, this state contains the following elements:
  - The name of the current city or district;
  - Temperature and humidity;
  - Pollution status according to EPA;
  - Average AQI value based on all sensors in the city and a scale for convenient viewing of pollution;
  - Click on "i" info-icon and a pop-up with the legend of pollutants will appear;
  - A section with general recommendations for sensitive groups of people regarding the current level of pollution;
  - History section of AQI level in the current, weekly and monthly range;
  - Interactive map showing air pollution monitoring stations, the markers of which are color-coded depending on the AQI level;
  - Click on the sensor marker on the map and go to the Sensor state (you can switch to other sensors by clicking on other device markers).

- **Sensor state** represents the current state of the specific sensor. This state contains the following elements:
  - Contains the same elements as the **City state**, but the data is based on the sensor;
  - Section of specific pollutants which include: PM2.5, PM10, NO2, CO, SO2, O3. 
    Click on one of these tiles and a pop-up will appear, which will display a description, general recommendations, as well as its statistics for this pollutant.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-sensor-state.png" alt="AQI Public Dashboard - Sensor State">
</div>


#### Administration Air Quality Monitoring Dashboard

This dashboard is designed for tenant administrators to perform basic device management tasks, and has multiple states:

-  **Main state** which is intended for monitoring sensors, alarms, etc. The Main state contains:
   - The **Sensors** section. 
    All sensor data is displayed in a table where you can see the following information: “Sensor Label”, “Sensor id”, “Connection”, “Battery level” and “Last AQI”.
    You can also add a new sensor, edit and delete a sensor.
    Click on a specific sensor from the table and go to the **Sensor state**;

   - **Alarms** section.
    All alarm data is displayed in a table where you can see the following information: “Created time”, “Type”, “Sensor id” and “Status”. 
    Click on the “Settings” icon where you can set the alarm rules manually.    
    By default you can configure the values at which alarms will be triggered for such values as Battery Level(in percent) and duration of no connection (in hours);
   - **Interactive map**. After selecting the marker of the sensor on the map, a pop-up will appear with information about it.
    Click on **“Details”** and go to the selected **Sensor state**.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-admin-state-1.png" alt="AQI Administration Dashboard - Sensor State">
</div>

- **Sensor state** allows you to view detailed information about the sensor. It contains the following sections:
   - Sensor details that show information about the sensor. Use the **“Edit”** button to edit senor details;
   - Sensor measures that show next data: “connection” status, “Battery level”, “Last AQI”, “PM2.5”, “PM10”, “NO2”, “CO”, “SO2”, “O3”;
   - Battery level chart;
   - A Map which shows the location of sensor, allows you to move, delete and restore the sensor marker;
   - Sensor alarms table;
   - Connection status chart.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-admin-state-2.png" alt="AQI Administration Dashboard - Sensor State">
</div>

#### Entity Groups

Solution has:
- Asset Group “AQI city” to store all cities that belong to this tenant;
- Device Group “AQI Sensor” to store all devices that belong to city.

#### Rule Chains

**AQI Sensor** rule chain responsible for processing the received components (pollutants) produced by all sensors, and calculation of AQI based on them. 
The next step is to find and save the maximum AQI value for AQI City and the average values for temperature and humidity.

<div class="img-float" style="max-width: 50%;margin: 10px auto;">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-rule-chain-aqi-sensor-1.png" alt="Rule Chain - AQI Sensor">
</div>

1. Also, we described the conditions for creating Inactivity alarms.
At the first stage, we check the activity of the sensors:
- If sensor receives **Inactivity Event** - **Create Alarm** is applied;
- If **Activity Event** then we apply **Clear Alarm**.

2. <!--In order to ensure the display of the dynamic status of device activity on the Administration Dashboard, a separate type of telemetry - **"activityState"** has been created and recorded. The conditions have been prescribed that allow us to check the device's activity and store data about its status.-->

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-rule-chain-aqi-sensor-2.png" alt="Rule Chain - AQI Sensor">
</div>


3. We also use “alarms count” node to count **batteryLevel** and **Inactivity alarms**, if they are defined by conditions.

<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-rule-chain-aqi-sensor-3.png" alt="Rule Chain - AQI Sensor">
</div>

<br>

**AQI City** rule chain responsible for the simultaneous processing, change or duplicate of such attributes as *batteryLevelThreshold*, *inactivityTimeout* of all sensors participating in the creation of Alarm Rules.

<div class="img-float" style="max-width: 240px;margin: auto;">
<img src="https://thingsboard.io/images/solutions/air_quality_index/instruction-rule-chain-aqi-city-1.png" alt="Rule Chain - AQI City">
</div>

<!--**AQI City** and **AQI Sensor** Rule Chains are responsible for processing all telemetry about pollutants from devices and calculating it to the AQI. Also, the "alarms count" node is used to propagate alarms if it is defined under the conditions.
-->

#### Device Profiles

The device profile listed below uses pre-defined values for alarm thresholds. Administrator may configure alarm thresholds for all devices by navigating to alarm rules. Also, if the connection of the device was lost for [configured_time_in_hours] (4 hours by default) we call an alert. The major alarm is raised when the device does not appear in the network during the time that was configured by the administrator.

###### AQI Sensor

The profile is configured to raise alarms if the value of "Low Battery Level" is lower than a threshold. The major alarm is raised when the value is lower than what was configured by the administrator.

#### Alarms
Alarms are generated using two <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"AIR Sensor" <a href="/profiles/deviceProfiles" target="_blank">device profile</a>.
User may turn alarms on and off as well as configure the alarm thresholds via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Air Quality Monitoring"</a> dashboard using "Edit Sensor" form.


#### Devices

We have already created five sensors and loaded some demo data for them. See device info and credentials below:
**The solution expects that the sensor device will upload all pollution values, temperature, humidity, and battery level. The most simple example of the expected payload is in JSON format:**

```json
{"temperature": 42, "humidity": 73, “pm25”: 24.4, “pm10”: 30, “no2”: 13, “co”: 2.8, “so2”: 7, “o3”: 0.164, "batteryLevel": 77 }{:copy-code}
```

<br>

**To emulate the data upload on behalf of device "Air Quality Sensor 1", one should execute the following command:**

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Solution entities

As part of this solution, the following entities were created:
${all_entities}


#### Examples

##### How to trigger low battery alarm on Air Quality Sensor 1 (Hollywood)

Let's reproduce the event in which we will configure an alarm that will respond to the specified limit value of the battery level.

Let's take for example the sensor Air Quality Sensor 1 (Hollywood), which currently has a battery level of 43.73%.

<br>
<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-1-1.png" alt="">
</div>

<br>

In order to adjust the **Battery Level** alarm parameters, click on the “settings” button in the “Alarms” section, after which a pop-up will appear for setting the limit values for alarms.

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-1-2.png" alt="">
</div>

<br>

Set Alarm rules to 10% and save by pressing the "Save" button.

<br>

<div class="img-float" style="max-width: fit-content;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-1-3.png" alt="">
</div>

<br>

Then to emulate the “batteryLevel” data of device "Air Quality Sensor 1" let’s take value - ”9” for example, and then we should execute the following command:


```bash
curl -v -X POST -d "{\"batteryLevel\": 9 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Now we can see that the Battery Level of the Hollywood sensor is 9%, this level is below the limit of 10% that we indicated earlier - the alarm has been triggered.


<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-1-4.png" alt="">
</div>


In this way, we can manually set the threshold for triggering alarms to control the battery level of the sensor, and control through the Administration Dashboard.

##### Report high AQI on Air Quality Sensor 2 (Downtown) using PM2.5


In this example, we will  simulate the sending of a high-level pollutant PM 2.5 by a sensor.


The starting value of PM 2.5 in AQI equivalent is 52, you can see it in the picture below.


<div class="img-float" style="max-width: 60%;margin:10px auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-2-1.png" alt="">
</div>


As an example, let's take the **hazardous** level of PM 2.5 that the sensor can potentially send and it will be equal to **400 μg/m3**.
Then to emulate the “pm25” data of device "Air Quality Sensor 2" we should execute the following command:


```bash
curl -v -X POST -d "{\"pm25\": 400 }" ${BASE_URL}/api/v1/${Air Quality Sensor 2ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```


<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-2-2.png" alt="">
</div>
<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://thingsboard.io/images/solutions/air_quality_index/use-case-sensor-2-3.png" alt="">
</div>


After the data has been sent, you can see that the dashboard received the value of **PM 2.5 - 400 μg/m3** and calculated it as **AQI - 433**, which is a **hazardous** level of pollution.

### [Optional] Edge computing

Optionally, this solution can be deployed to the edge.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

In the scope of this solution, new <a href="${Remote Location R1EDGE_DETAILS_URL}" target="_blank">edge</a> entity "Remote Location R1" was created.

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"AQI City"** *ASSET* group;
* **"AQI Sensor"** *DEVICE* group;
* **"Air Quality Monitoring"** *DASHBOARD* group.
* **"Air Quality Monitoring Public"** *DASHBOARD* group.

ThingsBoard Edge is a separate service that must be installed, configured and connected to the cloud.
The easiest way to install ThingsBoard Edge is to use <a href="https://docs.docker.com/compose/install/" target="_blank">Docker Compose</a>.

Docker compose installation instructions are available on the edge details page.
Please navigate to <a href="${Remote Location R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button to see the instructions.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### Push data to device on edge

**"AQI Sensor"** *DEVICE* group was assigned to the edge entity "Remote Location R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Device groups** page.

To emulate the data upload on behalf of device "Air Quality Sensor 1" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" http://localhost:8080/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" http://localhost:18080/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "Air Quality Sensor 1" on edge, you'll be able to see telemetry update on the cloud for this device as well.