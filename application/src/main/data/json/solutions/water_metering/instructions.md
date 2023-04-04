## Solution instructions

As part of this solution, we have created the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Water Metering Tenant Dashboard"</a> that allows you to manage
water metering devices, users and alarms:

* observe location and status of the water meters on the map. Markers are clustered to be able to show thousands of meters simultaneously;
* use "Analytics" view to compare consumption for the current and previous month; 
* use "Devices" view to get the list of all water meter devices with ability to 
    * create a new device and assign it to the customer;
    * change the location of the device;
    * configure alarm thresholds for this device;
    * navigate to "Device" view by clicking on the device row;
* use "Device" view to:
    * browse water consumption history for a particular water meter device;
    * browse active alarms for a particular water meter device;
    * change water meter location information
    * upload water meter photo;
    * change location of the device;
* use "Customers" view to manage your customers;   
* use "Alarms" view to browse and clear alarms from water meters;
* use "Settings" view to:
    * turn system alarms on and off;
    * define thresholds for system alarms;
    * turn sms and email notifications on and off;


You may always customize the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Water Metering Tenant Dashboard"</a> using dashboard development <a href="https://thingsboard.io/docs/user-guide/dashboards/" target="_blank">guide</a>.

We have also created the "Water Metering User Dashboard" for the end users. This dashboard is assigned to the new customers automatically. The end user dashboard allows customers to:

* observe location and status of the water meters on the map. Markers are clustered to be able to show thousands of meters simultaneously;
* browse active alarms and water consumption per day and week;
* use "Analytics", "Devices", "Alarms" views that are similar to the main dashboard;
* use "Settings" view to define alarm thresholds for the particular customer. Generated alarms will not be visible to Tenant Administrator by default;


### Devices

We have already created three water metering devices and loaded some demo data for them. See device info and credentials below:

${device_list_and_credentials}

Solution expects that the water meter device will report "pulseCounter", "temperature", "battery" and "leakage" values.
The most simple example of the expected payload is in JSON format:

```json
{"pulseCounter":  550, "temperature":  22.0, "battery":  97, "leakage":  false}{:copy-code}
```

To emulate the data upload on behalf of device "WM0000123", one should execute the following command:

```bash
curl -v -X POST -d "{\"pulseCounter\":  550, \"temperature\":  22.0, \"battery\":  97, \"leakage\":  false}" ${BASE_URL}/api/v1/${WM0000123ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="https://thingsboard.io/docs/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Most of the water meters are using LoRaWAN, Sigfox or NB IoT technology. Please check <a href="https://thingsboard.io/docs/user-guide/integrations/" target="_blank">ThingsBoard Integrations</a> for more info.

### Alarms

Alarms are generated using three <a href="https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules" target="_blank">Alarm rules</a> in the
"Water Meter" <a href="/profiles/deviceProfiles" target="_blank">device profile</a>. 
Alarms notifications are sent via SMS or email to Tenant Administrators and Customer Users depending on the thresholds and settings defined in the dashboard.

### Rule Chains

The "Water Metering Solution Main" rule chain is processing all incoming messages from water metering devices. 
This rule chain is responsible for aggregation of the incoming data on a daily and weekly basis for device, customer and tenant level. 
Aggregated data is stored as telemetry as well. The aggregation is done in the UTC time zone by default. 
You may change the time zone in the "aggregate stream" rule nodes. You may also aggregate data in different time zones.

There are two other rule chains: "Water Metering Solution Tenant Alarm Routing" and "Water Metering Solution Customer Alarm Routing". 
They are responsible for routing incoming messages to tenant administrators and customer users respectively.  

### Customers

Meters "WM0000123" and "WM0000124" are assigned to a newly created customer "Water Metering Customer A".
You may notice that "Water Metering Customer A" has a user, and the "Water Metering User Dashboard" dashboard is assigned to the user by default.
You may create more Customers and more Users via <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Water Metering Tenant Dashboard"</a>.

**User list**

${user_list}


### Solution entities

As part of this solution, the following entities were created:

${all_entities}

### Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of Water metering solution, edge computing could be useful if you have remote facilities that are located in different parts of town, country or worldwide.
In this case, ThingsBoard Edge can be deployed into every remote facility to process data from water meters, calculating daily consumption, enabling real-time analysis and decision-making, such as alarm generation in case no data from sensors after X hours. 
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally. 
Eventually, required data is going to be pushed to the cloud, once network connection is established. 
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Facility R1EDGE_DETAILS_URL}" target="_blank">Remote Facility R1</a> was added to a customer "Water Metering Customer A".

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Customer Administrators"** *USER* group of customer "Water Metering Customer A";
* **"Water Meters"** *DEVICE* group of customer "Water Metering Customer A";
* **"Water Metering"** *DASHBOARD* group of your tenant.
* **"Water Metering Shared"** *DASHBOARD* group of your tenant.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Facility R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant or users of customer "Water Metering Customer A" credentials.

#### Push data to device on edge

**"Water Meters"** *DEVICE* group of customer "Water Metering Customer A" was assigned to the edge entity "Remote Facility R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of device "WM0000123" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"pulseCounter\":  550, \"temperature\":  22.0, \"battery\":  97, \"leakage\":  false}" http://localhost:8080/api/v1/${WM0000123ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"pulseCounter\":  550, \"temperature\":  22.0, \"battery\":  97, \"leakage\":  false}" http://localhost:18080/api/v1/${WM0000123ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "WM0000123" on edge, you'll be able to see telemetry update on the cloud for this device as well.
