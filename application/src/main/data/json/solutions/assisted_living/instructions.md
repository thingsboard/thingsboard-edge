## Solution instructions

As part of this solution, we have created the "Assisted Living Administration" dashboard. We will review and describe each solution part below.

The solution is designed to be used with BLE or LoRa gateways and devices.
The rooms may be equipped with a number of sensors like room temperature, humidity, indoor air quality (IAQ), leak, smoke, and open/close detectors.
The geopositioning of the resident is done via the beacon in the wristband and a set of nearby gateways. 
The platform deduplicates the incoming message from the beacon and enriches it with the attributes of the nearby gateways. 
The geopositioning algorithm is relatively simple and based on the payload's RSSI parameter. 
One may improve the algorithm based on the particular use case.


<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="http://thingsboard.io/images/solutions/assisted_living/al-scheme.png" alt="Assisted Living">
</div>


### Assisted Living Administration Dashboard

The "Assisted Living Administration" dashboard
is intended for monitoring and controlling the status of residents, areas of the institution, devices, and their management. It has multiple states:

* **Main** state is assigned to provisions of residents, alarms of residents, and rooms. The Main state contains:
  * A section with an interactive scheme of zones and resident location markers that can be viewed in real-time. When the status of a resident or room changes or an alarm occurs, the marker will change. To get more detailed information, click on the Resident's marker, and a pop-up with detailed information about the Resident will be displayed. 
    The card contains detailed information about the Resident, as well as current vital signs, such as heart rate, temperature, panic button status, etc. You can also view vital statistics.
  
  * The Resident Alarm section is designed to display all alarms about residents health status or behavior. 
    You can track the following data: "type" of alarm, resident "name", "location", "duration" of alarm, "severity," and also perform one of the actions: "call an ambulance", "call nurse," or resolve the alarm. 
    By default, you can set the values (for major or severity) at which alarms will be triggered. These values are panic button (number of presses), heart rate (range from/to), body temperature (range from/to), and noise level. You can also determine the number of an ambulance and the number of a nurse.

  * The Room Alarm section is designed to display all alarms from the sensors located in the room. You can track the following data: “type”, “location”, “duration”, and “severity”, and also perform one of the actions: “call attendant” or resolve the alarm.
    By default, you can set the values (for major and critical) at which alarms will be triggered. These values are: Room temperature(range from/to in %), Room humidity(range from/to in C), Room air quality(range from/to in IAQ), Door open(duration in min), Window open(duration in min), Sensors battery level(in %), Water leaks and Smoke detected. You can also determine the number of the attendant.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/1-main-state.png" alt="Assisted Living">
</div>

The main state also contains links to the states of resident and zone management.
To switch to the Resident state - click on the “Residents” button on Main State.

<br>

* **Residents state** is assigned to resident management. You can create, edit or delete them, and if such users exist, follow them in the general list.
Basic data of residents is divided into the following data blocks: "Personal info", "Emergency contact", "Health information", "Location", "Wristband".

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://thingsboard.io/images/solutions/assisted_living/2-residents-state.png" alt="Assisted Living">
</div>
  
Click the “Zones” button on Main State to switch to the Zones state.

<br>

* **Zones state** is intended for the management of zones, which in the future will be the basis for rooms and devices. You can create, edit or delete a zone as needed. In order to create a new zone - click the "Add zone" button and then specify the name and add a mapping scheme. Then save the zone. In our example, we created the zones “Floor 1” and “Floor 2”. 

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="http://thingsboard.io/images/solutions/assisted_living/3-zones-state.png" alt="Assisted Living">
</div>


In order to go to the main state of a specific zone - click on its line, after which you will be redirected to the page.

<br>

  * **Zone State** is intended for room and device management.
You can create the desired room and define it in the corresponding location on the Zone map you downloaded earlier. After saving, the room will occupy the place you specified.
You can create a device of the appropriate type and attach it to the corresponding room, thus creating a connection between them.

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://thingsboard.io/images/solutions/assisted_living/4-zone-state.png" alt="Assisted Living">
</div>

<br>

**When you are trying to add devices, note that you can select only those devices that are in "Device Groups" -> "Unassigned Devices" on the "Customers hierarchy" page.** To add new devices on your dashboard, first create them on the "Customers hierarchy" page.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="http://thingsboard.io/images/solutions/assisted_living/5-customer-hierarchy.png" alt="Assisted Living" style="border: 1px solid #eee;">
</div>


### Rule Chains

* **AL Gateway Rule Chain** rule chain responsible for processing the data from the gateways: deduplication and enrichment of the payload with the signal strength and location of the gateway.

