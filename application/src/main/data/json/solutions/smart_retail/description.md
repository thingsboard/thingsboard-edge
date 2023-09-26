### Solution description

With a Smart Retail template you get the tool for effective smart supermarket solutions.
You may easily adopt the template to your needs and monitor other types of retail facilities.  
We share this and other templates with you to make the on-boarding faster and simpler.

#### Benefits

No need to develop specific rule chains, set up alarm rules, and spend time with multi-layer interactive dashboard development.

One-click install and uninstall of the template.

#### What’s available?

As a Tenant Administrator, you get a "Smart Supermarket Administration" dashboard to provision multiple customers.
We assume you have manufactured or acquired different types of devices listed below. 
You will be able to connect real devices to the platform and assign them to your Customers.
Either you our your customers will upload the floor plan of the supermarket and position sensors on the plan.
Customer users are also able to configure the thresholds to raise the alarms.

We have configured the platform to raise alarms on data that arrives from various devices using device profiles:

* "Smart Shelf" devices generate alarm when shelf weight is below a defined threshold;
* "Chiller" and "Freezer" devices generate alarm when temperature is above the defined threshold;
* "Smart bin" sensor generates alarm when the bin level is above the defined threshold;
* "Door" and "Motion" sensors raise alerts if someone is in the store after working hours;
* "Smoke Sensor" devices react on smoke and fire;
* "Liquid Level Sensor" devices raise alarms on low liquid levels in sanitizers and soap dispensers;
* "Occupancy Sensor" devices raise alarms on prolonged occupancy of critical rooms;

#### How to use?

Use this template with zero changes for PoCs and small projects with straightforward requirements or embed it to more complex Smart Retail solutions.

#### How to connect real devices?

The most popular approach is to use [IoT gateway](https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/) per supermarket. 
Chillers, freezers and HVAC may be connected using [Modbus](https://thingsboard.io/docs/iot-gateway/config/modbus/), 
[BACnet](https://thingsboard.io/docs/iot-gateway/config/bacnet/) and other supported protocols. 
Alternatively, use your own IoT Gateway and connect it to ThingsBoard using the [MQTT Gateway API](https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/).

You may also connect existing LoRaWAN, NB IoT, SigFox and other sensors through [integrations](https://thingsboard.io/docs/user-guide/integrations/).

