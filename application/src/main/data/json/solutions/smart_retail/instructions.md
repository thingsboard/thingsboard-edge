## Solution instructions

As part of this solution, we have created 2 dashboards, 8 device profiles and 20+ test devices with simulated data. 
We have also created 2 test Customers with test Users for you to see how your customers may use this solution.

We will review and describe each solution part below:

### Dashboards

##### Smart Supermarket Administration

The <a href="${Smart Supermarket AdministrationDASHBOARD_URL}" target="_blank">"Smart Supermarket Administration"</a> dashboard
is designed to provision customers, their users, supermarkets and devices. It has multiple states:

* **Main** state allows you to list the retail companies (customers). 
  We assume that the customer is a retail company that own one or multiple supermarkets.
  We have provisioned two "fake" retail companies with number of supermarkets for demonstration purposes.
    * Click the "+" button in the top right corner of the companies table to create a new customer. 
      You may input the company name, address and other information;
    * Click the "Manage devices" button in the corresponding company row to open the device management state;
    * Click the "Manage users" button in the corresponding company row to open the user management state;
    * Click the "Manage supermarkets" button or simply the corresponding company row to open the supermarket management state;
* **Device management** state allows you to manage devices in scope of the retail company (customer).
  You may provision new devices or delete existing devices. The state displays a table with all devices assigned to this retail company.
  This means that Tenant or Supermarket Administrator will be able to use those devices to position them in the Supermarket.
  You may treat this list as a pool of devices that are available for installation in the Supermarkets of the Customer. 
    * Click the "+" button in the top right corner of the devices table to create a new device.
      You will be able to input device name, label and type. 
      The device name is a unique identifier of the device (e.g. serial number or  MAC address), 
      while device label is non-unique custom label (e.g "Fruits shelf" or "Ice Cream Freezer").
      List of available device types corresponds to the pre-configured device profile for this solution. 
      See **Device profiles** for more details.
      **Note:** Instead of provisioning devices manually, you may navigate to the "Supermarket Devices" group of the customer and use bulk provisioning feature. 
    * Click the "Edit" button in the corresponding device row to edit the device.      
    * Click the "Delete" button in the corresponding device row to delete the device.
* **Supermarket management** state allows you to manage supermarkets in scope of the retail company (customer).
    The dashboard state displays supermarkets on the map and a list of supermarkets in the table.  
    Supermarkets are assets that may contain multiple devices and few attributes: floor plan and address.
    * Click the supermarket marker in the map to open supermarket details in the right panel and open the popup which contains two links:
        * "Supermarket devices" link will open the supermarket devices state
        * "Delete" link will delete the supermarket asset.
    * Click the "+ Add supermarket" button to provision new supermarket asset. 
      The dialog will prompt for supermarket name. Once you input the name and click "Add supermarket", new asset will be created.
      The corresponding marker will be placed in the hard-coded location on the map. 
      You will be able to drag the market to define precise location of the asset.
    * Supermarket details panel allows you to provision the floor plan and define both address and location. 
      Although it is possible to lookup location based on address, this feature requires API key for Google Map 
      or similar service and is not included into solution out-of-the-box. You may drag and drop marker on the map to change the location.
    * Click "Supermarket devices" button to navigate to the supermarket devices state. 
* **Supermarket devices** state displays an indoor map with the floor plan of supermarket and device markers.
    You may drag-and-drop the device markers to define precise location of the device in the supermarket.
    * Click the device marker on the map to open the popup which contains two links:
        * "Update label" link will allow you to edit the device label.
        * "Remove device" link will un-assign the device from the supermarket and make it available for use in other supermarkets of the customer; 
    * Click the "+ Add device" button to select new device from the pool of available devices. 
      Once selected, you may change the device label and click "Add device" button. 
      The device will be placed in the center of the floor plan
      You may add more devices to the pool via **Device management** state. 

##### Smart Supermarket

The <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Smart Supermarket"</a> dashboard
is designed for supermarket managers to monitor state of the supermarket and react on alarms. It has multiple states:

* **Main** state contains a map of the supermarkets and a list of alarms.
  Alarms are propagated from devices to the corresponding supermarket.
  The platform calculates state of each supermarket based on the highest severity of the propagated alarms.
  As a user, you are able to filter supermarkets on the map based on the state of the supermarket. 
    * Click the "Critical" button to open a popup with critical alarms table.
    * Click the "Major" button to open a popup with major alarms table.
    * Click on the supermarket marker to open the supermarket details in the right panel of the dashboard.
    * Alarms view allows you to clear and acknowledge the alarms.
    * Click on the supermarket to open the "floor plan" state.
