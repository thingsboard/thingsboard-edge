The Smart Irrigation template represents a generic field irrigation solution. 
You may provision fields by selecting the crop type and moisture thresholds.
You may also define the field location using a rectangle or a complex polygon on the map.

Each Field may contain multiple moisture sensors. 
The soil moisture thresholds are applied to each sensor reading. 
Use receives an alarm when the threshold is violated or the sensor battery is low.
The sensor alarms are propagated to the field level.

The sensor readings are also aggregated to the average soil moisture of the field. 
Users may observe the history of the aggregated value or each particular sensor reading separately.

Users may configure the irrigation schedule and define desired water consumption or irrigation duration for each scheduled task.
The history of the irrigation tasks is available in the field details. 

Press the "install" button, and you will have a ready-to-use generic application that covers all common requirements.

#### Solution structure

* "Irrigation Management" dashboard for tenant administrators to provision the fields, sensors, and irrigation scheduling;
* Multiple device and asset profiles with pre-configured alarm rules: "SI Water Meter", "SI Smart Valve", "SI Soil Moisture Sensor", "SI Field";
* Rule Chains to control the irrigation logic and process data from devices and assets;
* Each device type generates a specific alarm based on configurable thresholds.