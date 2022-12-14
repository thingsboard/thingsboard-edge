///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { IntegrationTypeSelectComponent } from '@home/components/integration/integration-type-select.component';
import {
  IntegrationConfigurationComponent
} from '@home/components/integration/configuration/integration-configuration.component';
import {
  MqttIntegrationFormComponent
} from '@home/components/integration/configuration/mqtt-integration-form/mqtt-integration-form.component';
import {
  MqttTopicFiltersComponent
} from '@home/components/integration/mqtt-topic-filters/mqtt-topic-filters.component';
import { CertUploadComponent } from '@home/components/integration/cert-upload/cert-upload.component';
import {
  IntegrationCredentialsComponent
} from '@home/components/integration/integration-credentials/integration-credentials.component';
import {
  HttpIntegrationFormComponent
} from '@home/components/integration/configuration/http-integration-form/http-integration-form.component';
import {
  SigfoxIntegrationFormComponent
} from '@home/components/integration/configuration/http-integration-form/sigfox-integration-form.component';
import {
  TtnIntegrationFormComponent
} from '@home/components/integration/configuration/ttn-tti-integration-form/ttn-integration-form.component';
import {
  TtiIntegrationFormComponent
} from '@home/components/integration/configuration/ttn-tti-integration-form/tti-integration-form.component';
import {
  AwsIotIntegrationFormComponent
} from '@home/components/integration/configuration/aws-iot-integration-form/aws-iot-integration-form.component';
import {
  OceanConnectIntegrationFormComponent
} from '@home/components/integration/configuration/http-integration-form/ocean-connect-integration-form.component';
import {
  TMobileIotIntegrationFormComponent
} from '@home/components/integration/configuration/http-integration-form/t-mobile-iot-integration-form.component';
import {
  OpcUaIntegrationFormComponent
} from '@home/components/integration/configuration/opc-ua-integration-form/opc-ua-integration-form.component';
import {
  OpcUaMappingComponent
} from '@home/components/integration/configuration/opc-ua-integration-form/opc-ua-mapping.component';
import {
  OpcUaSubscriptionComponent
} from '@home/components/integration/configuration/opc-ua-integration-form/opc-ua-subscription.component';
import {
  LoriotIntegrationFormComponent
} from '@home/components/integration/configuration/loriot-integration-form/loriot-integration-form.component';
import { ChirpStackIntegrationFormComponent } from '@home/components/integration/configuration/chirp-stack-integration-form/chirp-stack-integration-form.component';
import {
  PubSubIntegrationFormComponent
} from '@home/components/integration/configuration/pubsub-integration-form/pubsub-integration-form.component';
import {
  CustomIntegrationFormComponent
} from '@home/components/integration/configuration/custom-integration-form/custom-integration-form.component';
import {
  ApachePulsarIntegrationFormComponent
} from '@home/components/integration/configuration/apache-pulsar-integration-form/apache-pulsar-integration-form.component';
import {
  AwsKinesisIntegrationFormComponent
} from '@home/components/integration/configuration/aws-kinesis-integration-form/aws-kinesis-integration-form.component';
import { IbmWatsonIotIntegrationFormComponent } from '@home/components/integration/configuration/ibm-watson-iot-integration-form/ibm-watson-iot-integration-form.component';
import { AwsSqsIntegrationFormComponent } from '@home/components/integration/configuration/aws-sqs-integration-form/aws-sqs-integration-form.component';
import {
  UdpIntegrationFormComponent
} from '@home/components/integration/configuration/udp-integration-form/udp-integration-form.component';
import { AzureEventHubIntegrationFormComponent } from '@home/components/integration/configuration/azure-event-hub-integration-form/azure-event-hub-integration-form.component';
import {
  TcpIntegrationFormComponent
} from '@home/components/integration/configuration/tcp-integration-form/tcp-integration-form.component';
import { CoapIntegrationFormComponent } from '@home/components/integration/configuration/coap-integration-form/coap-integration-form.component';
import {
  ThingParkIntegrationFormComponent
} from '@home/components/integration/configuration/thing-park-integration-form/thing-park-integration-form.component';
import {
  ThingParkEnterpriseIntegrationFormComponent
} from '@home/components/integration/configuration/thing-park-integration-form/thing-park-enterprise-integration-form.component';
import { KafkaIntegrationFormComponent } from '@home/components/integration/configuration/kafka-integration-form/kafka-integration-form.component';
import { RabbitMqIntegrationFormComponent } from '@home/components/integration/configuration/rabbit-mq-integration-form/rabbit-mq-integration-form.component';
import {
  AzureIotHubIntegrationFormComponent
} from '@home/components/integration/configuration/azure-iot-hub-integration-form/azure-iot-hub-integration-form.component';

@NgModule({
  declarations: [
    IntegrationTypeSelectComponent,
    IntegrationConfigurationComponent,
    IntegrationCredentialsComponent,
    MqttTopicFiltersComponent,
    CertUploadComponent,
    MqttIntegrationFormComponent,
    HttpIntegrationFormComponent,
    SigfoxIntegrationFormComponent,
    TtnIntegrationFormComponent,
    TtiIntegrationFormComponent,
    AwsIotIntegrationFormComponent,
    OceanConnectIntegrationFormComponent,
    TMobileIotIntegrationFormComponent,
    OpcUaIntegrationFormComponent,
    OpcUaMappingComponent,
    OpcUaSubscriptionComponent,
    LoriotIntegrationFormComponent,
    ChirpStackIntegrationFormComponent,
    PubSubIntegrationFormComponent,
    ApachePulsarIntegrationFormComponent,
    CustomIntegrationFormComponent,
    AwsKinesisIntegrationFormComponent,
    IbmWatsonIotIntegrationFormComponent,
    AwsSqsIntegrationFormComponent,
    UdpIntegrationFormComponent,
    AzureEventHubIntegrationFormComponent,
    TcpIntegrationFormComponent,
    CoapIntegrationFormComponent,
    ThingParkIntegrationFormComponent,
    ThingParkEnterpriseIntegrationFormComponent,
    KafkaIntegrationFormComponent,
    RabbitMqIntegrationFormComponent,
    AzureIotHubIntegrationFormComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    IntegrationTypeSelectComponent,
    IntegrationConfigurationComponent
  ]
})
export class IntegrationComponentModule { }