* **Floor plan** state contains an indoor map with the floor plan of supermarket and device markers.
  Besides the map, state also contains two filters: based on device type and device state.
  Filter settings are persisted on the user level.
    * State filter allows you to filter devices based on the highest severity of the alarms. 
      For example, you may choose to display devices that have at least one critical alarm.
    * Device type filter allows you to show or hide specific devices based on the type of device.
      For example, you may display only Freezers and Chillers and hide all other devices.
    * Click on specific device marker to display device details state in the right panel of the dashboard.
      Content of the device details is specific to the device type. 
      For example, freezer device have a line chart with the temperature readings while smart bin has a bar chart with the fullness level.
      Nevertheless, the common elements of the device details is the header and alarms list.
      Header contains information about current state of the device and it's battery level (if device is battery powered). 
      Header also allows you to navigate to the settings of the particular device. Those settings allow you to configure the alarm thresholds.
      
### Roles

The "Smart Retail Read Only" role is created to share the read-only access to "Smart Supermarket" dashboards with all users of all customers.

The "Smart Retail User" role is for Supermarket Users. This role allows read only access to all entities and write access to alarms and device/asset attributes.

The "Smart Retail Administrator" role is for Supermarket Administrators. This role allows write access to devices and assets within a specific Customer(if assigned to Customer User) or even entire Tenant (if assigned to Tenant User). 

### Entity Groups

Each Customer has:

 * asset group "Supermarkets" to store all supermarkets that belong to this customer.
 * device group "Supermarket Devices" to store all devices that belong to this customer.
 * device group "Unassigned Devices" to store devices that are not yet assigned to any supermarket.
 * user group "Smart Retail Users" to store users with "Smart Retail User" role.
 * user group "Smart Retail Administrators" to store users with "Smart Retail Administrator" role.
   
Tenant has:

 * dashboard group "Supermarket Users Shared" to share "Smart Supermarket" dashboard in read-only mode with all Customer user groups named "Smart Retail Users".
 * dashboard group "Supermarket Admins Shared" to share "Smart Supermarket Administration" dashboard in read-only mode with all Customer user groups named "Smart Retail Administrators".


### Rule Chains

The "Supermarket Devices" Rule Chain is responsible for processing all telemetry from devices and raising the alarms. The "alarms count" node is used to propagate alarm counts to Tenant, Customer and Supermarket assets.

### Customers

Supermarkets "S1" and "S2" are assigned to a newly created customer "Retail Company A". Supermarket "S3" is assigned to customer "Retail Company B".

You may notice that both "Retail Company A" and "Retail Company B" has two users.
One of the users is a supermarket manager with default dashboard "Smart Supermarket Administration" assigned.
The other user is a supermarket user with default dashboard "Smart Supermarket" assigned.

You may create more Customers and more Users via <a href="${Smart Supermarket AdministrationDASHBOARD_URL}" target="_blank">Smart Supermarket Administration</a> dashboard.

**User list**

${user_list}

### Device Profiles

The device profile listed below use pre-defined values for alarm thresholds. This values are common for all devices that share same device profile.
Supermarket manager may tune alarm thresholds for each specific device by navigating to device details via <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Smart Supermarket"</a> dashboard.

##### Smart Shelf

The profile is configured to raise alarms if the value of "weight" telemetry is lower than a threshold.
Major alarm is raised when the value is below 20 units (kg or lbs depends on what is reported by the device).
Critical alarm is raised when the value is below 10 units.

Sample device payload:

```json
{"weight": 42}
```

##### Freezer

The profile is configured to raise alarms if the value of "temperature" telemetry is above or below certain thresholds.
Major alarm is raised when the value is above -2 degrees or below -25.
Critical alarm is raised when the value is above -1 degrees or below -30.

Sample device payload:

```json
{"temperature": -5.4}
```

##### Chiller

Chiller profile is very similar to Freezer but with different default threshold values.


Sample device payload:

```json
{"temperature": 6.2}
```

##### Door sensor

The profile is configured to raise major alarm if the door is left open for more than 30 minutes or critical alarm if the door is left opened for 1 hour.
The profile is also configured to raise critical alarm if the door is opened during non-working hours. You may configure schedule of the non-working hours in the alarm rule of the device profile.

Since door sensors are usually battery powered, the corresponding alarms are raised when the battery level is below 30(major) or 10(critical) percent.
If your sensor is not battery powered, you may simply ignore the alarm rule.

Sample device payload:

```json
{"open": true, "batteryLevel":  99}
```

##### Motion sensor

Similar to Door sensor, motion sensor is configured to raise critical alarm if the motion is detected during non-working hours.
You may configure schedule of the non-working hours in the alarm rule of the device profile.

```json
{"motion": true, "batteryLevel":  99}
```

##### Smoke sensor

Smoke sensor will raise critical alarm if the smoke is detected.
Since smoke sensors are usually battery powered, the corresponding alarms are raised when the battery level is below 30(major) or 10(critical) percent.
If your sensor is not battery powered, you may simply ignore the alarm rule.

