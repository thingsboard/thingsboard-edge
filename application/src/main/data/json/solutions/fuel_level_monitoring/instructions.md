## Solution instructions

As part of this solution, we have created the Fuel Level Monitoring dashboard. We will review and describe each solution part below.

#### Fuel Level Monitoring dashboard

This dashboard is intended to monitor the remaining fuel in the tanks, view consumption statistics, manage devices, and respond to changes in defined conditions using the alarm system.

- The **main state** is designed to monitor the remaining fuel and control the placement of tanks, device management, and the alarm system. This page contains the following elements:



  - the section with an ***interactive map*** displays the location of the tanks with the help of markers. The marker also informs about the current status of the sensor, namely: green - the sensor is in a normal state, and the rules for triggering alarms are not applied;  yellow - low battery; red - at least one of the conditions for starting a warning, for example, a low level of fuel remaining or low/high temperature, is used; gray - the sensor is in mode offline. To get more information, click on the tank marker - a popup with detailed information will appear.


    The user can use the map filter - map switches that will help sort the display according to requirements.	


  - the ***Tanks section*** is a list designed to display all existing tanks. You can delete or edit existing ones. The main list of “Tanks” contains the following data: “Total label”, “Remaining, %”, “Temperature”, “Battery”, “Connection” and action buttons. The user can create/add new sensors by clicking on the "+" button. To create a sensor, the user must go through the following steps: "General info", "Tank info," and "Set location". Note that we provided the ability to add and define nine types of tanks, which we can calculate based on their geometric parameters. See detailed information in the **"Tank Creation"** paragraph.

  - the ***Alarms section*** is designed to display all alarms related to the remaining fuel level, temperature, and battery level. You can set the conditions under which alarms will be triggered by clicking the “Alarm Rules” button. By default, the following types of alarms are defined: “Low battery level”, “Low temperature”, “High temperature” and “Low remaining level”.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-1.png" alt="Fuel level monitoring">
</div>

The user can go to the Tank state in several ways: click on the line in the Tanks section of a specific tank or click the "Details" button on the popup when clicking on the marker of the interactive map.

- **Tank state** is designed to display information about a specific tank. This page contains the following elements:

  - fuel remaining display widget;
  - section for displaying detailed tank information: “Tank Name”, “Serial number”, “Liquid type”, “Tank temperature”, “Battery level”, “Connection” and “Last Update”. Also, using the functionality of this section, you can edit the main fields - for this, click the "Edit" button, as well as change the location of the tank, and the marker on the map - by clicking on the "Edit Map" button;
  - the Consumption and remaining fuel section is a table with a list of consumption, remaining, and fuel replenishment. The ability to monitor the duration of refueling or filling the tank with fuel and the timestamp of the action has also been added;
  - the Remaining chart is designed to display the statistics of the tank's remaining and fuel consumption in the form of a graph, which is shown in terms of volume/% and time intervals;
  - the Alarm section is a list of alarms for a specific tank.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-3.png" alt="Fuel level monitoring">
</div>

#### Tank creation

As we have already said, we created nine tanks of different geometric shapes for our template. This will allow easy use of ready-made templates to calculate the volume of your tank.

<div class="img-float" style="max-width:45%;margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/tank-shapes.png" alt="Fuel level monitoring">
</div>

To create a tank, the user needs to go through 3 main steps, namely:

- **General Info** - the user must fill in mandatory fields, such as: "Serial number" and "Sensor label".

After that, the user must press the "Next" button to go to the next step - "Tank Info". Users can cancel and close the window anytime by clicking the "Cancel" button.

<br>

- **Tank Info** - this step contains basic information about the tank's parameters, sensor, and fuel type. The entered data will directly affect the final result of calculating the tank's volume.

In the **"Tank Info" section**, the user must select one of the tank shapes.


Next, the user needs to select the desired measurement system in the "Measurement system" field, which is represented by two options - "Metric" and "Imperial". Then select "Dimension units" -
this list will be determined after selecting the appropriate measurement system. For "Metric" - "mm", "cm", "m", and for "Imperial" - "ft", "in".

