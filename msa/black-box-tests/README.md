
## Black box tests execution
To run the black box tests with using Docker, the local Docker images of Thingsboard's microservices should be built. <br />
- Build the local Docker images in the directory with the Thingsboard's main [pom.xml](./../../pom.xml):
        
        mvn clean install -Ddockerfile.skip=false
- Verify that the new local images were built: 

        docker image ls

Also, for start test for MQTT integration, you need to have an **_eclipse-mosquitto_** image. For get this image, you need use:
        
        docker pull eclipse-mosquitto

Also, for start test for OPC UA integration, you need to have an **_opc-plc_** image. For get this image, you need use:

        docker pull mcr.microsoft.com/iotedge/opc-plc:latest

As result, in REPOSITORY column, next images should be present:

        thingsboard/tb-pe-coap-transport
        thingsboard/tb-pe-lwm2m-transport
        thingsboard/tb-pe-http-transport
        thingsboard/tb-pe-mqtt-transport
        thingsboard/tb-pe-snmp-transport
        thingsboard/tb-pe-node
        thingsboard/tb-pe-web-ui
        thingsboard/tb-pe-js-executor
        thingsboard/tb-pe-web-report
        thingsboard/tb-pe-http-integration
        thingsboard/tb-pe-mqtt-integration

- Run the black box tests (without ui tests) in the [msa/black-box-tests](../black-box-tests) directory with Redis standalone:

        mvn clean install -DblackBoxTests.skip=false

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with Redis cluster:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisCluster=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory in Hybrid mode (postgres +
  cassandra):

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.hybridMode=true

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:
- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with integrations:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.integrations.skip=false

  Note: for AWS IOT integration add next VM options:

  -DblackBoxTests.aws.endpoint=YOUR_AWAZON_CLIENT_ENDPOINT
  -DblackBoxTests.aws.rootCA=PATH_TO_ROOT_CA_PEM
  -DblackBoxTests.aws.cert=PATH_TO_CERT
  -DblackBoxTests.aws.privateKey=PATH_TO_PRIVATE_KEY
  
  Note: for Azure IoT Hub integration add next VM options:
        
  -DblackBoxTests.azureIotHubHostName=YOUR_HOST_NAME
  -DblackBoxTests.azureIotHubDeviceId=YOUR_DEVICE_ID
  -DblackBoxTests.azureIotHubSasKey=YOUR_SAS_KEY
  -DblackBoxTests.azureIotHubConnectionString=YOUR_CONNECTION_STRING
  [connection string](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-java-java-c2d#get-the-iot-hub-connection-string)
 
 Note: for Azure Event Hub integration add next VM options:

  -DblackBoxTests.azureEventHubConnectionString=YOUR_CONNECTION_STRING

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:

        mvn clean install -DblackBoxTests.skip=false -DrunLocal=true

- To run only ui tests in the [msa/black-box-tests](../black-box-tests) directory: 

        mvn clean install -DblackBoxTests.skip=false -Dsuite=uiTests

- To run all tests (black-box and ui) in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=all 


