## Solution instructions

As part of this solution, we have created 2 dashboards that display data from multiple sensors.

We will review and describe each solution part below:

### Dashboards
#### Public Air Quality Monitoring Dashboard

Use the link to insert this dashboard to your site: 
``` 
(your_domain_name)${MAIN_DASHBOARD_PUBLIC_URL}
``` 

This dashboard is designed for general monitoring of air pollution and provides general recommendations in a convenient to people form. It has multiple states:

- **City state** represents the air pollution monitoring of a specific city (in our case Los Angeles) and calculates the average value based on the AQI received from city devices. Also, this state contains the following elements:
  - The name of the current city
  - Temperature and humidity
  - Pollution status according to EPA
  - Average AQI value based on all devices in the city and a scale for convenient viewing of pollution
  - Click on "i" info-icon and a pop-up with the legend of pollutants will appear
  - A section with general recommendations for sensitive groups of people regarding the current level of pollution
  - History section of AQI level in the current, weekly and monthly range
  - Interactive map showing air pollution monitoring devices, the markers of which are highlighted depending on the AQI level
  - Click on the device marker on the map and go to the Device state. (you can switch to other devices by clicking on other device markers)


- **Device state** represents the current state of the specific device. This state contains the following elements:
  - Contains the same elements as the **City state**, but the data is based on the device
  - Section of specific pollutants which include: PM2.5, PM10, NO2, CO, SO2, O3. 
    Click on one of these tiles and a pop-up will appear, which will display a description, general recommendations, as well as its statistics for this pollutant.

#### Administration Air Quality Monitoring Dashboard

This dashboard is designed to control the public dashboard and has multiple states:

-  **Main state** which is intended for monitoring devices, alarms, etc. The Main state contains:
   - The **Sensors** section. 
    All sensor data is displayed in a table where you can see the following information: “Sensor Label”, “Sensor id”, “Connection”, “Battery level” and “Last AQI”.
    You can also add a new sensor, edit and delete a sensor.
    Click on a specific device from the table and go to the **Device state**

   - **Alarms** section.
    All alarm data is displayed in a table where you can see the following information: “Created time”, “Type”, “Sensor id” and “Status”.
    Click on the “Settings” icon where you can define the alarm rules manually.
    

   - **Interactive map**. After selecting the marker of the device on the map, a pop-up will appear with information about it.
    Click on **“Details”** and go to the selected **Device state**.

- **Device state** allows you to view detailed information about the device. It contains the following sections:
   - Sensor details that show information about the device.
    Click the **“Edit”** button and you will be able to edit information.

   - Sensor measures that show next data: “connection” status, “Battery level”, “Last AQI”, “PM2.5”, “PM10”, “NO2”, “CO”, “SO2”, “O3”
   - Battery level chart
   - A Map which shows the location of sensor, allows you to move, delete and restore the device marker
   - Sensor alarms table 
   - Connection status chart

#### Entity Groups

Tenant has:
- Asset Group “AQI city” to store all cities that belong to this tenant
- Device Group “AQI Sensor” to store all devices that belong to city

#### Rule Chains

**AQI City** and **AQI Sensor** Rule Chains are responsible for processing all telemetry about pollutants from devices and calculating it to the AQI. Also, the "alarms count" node is used to propagate alarms if it is defined under the conditions.

#### Device Profiles

The device profile listed below uses pre-defined values for alarm thresholds. Admin may configure alarm thresholds for all devices by navigating to alarm rules. Also, if the connection of the device was lost for [configured_time_in_hours] we call an alert. The major alarm is raised when the device does not appear in the network during the time that was configured by the administrator.

###### AQI Sensor

The profile is configured to raise alarms if the value of "batteryLevel" is lower than a threshold. The major alarm is raised when the value is lower than what was configured by the administrator.

#### Alarms
Alarms are generated using two <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"AIR Sensor" <a href="/deviceProfiles" target="_blank">device profile</a>.
User may turn alarms on and off as well as configure the alarm thresholds via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Air Quality Monitoring"</a> dashboard using "Edit Sensor" form.


#### Devices

We have already created five sensors and loaded some demo data for them. See device info and credentials below:
${device_list_and_credentials}
**The solution expects that the sensor device will upload all pollution values, temperature, humidity, and battery level. The most simple example of the expected payload is in JSON format:**
```json
{"temperature": 42, "humidity": 73, “pm25”: 24.4, “pm10”: 30, “no2”: 13, “co”: 2.8, “so2”: 7, “o3”: 12, "batteryLevel": 77 }
```


**To emulate the data upload on behalf of device "Air Quality Sensor 1", one should execute the following command:**
```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  12, \"batteryLevel\":  77 }" https://thingsboard.cloud/api/v1/eTCUxcClWs5hgzpnNagC/telemetry --header "Content-Type:application/json"
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Solution entities

As part of this solution, the following entities were created:
${all_entities}










<!--
The dashboard has two states. The first public dashboard provides a basic set of widgets for monitoring the city's polluters.
The main widgets are monitoring current AQI and recommendations for sensitive groups of people, displaying a set of specific pollutants, data analytics in the charts, and an interactive map of devices.

You may use the **public dashboard** for:
* Monitoring AQI level by using a dashboard with all the necessary widgets as a level of the city’s AQI, air pollutants, and statistics
* View specific pollutants and their legend
* Get general recommendations to sensitive groups of people on major pollutants
* Get temperature and humidity data
* Observe devices on the interactive map;

Everyone will be able to easily use the public dashboard to place on their own resource and use all the possibilities of this solution for their own needs.

The admin dashboard contains the ability to create, edit, delete and maintain devices and control the alert system. The admin dashboard isn’t available in the main public state.

You may use the **admin dashboard** for:
* Manage air monitoring sensors;
* Supervise and maintain the alert system;
* Observe battery level, connection statuses of the devices.

You may always customize the <a href="#">"Air Quality Monitoring"</a> dashboard using the dashboard development <a href="https://thingsboard.io/docs/user-guide/dashboards/" target="_blank">guide</a>.


### Devices

We have already created five sensors and loaded some demo data for them. See device info and credentials below:
${device_list_and_credentials}

Solution expects that the sensor device will upload "aqi" and all pollution values. 
The most simple example of the expected payload is in JSON format:
```json
{"temperature": 42, "humidity": 73, "aqi": 77, “PM 2.5”: 24.4, “PM 10”: 30, “NO2”: 13, “CO”: 2.8, “SO2”: 7, “O3”: 12}{:copy-code}
```

To emulate the data upload on behalf of device "Air Quality Sensor 1", one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"PM 2.5\":  24.4, \"PM 10\":  30, \"NO2\":  13, \"CO\":  2.8, \"SO2\":  7, \"O3\":  12}" ${BASE_URL}/api/v1/${Sensor T1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Alarms

Alarms are generated using two <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"AIR Sensor" <a href="/deviceProfiles" target="_blank">device profile</a>.
User may turn alarms on and off as well as configure the alarm thresholds via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Air Quality Monitoring"</a> dashboard using "Edit Sensor" form.

### Customers

"Sensor 1" is assigned to a newly created customer "Customer D".
You may notice that "Customer D" has two users, and the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Air Quality Monitoring"</a> dashboard is accessible for those users.
You may create more Customers and more Users via administration UI.

${user_list}

### Solution entities

As part of this solution, the following entities were created:

${all_entities}

-->