Measurement fields like "length", "height", and so on will be automatically adjusted depending on the selected tank shape, and the user will only have to enter the required values.

When all the necessary fields have been filled in, selecting "Capacity output" is essential, determining in what value the final volume calculation will be output. After selecting the appropriate parameter, the user can see the volume - "Capacity" of the given tank of the corresponding parameters.

<div class="img-float" style="max-width:35%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-2-1.png" alt="Fuel level monitoring">
</div>


<br>

In the **"Sensor Info" section**, the user will have the opportunity to choose the tank "level measurement type", the "measurement system" and "sensor reading units" for the data that the tank sensor will potentially send.

Various types of sensors can be used to calculate the volume of the tank, so we have provided the option of selecting the "level measurement type". This means we can connect most liquid-level measurement sensors, which offers unlimited flexibility in using the example.

The measurement type has two parameters:
  - "Fill height" - the direct value of the height of the liquid in the tank (this space can be measured, for example, by a float sensor);
  - "Remaining space" - the value of the height of the empty space from the top of the tank to the beginning of the liquid (an ultrasonic sensor can measure this height).


<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-2-3.png" alt="Fuel level monitoring">
</div>

We also anticipated the possibility of a technical neck of the tank, which can affect the volume calculation. Therefore, when selecting the "Remaining space" parameter, an additional field - **"Sensor gap"** will be displayed. This field will help exclude the neck's height when calculating the volume tank.

<br>

**"Liquid info"** is the last section. Here the user will be able to choose the desired type of fuel from the list.

After correctly filling in all the relevant fields of the "Tank Info" step, the user can proceed to the next step - choosing the location of the tank.

<br>

- **Set location** - the stage at which the user can choose the location of the tank. The interactive map allows you to select a point on the map manually.

<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-3.png" alt="Fuel level monitoring">
</div>

After all, stages have been completed, the user can save all changes by pressing the "Save" button - this tank will be placed in the general list of tanks, and the corresponding marker will be displayed on the interactive map.

<br>

***For the completeness of the picture, you can test the functionality and simulate the data of the tank. To do this, go to the "Examples" section to find practical examples and instructions.***

#### Rule Chains

The **"Fuel Monitoring"** rule chain is processing all incoming messages from tank sensors. This rule chain is responsible for counting alarms of all types (temperature, battery, fuel level, fuel height) and updating the status of the tank sensor based on alarms count and connectivity of device.

<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/rule-chain.png" alt="Fuel level monitoring">
</div>

#### Device Profiles

The device profile listed below uses predefined values for alarm thresholds. Administrators may configure alarm thresholds for all devices by navigating to alarm rules.


###### Tank Sensor

The profile by default is configured to raise alarms if:
- for Low Fuel Level the value of "fuelLevel" is equal or less than a configured. By default, the value is set to 10%;
- for Low Battery Level the value of "battery" is equal or less than a configured. By default, the value is set to 20%;
- for High Temperature Level the value of "temperature" is equal or less than a configured. By default, the value is set to 80 C;
- for Low Temperature Level the value of "temperature" is equal or greater than a configured. By default, the value is set to 0 C.



#### Alarms
Alarms are generated using four <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"Tank Sensor" <a href="/profiles/deviceProfiles" target="_blank">device profile</a>.
User may configure the alarm rules via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Fuel Level Monitoring"</a> dashboard using "Alarm rules" form.



#### Devices

We have already created nine sensors and loaded some demo data for them. 
**The solution expects that the sensor device will upload temperature, fuel and battery level.  The most simple example of the expected payload is in JSON format:**

```json
{"battery": 77, "fuelLevel": 91, "fuelHeight": 125, "temperature": 32 }{:copy-code}
```

<br>

**To emulate the data upload on behalf of device "Tank Sensor" - "Tank 1273", one should execute the following command:**

```bash
curl -v -X POST -d "{\"battery\":  77, \"fuelLevel\":  91, \"temperature\": 32 }" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**You can also emulate the "fuelHeight" data upload on behalf of device "Tank Sensor" - "Tank 1273", one should execute the following command:**

```bash
curl -v -X POST -d "{\"fuelHeight\":  100}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.