The "Fetch Room attributes" node enriches the incoming message with the location of the gateway. 
The "Change Owner from Gateway to Device" nodes transforms the incoming message and lookup associated device based on the value of serial number.
The "Switch by Device Type" node routes the incoming message to "Room" or "Wristband" rule chains.
The "Deduplicate From Multiple Gateways" combines all copies of the message from multiple gateways. Each copy contains parameters of the gateway including the RSSI. 
The "Use msg with Max RSSI" node calculates the location of the resident beacon based on the attributes of the closest gateway. 
  
* **AL Wristband Device Rule Chain** is very similar to default platform rule chain. The rule chain also count number of alarms and propagate the value to corresponding resident user.

* **AL Room Device Rule Chain** is very similar to "AL Wristband Device Rule Chain" but does not propagate alarm counts to the user.


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

#### Examples

##### How to call a resident's heart rate alarm?

Let's recreate an event where we will generate data that will trigger an alarm about a specific resident.
For example, let's take resident **"William Harris"**. His current vital heart rate is - 95 BPM.

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-1-1.png" alt="Assisted Living">
</div>

To check the current resident alarm settings, go to the "Notification rules" section in the "Resident alarms" section by clicking on the "gear" button.

You can see the heart rate alarm threshold for different alarm types.

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-1-2.png" alt="Assisted Living">
</div>

Then to emulate the resident's "pulse" data let's take the value: "55" for bpm. After that, we should execute the following command:

```bash
curl -v -X POST -d "{\"serial\": \"C00000025FE2\", \"data\":{\"pulse\":55}}" ${BASE_URL}/api/v1/${D00000020002ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Since the BPM indicator is equal to 55 and falls under the requirements for calling an alarm - the alarm with the type "heart rate" for the resident "William Harris" was displayed in the "Resident alarms" section, and its marker was also highlighted in red.

<div class="img-float" style="max-width: 55%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-1-3.png" alt="Assisted Living">
</div>


##### Moving a resident from one room to another

We remind you that the system is also intended for auditing the movement of residents of the institution.

Therefore, for example, let's create a case in which the system tracks and displays the movement of a guest.

Take, for example, **"Isabella Davis"**, who is in her room on the **Floor 1**.

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-2-1.png" alt="Assisted Living">
</div>

The system determines the placement of residents using a bracelet that transmits the corresponding signal and a Gateway placed in the corresponding room/zone that processes it. Thus, the Gateway with the best connection level with the bracelet is considered the resident's location.

To emulate the data for moving the resident, we will generate the gateway data, namely **“rssi”**.

In our case, we will reproduce the data of several Gateways with different bracelet coverage levels. Let's take the "rssi" value for "Room 103": "-10"(better connection) and Room 104: "-70"(worse connection). After that, we should execute the following command:

```bash
curl -v -X POST -d "{\"serial\": \"C00000066F66\", \"rssi\": -10, \"data\":{\"batteryLevel\":55}}" ${BASE_URL}/api/v1/${D00000030003ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" && curl -v -X POST -d "{\"serial\": \"C00000066F66\", \"rssi\": -70, \"data\":{\"batteryLevel\":55}}" ${BASE_URL}/api/v1/${D00000040004ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" {:copy-code}
```


<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="http://thingsboard.io/images/solutions/assisted_living/example-2-2.png" alt="Assisted Living">
</div>

After using this command, we can see that after the data transfer, **Isabella Davis** moved to **“Room 103”** because his gateway connection signal was better than “Room 104”.

<br>

##### Room alarm when the door is opened

This time we will reproduce the alarm of the room sensor that monitors the IAQ level.

To check the current room alarm settings, go to the "Notification rules" section in the "Room alarms" section by clicking on the "gear" button.

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-3-1.png" alt="Assisted Living">
</div>

As we can see, the room IAQ level alarm will go off if the value exceeds 150.

Next, for our example, let's take a resident's room, for instance - Room 101, in which the IAQ level is at the permissible level - "67".

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-3-2.png" alt="Assisted Living">
</div>

To emulate the room's "iaq" data, let's take the value: "160". After that, we should execute the following command:

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://thingsboard.io/images/solutions/assisted_living/example-3-3.png" alt="Assisted Living">
</div>



```bash
curl -v -X POST -d "{\"serial\": \"E00000015FE1\", \"rssi\": -50, \"data\":{\"IAQ\":160}}" ${BASE_URL}/api/v1/${D00000010001ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" {:copy-code}
```

<br>

We can see that the IAQ has changed by 160, so its level exceeds the indicators specified in the rules for calling alarms. Therefore, in the "Room alarms" section, we can track the appearance of an alarm with the type "Air Quality" - Room 101.


### Devices

We have already created devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}

### Solution entities

As part of this solution, the following entities were created:

${all_entities}


