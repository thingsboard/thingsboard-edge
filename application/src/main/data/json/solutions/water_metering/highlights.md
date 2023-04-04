Water Metering template represent generic water metering solution. 
With this template you get interactive dashboards that allow administrator and end user 
browse state of the water meters and aggregated water consumption statistics. 
Users are able to define thresholds and enable alarms and notifications over SMS or email.  


Press the install button and you will have a ready-to-use generic application that covers all common requirements.

#### Solution structure

* "Water Metering Tenant Dashboard" dashboard for tenant administrators;
* "Water Metering User Dashboard" dashboard for end customers;
* Three "Water Meter" devices with battery level, water temperature and consumption history;
* "Water Meter" device profile that generates "Low battery", "Low temperature", "Leakage Detected" and "Daily/Weekly Consumption" alarms;
* Rule chains:
   * "Water Metering Solution Main" contains data aggregation rule nodes. 
   * "Water Metering Solution Tenant Alarm Routing" responsible for sending alarm notifications over SMS or email if corresponding administrator settings are enabled.
   * "Water Metering Solution User Alarm Routing" responsible for sending alarm notifications over SMS or email if corresponding user settings are enabled.
* [Optional] Edge instance - add edge computing to your solution.