### Solution entities

As part of this solution, the following entities were created:
${all_entities}



#### Examples

### Alarms elimination

In the first example, we will eliminate all existing alarms outside the set alarm rules.

As you can see on the screen (highlighted), we have alarms related to "High temperature", "Low Fuel Level", and "Low Battery Level".


<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-2.png" alt="Fuel level monitoring">
</div>

In our case, we will consider a specific tank, namely "Tank 1273". Its current telemetry shows us that "Tank temperature" is 82 C, "Battery level" is 9%, and "Remaining" level is 5%(465 Gal from 9306).

Since our main task is to eliminate alarms, let's simulate sending data that will not fall under the rules of alarms - accordingly, they will disappear.


<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-1.png" alt="Fuel level monitoring">
</div>


Let's look at the configured alarm rules, which determine the rules for calling alarms.

As you can see, the rules define the conditions, namely:
- if the tank temperature is less than 0 C or more than 80 C;
- if the remaining fuel is less than 10%;
- if the battery level is less than 20%.


<div class="img-float" style="max-width:60%;margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-2.png" alt="Fuel level monitoring">
</div>

<br>

Based on these rules, we collect data that do not fall under these conditions.

For example, "Tank temperature" - 25 C, "remaining" level - 100% (simulation of refueling), and "Battery level" - 100% (full charge/replacement of the battery).

<br>

To send the relevant data, you need to use the following command:

```bash
curl -v -X POST -d "{\"battery\":  100, \"fuelLevel\":  100, \"temperature\": 25 }" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Good job. The data was sent successfully, and as a result, the alarm was cleared.


<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-3.png" alt="Fuel level monitoring">
</div>




#### Level measurement representation

As mentioned above, our dashboard can calculate the volume of the presented tank shapes based on the **"filling height"** or **"remaining space"**.

In our examples, we will analyze an option showing how to achieve the same result differently.

###### Filling height

First, let's look at the "filling height" example.

As a basis, we will take the tank from the previous example, namely **"Tank 1273"**.


<div class="img-float" style="max-width:45%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-1.png" alt="Fuel level monitoring">
</div>

Next, we need to specify the height value that the sensor will send us. As we can see, the "height" of the tank is 200 cm, and the total volume calculated based on all parameters is 1570.8 L. "Level measurement type" - "Fill height", "sensor reading units" - cm.

Next, we need to ground the height value that the sensor will send us. For the comfort of calculation, let's use the value - 100 cm, half of the tank's height (200 cm). To get this value, we need to use the following command:

```bash
curl -v -X POST -d "{\"fuelHeight\":  100}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

As you can see, the data has been sent successfully, and the tank volume is 50% (785 L).

<div class="img-float" style="max-width:65%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-2.png" alt="Fuel level monitoring">
</div>

<br>

###### Remaining space

In the following example, I will show you how to use the height of the **"remaining space"**.

We will also take **"Tank 1273"** as a basis. However, as you can see, we have an additional value, namely **"Sensor gap"**. Let me remind you that a "Sensor gap" in some instances will play the role of a deal that will not be taken into account in the height of the tank (for example, the neck of the tank) but will only be a "hint" to the system that this height should not be taken into account.

<div class="img-float" style="max-width:45%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-1-1.png" alt="Fuel level monitoring">
</div>

We repeat the same steps for height emulation, but do not forget about the "gap".

We will use the value **110** cm (100 cm - "remaining space", 10 cm - gap) for easy calculation.

<br>

***For this example, don't forget to change "Level measurement space" to "Remaining space." Each type has a different approach to calculating tank volume.***

<br>

To get this value, we need to use the following command:

```bash
curl -v -X POST -d "{\"fuelHeight\":  110}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<div class="img-float" style="max-width:65%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-2.png" alt="Fuel level monitoring">
</div>

Data was sent successfully. And we determined the volume using this method.

<br>

We can calculate the tank's volume in two ways without too much effort. Try to practice your options to fully understand the operation of the **Fuel Level Monitoring** solution template.