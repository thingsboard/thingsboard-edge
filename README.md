# ThingsBoard 

The **ThingsBoard Edge** is an open-source ThingsBoard's software product for edge computing.

It allows bringing data analysis and management to the edge, where the data created. At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud ([ThingsBoard Demo](https://demo.thingsboard.io/) or [ThingsBoard CE](https://github.com/thingsboard/thingsboard)) according to your business needs.

## Documentation

ThingsBoard Edge documentation is hosted on [thingsboard.io](https://thingsboard.io/docs/edge/).

## ThingsBoard Edge use-cases

- **Autonomous Vehicles**
Edge computing makes it possible to collect, process and react to road events with almost no latency. Modern autonomous vehicles produces tons of data - between 5 TB and 20 TB a day. 4G or 5G will not able to provide that network throughput, but with ThingsBoard Edge you are able to filter data. Most of this data should be processed locally, and only subset of this data will be pushed to the cloud.

- **Smart Farming**
Quickly react to failures of silo aeration systems on a remote site even if connectivity to the cloud from on-field location is pure at the moment.

- **Smart Houses**
Bringing the processing and analyzing data closer to the smart house provides the possibility to secure sensitive user information at the edge. Additionally, it provides a good user experience because of the low latency of smart house solutions - user will get responses from end devices much faster, comparing to connecting edge devices to the cloud to make some decisions. 

- **Security Solutions**
It's necessary to react to security violations and threats within seconds and edge provides this possibility. You don't need to care about quality of your connectivity to cloud - decision will be made by local edge engine on a remote site in real-time. 

- **In-hospital Monitoring**
To secure data privacy in healthcare devices processing of this data must be done on the edge. Push to the cloud only required pieces of readings from medical devices, while storing all other sensitive data on the edge. 
Additional benefit from edge processing in this use-case - react to critical medical cases as quickly as possible due to real time processing of data from edge medical devices.

- **Predictive Maintenance**
Brings processing and storage of edge device readings closer to the equipment. Analyze tons of data locally and detect changes in the production lines before a failure occurs. Send to the cloud only average readings from productions lines according to your business needs.

## ThingsBoard Edge features

With **ThingsBoard Edge** you get the following benefits:

 - **Local deployment and storage** to process and store data from edge (local) devices without connection to the cloud. Push updates to the cloud once connection restored.
 
 ![image](https://thingsboard.io/images/edge/overview/offline_network_.svg)

 - **Traffic filtering** to filter data from edge (local) devices on the ThingsBoard Edge service and push to cloud only subset of the data for further processing or storage.
 
  ![image](https://thingsboard.io/images/edge/overview/data_filtering.svg)
 
 - **Local alarms** to react instantly to critical situations on site without connectivity to cloud.
 
  ![image](https://thingsboard.io/images/edge/overview/alarm.svg)

 - Monitor local events and timeseries data with a **real-time dashboards**.
 - **Batch Update** thousands of edge configurations in a single click.
 
 ![image](https://thingsboard.io/images/edge/overview/update_dashboard.svg)
 
 - **Local storage** to store data from the edge devices on the edge if there is no active connection to cloud and push to the cloud updates once connection restored.

ThingsBoard Edge inherits features from ThingsBoard CE to provide you the same experience how to connect, manage and process data from your devices.  

It supports next **ThingsBoard Community Edition** features:
 * [**Attributes**](https://thingsboard.io/docs/user-guide/attributes/) - assign and manage custom attributes to your entities.
 * [**Telemetry**](https://thingsboard.io/docs/user-guide/telemetry/) - API for collection of time-series data of your devices.
 * [**Entities and relations**](https://thingsboard.io/docs/user-guide/entities-and-relations/) - model physical world objects (e.g. devices and assets) and relations between them.
 * [**Data visualization**](https://thingsboard.io/docs/guides#AnchorIDDataVisualization) - develop custom dashboards and widgets.
 * [**Rule engine**](https://thingsboard.io/docs/user-guide/rule-engine-2-0/re-getting-started/) - manage data processing & actions on incoming telemetry and events.
 * [**RPC**](https://thingsboard.io/docs/user-guide/rpc/) - send remote procedure calls (RPC) **both from edge and cloud sides** to devices and vice versa.
 * [**Audit log**](https://thingsboard.io/docs/user-guide/audit-log/) - track user activity.
 * [**API Limits**](https://thingsboard.io/docs/user-guide/api-limits/) - control and limit number of API requests from a single host.

## Getting Started

Collect and Visualize your IoT Edge data in minutes by following this [guide](https://thingsboard.io/docs/edge/getting-started/).

## Licenses

This project is released under [Apache 2.0 License](./LICENSE).
