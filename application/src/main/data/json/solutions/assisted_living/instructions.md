## Solution instructions

As part of this solution, we have created the "Assisted Living Administration" dashboard. We will review and describe each solution part below.

### Dashboards

##### Assisted Living Administration Dashboard

The <a href="${Assisted LivingDASHBOARD_URL}" target="_blank">"Assisted Living"</a> dashboard
is intended for monitoring and controlling the status of residents, areas of the institution, devices, and their management. It has multiple states:

* **Main** state is assigned to provisions of residents, alarms of residents, and rooms. The Main state contains:
  * A section with an interactive scheme of zones and resident location markers that can be viewed in real-time. When the status of a resident or room changes or an alarm occurs, the marker will change. To get more detailed information, click on the Resident's marker, and a pop-up with detailed information about the Resident will be displayed. 
    The card contains detailed information about the Resident, as well as current vital signs, such as heart rate, temperature, panic button status, etc. You can also view vital statistics.
  
  * The Resident Alarm section is designed to display all resident alarms. You can track the following data: "type" of alarm, resident "name", "location", "duration" of alarm, "severity," and also perform one of the actions: "call an ambulance", "call nurse," or resolve the alarm. 
    By default, you can set the values (for major or severity) at which alarms will be triggered. These values are panic button (number of presses), heart rate (range from/to), body temperature (range from/to), and noise level. You can also determine the number of an ambulance and the number of a nurse.

  * The Room Alarm section is designed to display all room-related alarms. You can track the following data: “type”, “location”, “duration”, and “severity”, and also perform one of the actions: “call attendant” or resolve the alarm.
    By default, you can set the values (for major and critical) at which alarms will be triggered. These values are: Room temperature(range from/to in %), Room humidity(range from/to in C), Room air quality(range from/to in IAQ), Door open(duration in min), Window open(duration in min), Sensors battery level(in %), Water leaks and Smoke detected. You can also determine the number of the attendant.

    The main state also contains links to the states of resident and zone management.
To switch to the Resident state - click on the “Residents” button on Main State.

* **Residents state** is assigned to resident management. You can create, edit or delete them, and if such users exist, follow them in the general list.
Basic data of residents is divided into the following data blocks: "Personal info", "Emergency contact", "Health information", "Location", "Wristband".
  
Click the “Zones” button on Main State to switch to the Zones state.

* **Zones state** is intended for the management of zones, which in the future will be the basis for rooms and devices. You can create, edit or delete a zone as needed. In order to create a new zone - click the "Add zone" button and then specify the name and add a mapping scheme. Then save the zone. In our example, we created the zones “Floor 1” and “Floor 2”. 

In order to go to the main state of a specific zone - click on its line, after which you will be redirected to the page.

  * **Zone State** is intended for room and device management.
You can create the desired room and define it in the corresponding location on the Zone map you downloaded earlier. After saving, the room will occupy the place you specified.
You can create a device of the appropriate type and attach it to the corresponding room, thus creating a connection between them.

### Rule Chains

* AL Gateway Rule Chain
* AL Wristband Device Rule Chain
* AL Room Device Rule Chain


### Device Profiles

The device profile listed below uses pre-defined values for alarm thresholds. Administrator may configure alarm thresholds for all devices by navigating to alarm rules.

##### Wristband
The profile by default is configured to raise alarms if:
* the value of "panicButton" is TRUE and repeated 1 time for Major alarm, and 2 and more times for Critical alarm;
* the value of "pulse" is lower or greater than a threshold. Also Major and Critical alarms for Heart Rate defined by the administrator;
* the value of "temperature" is less or greater than a threshold. Also Major and Critical alarms for Body Temperature defined by the administrator;
* the value of "noise" is equal or greater than a configured. Also Major and Critical alarms for Noise defined by the administrator;
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;

##### Window Sensor
The profile by default is configured to raise alarms if:
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "windowOpen" is equal or greater than a configured. Also Major and Critical duration of alarms for Window opened defined by the administrator;

##### Smoke Sensor
The profile by default is configured to raise alarms if:
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "smoke" is TRUE.

##### Room Sensor
The profile by default is configured to raise alarms if:
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "roomIaq" is equal or greater than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "roomTemperature" is less or greater than a threshold. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "roomHumidity" is less or greater than a threshold. Also Major and Critical alarms for Battery level defined by the administrator;

##### Leak Sensor
The profile by default is configured to raise alarms if:
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "waterLeak" is TRUE.

##### Door Sensor
The profile by default is configured to raise alarms if:
* the value of "battery" is equal or less than a configured. Also Major and Critical alarms for Battery level defined by the administrator;
* the value of "doorOpen" is equal or greater than a configured. Also Major and Critical duration of alarms for Door opened defined by the administrator;


### Devices

We have already created devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}


### Solution entities

As part of this solution, the following entities were created:

${all_entities}

Examples