```json
{"alarm": false, "batteryLevel":  99}
```

##### Smart Bin

The profile is configured to raise alarms if the fullness level is above certain threshold.
Major alarm is raised when the level is above 70%.
Critical alarm is raised when the level is above 90%.

Smart bin sensors are usually battery powered, the corresponding alarms are raised when the battery level is below 30(major) or 10(critical) percent.
If your sensor is not battery powered, you may simply ignore the alarm rule.

Sample device payload:

```json
{"level": 35, "batteryLevel":  89}
```

##### Liquid Level Sensor

The profile is configured to raise alarms if the liquid level is below certain threshold.
Major alarm is raised when the level is below 30%.
Critical alarm is raised when the level is below 10%.

Liquid Level sensors are usually battery powered, the corresponding alarms are raised when the battery level is below 30(major) or 10(critical) percent.
If your sensor is not battery powered, you may simply ignore the alarm rule.

Sample device payload:

```json
{"level": 85, "batteryLevel":  99}
```

##### Occupancy sensor

The profile is configured to raise major alarm if the room is occupied for more than 30 minutes or critical alarm if the room is occupied for more then 1 hour.

Since occupancy sensors may be battery powered, the corresponding alarms are raised when the battery level is below 30(major) or 10(critical) percent.
If your sensor is not battery powered, you may simply ignore the alarm rule.

```json
{"occupied": true, "batteryLevel":  99}
```

### Devices

We have already created 20+ devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}

Solution expects that the device telemetry will correspond to the samples provided in device profile section of the instruction.
The most simple example of the freezer payload is in JSON format:

```json
{"temperature": -5.4}{:copy-code}
```

To emulate the data upload on behalf of device "Freezer 1" located inside supermarket "Supermarket S1", one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" ${BASE_URL}/api/v1/${Freezer 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a> for simplicity of demonstration.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Based on our experience, the devices inside supermarkets are usually connected using either some form of the IoT Gateway.
If this is your case, you may explore <a href="https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/" target="_blank">ThingsBoard IoT Gateway</a> to use existing open-source project or
develop your own gateway using <a href="https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/" target="_blank">ThingsBoard MQTT Gateway API</a>.
You may also integrate existing gateways or LoRaWAN, Sigfox, and NB IoT devices.
Please check <a href="https://thingsboard.io/docs/user-guide/integrations/" target="_blank">ThingsBoard Integrations</a> for more info.

      
### Implementation details

#### Dashboards

Experienced users may notice that the solution dashboards use several new features. Those features were introduced in version 3.3.3.
The most important feature is the ability to embed multiple dashboard states into one widget.
This is done via new widget "Dashboard State" or by using 'tb-dashboard-state' component in the Markdown card widgets.

In fact, the dashboard has 30+ states that are used as a building blocks for more advanced states.
For advanced usage example, you may review the "device_card_details" state which is using
markdown value function to dynamically load states based on the device type.

#### Device Profiles

The thresholds defined by supermarket administrator are stored as the server-side attributes. 
For example, smart shelf devices use 'lowWeightMajorThreshold' and 'lowWeightCriticalThreshold'.
This attributes are referenced in the alarm rules for corresponding device profiles as the source attribute of the dynamic threshold.

### Solution entities

As part of this solution, the following entities were created:

${all_entities}

### [Optional] Edge computing

Optionally, this solution can be deployed to the edge.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

In the scope of this solution, new edge entity <a href="${Remote Supermarket R1EDGE_DETAILS_URL}" target="_blank">Remote Supermarket R1</a> was added to a customer "Retail Company B".

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Smart Retail Users"** *USER* group of customer "Retail Company B";
* **"Supermarkets"** *ASSET* group of customer "Retail Company B";
* **"Supermarket Devices"** *DEVICE* group of customer "Retail Company B";
* **"Supermarket Users Shared"** *DASHBOARD* group of your tenant.

ThingsBoard Edge is a separate service that must be installed, configured and connected to the cloud.
The easiest way to install ThingsBoard Edge is to use <a href="https://docs.docker.com/compose/install/" target="_blank">Docker Compose</a>.

Docker compose installation instructions are available on the edge details page.
Please navigate to <a href="${Remote Supermarket R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button to see the instructions.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant or users of customer "Retail Company B" credentials.

#### Push data to device on edge

**"Supermarket Devices"** *DEVICE* group of customer "Retail Company B" was assigned to the edge entity "Remote Supermarket R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Device groups** page of customer "Retail Company B".

To emulate the data upload on behalf of device "Freezer 67478" (located inside supermarket "Supermarket S3") to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" http://localhost:8080/api/v1/${Freezer 67478ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" http://localhost:18080/api/v1/${Freezer 67478ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "Freezer 67478" on edge, you'll be able to see telemetry update on the cloud for this device as well.