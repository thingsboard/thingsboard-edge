import { CertUploadComponent } from './cert-upload/cert-upload.component';
import { HttpIntegrationFormComponent } from './http-integration-form/http-integration-form.component';
import { MqttIntegrationFormComponent } from './mqtt-integration-form/mqtt-integration-form.component';
import { OpcUaIntegrationFormComponent } from './opc-ua-integration-form/opc-ua-integration-form.component';
import { AwsKinesisIntegrationFormComponent } from './aws-kinesis-integration-form/aws-kinesis-integration-form.component';
import { AwsIotIntegrationFormComponent } from './aws-iot-integration-form/aws-iot-integration-form.component';
import { AwsSqsIntegrationFormComponent } from './aws-sqs-integration-form/aws-sqs-integration-form.component';
import { AzureEventHubIntegrationFormComponent } from './azure-event-hub-integration-form/azure-event-hub-integration-form.component';
import { IbmWatsonIotIntegrationFormComponent } from './ibm-watson-iot-integration-form/ibm-watson-iot-integration-form.component';
import { KafkaIntegrationFormComponent } from './kafka-integration-form/kafka-integration-form.component';
import { TcpIntegrationFormComponent } from './tcp-integration-form/tcp-integration-form.component';
import { TtnIntegrationFormComponent } from './ttn-integration-form/ttn-integration-form.component';
import { UdpIntegrationFormComponent } from './udp-integration-form/udp-integration-form.component';
import { MqttTopicFiltersComponent } from './mqtt-topic-filters/mqtt-topic-filters.component';
import { OpcUaSubscriptionTagsComponent } from './opc-ua-subscription-tags/opc-ua-subscription-tags.component';
import { CustomIntegrationFormComponent } from './custom-integration-form/custom-integration-form.component';

export const integrations = [
    CertUploadComponent,
    CustomIntegrationFormComponent,
    OpcUaSubscriptionTagsComponent,
    MqttTopicFiltersComponent,
    HttpIntegrationFormComponent,
    MqttIntegrationFormComponent,
    OpcUaIntegrationFormComponent,
    AwsKinesisIntegrationFormComponent,
    AwsIotIntegrationFormComponent,
    AwsSqsIntegrationFormComponent,
    AzureEventHubIntegrationFormComponent,
    IbmWatsonIotIntegrationFormComponent,
    KafkaIntegrationFormComponent,
    TcpIntegrationFormComponent,
    TtnIntegrationFormComponent,
    UdpIntegrationFormComponent,
    OpcUaSubscriptionTagsComponent
];