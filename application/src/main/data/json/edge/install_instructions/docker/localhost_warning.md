###### WARNING NOTE: 'localhost' can not be used as CLOUD_RPC_HOST

Please note that your ThingsBoard base URL is **'localhost'** at the moment. **'localhost'** cannot be used for docker containers - please update **CLOUD_RPC_HOST** environment variable below to the IP address of your machine (*docker **host** machine*). IP address must be **192.168.1.XX** or similar format. In other case - ThingsBoard Edge service, that is running in docker container, will not be able to connect to the cloud.

Additionally, please make sure that edge **Cloud Endpoint** is **NOT** set to **'localhost'** in the **ThingsBoard Edge** configuration page. If it is - please update it to the IP address of your machine. 