## Command to cleanup test log file
sed -E 's|^[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3} \[docker-java-stream-[^]]*\] INFO org\.testcontainers\.containers\.DockerComposeContainer -- ||g; s|/[A-Za-z0-9]{12}_||g; s| STDOUT: | |g' /tmp/edge-test.log > /tmp/edge-test-clean.log

## Edge Black box tests execution
To run the black box tests with using Docker, the local Docker images of Thingsboard's microservices should be built. <br />
- Build the local Docker images in the directory with the Thingsboard's main [pom.xml](./../../pom.xml):
        
        mvn clean install -Ddockerfile.skip=false
- Verify that the new local images were built: 

        docker image ls

Also, for start test for MQTT integration, you need to have an **_eclipse-mosquitto_** image. For get this image, you need use:
        
        docker pull eclipse-mosquitto

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

- Run the black box tests in the [msa/edge-black-box-tests](../edge-black-box-tests) directory:

        mvn clean install -DedgeBlackBoxTests.skip=false


