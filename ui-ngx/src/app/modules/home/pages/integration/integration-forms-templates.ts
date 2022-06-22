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

import { CoapSecurityMode, IntegrationType, IntegrationTypeInfo } from '@shared/models/integration.models';
import { baseUrl, coapBaseUrl, generateId } from '@app/core/utils';
import { AbstractControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';

export const handlerConfigurationTypes = {
  text: {
    value: 'TEXT',
    name: 'extension.text'
  },
  binary: {
    value: 'BINARY',
    name: 'extension.binary'
  },
  json: {
    value: 'JSON',
    name: 'extension.json'
  },
  hex: {
    value: 'HEX',
    name: 'extension.hex'
  }
};

export const tcpBinaryByteOrder = {
  littleEndian: {
    value: 'LITTLE_ENDIAN'
  },
  bigEndian: {
    value: 'BIG_ENDIAN'
  }
};

export const tcpTextMessageSeparator = {
  systemLineSeparator: {
    value: 'SYSTEM_LINE_SEPARATOR'
  },
  nulDelimiter: {
    value: 'NUL_DELIMITER'
  }
};

export const opcSecurityTypes = {
  Basic128Rsa15: 'Basic128Rsa15',
  Basic256: 'Basic256',
  Basic256Sha256: 'Basic256Sha256',
  None: 'None'
};

export type loriotCredentialType = 'basic' | 'token';

export const loriotCredentialTypes = {
  basic: {
    value: 'basic',
    name: 'extension.basic'
  },
  token: {
    value: 'token',
    name: 'extension.token'
  }
};

export type mqttCredentialType = 'anonymous' | 'basic' | 'cert.PEM';

export const mqttCredentialTypes = {
  anonymous: {
    value: 'anonymous',
    name: 'extension.anonymous'
  },
  basic: {
    value: 'basic',
    name: 'extension.basic'
  },
  'cert.PEM': {
    value: 'cert.PEM',
    name: 'extension.pem'
  }
};

export type azureIotHubCredentialsType = 'sas' | 'cert.PEM';
export const azureIotHubCredentialsTypes = {
  sas: {
    value: 'sas',
    name: 'extension.sas'
  },
  'cert.PEM': {
    value: 'cert.PEM',
    name: 'extension.pem'
  }
};

export type apachePulsarCredentialsType = 'anonymous' | 'token';
export const apachePulsarCredentialsTypes = {
  anonymous: {
    value: 'anonymous',
    name: 'extension.anonymous'
  },
  token: {
    value: 'token',
    name: 'extension.token'
  }
};

export function updateIntegrationFormState(type: IntegrationType, info: IntegrationTypeInfo,
                                           integrationForm: FormGroup, disabled: boolean) {
  if (disabled) {
    integrationForm.disable({emitEvent: false});
  } else {
    integrationForm.enable({emitEvent: false});
    if (info.http) {
      integrationForm.get('httpEndpoint').disable({emitEvent: false});
    } else if (type === IntegrationType.TTN) {
      integrationForm.get('topicFilters').disable({emitEvent: false});
    } else if (type === IntegrationType.CHIRPSTACK) {
      integrationForm.get('clientConfiguration.httpEndpoint').disable({emitEvent: false});
    }
  }
}

export function updateIntegrationFormDefaultFields(type: IntegrationType, integrationForm: FormGroup) {
  if (type === IntegrationType.KAFKA) {
    if (!integrationForm.get('clientConfiguration').get('groupId').value) {
      integrationForm.get('clientConfiguration').get('groupId').patchValue('group_id_' + generateId(10));
    }
    if (!integrationForm.get('clientConfiguration').get('clientId').value) {
      integrationForm.get('clientConfiguration').get('clientId').patchValue('client_id_' + generateId(10));
    }
  } else if (type === IntegrationType.CUSTOM) {
    if (!integrationForm.get('configuration').value) {
      integrationForm.get('configuration').patchValue('{}');
    }
  }
}

export function updateIntegrationFormValidators(integrationForm: FormGroup,
                                                fieldValidators: {[key: string]: ValidatorFn | ValidatorFn[]} = {},
                                                type: IntegrationType,
                                                integrationScope: string) {
  for (const field of Object.keys(fieldValidators)) {
    const validators = filterEdgeIntegrationTemplateValidators(fieldValidators[field], type, integrationScope);
    const path = field.split('.');
    let control: AbstractControl = integrationForm;
    for (const part of path) {
      control = control.get(part);
    }
    control.setValidators(validators);
    control.updateValueAndValidity();
  }
}

export function filterEdgeIntegrationTemplateValidators(fieldValidators: ValidatorFn | ValidatorFn[],
                                                        type: IntegrationType,
                                                        integrationScope: string): ValidatorFn | ValidatorFn[] {
  if (integrationScope == 'edge' || integrationScope == 'edges') {
    if (!edgeTemplateIntegrationIgnoredValidators[type]) {
      return fieldValidators;
    }
    let filteredFieldValidators: ValidatorFn[] = [];
    if (Array.isArray(fieldValidators)) {
      for (const validator of fieldValidators) {
        if (!edgeTemplateIntegrationIgnoredValidators[type].includes(validator)) {
          filteredFieldValidators.push(validator)
        }
      }
    } else {
      if (!edgeTemplateIntegrationIgnoredValidators[type].includes(fieldValidators)) {
        filteredFieldValidators.push(fieldValidators)
      }
    }
    return filteredFieldValidators;
  } else {
    return fieldValidators;
  }
}

export const ibmWatsonIotApiKeyPatternValidator = Validators.pattern(/^a-\w+-\w+$/)
export const mqttClientIdPatternValidator = Validators.pattern('[a-zA-Z0-9]*')
export const mqttClientIdMaxLengthValidator = Validators.maxLength(23)

export const edgeTemplateIntegrationIgnoredValidators = {
  [IntegrationType.IBM_WATSON_IOT]:
    [
    ibmWatsonIotApiKeyPatternValidator
    ],
  [IntegrationType.MQTT]:
    [
    mqttClientIdPatternValidator,
    mqttClientIdMaxLengthValidator
    ]
}

export const templates = {
  http: {
    baseUrl: baseUrl(),
    replaceNoContentToOk: '',
    enableSecurity: false,
    downlinkUrl: 'https://api.thingpark.com/thingpark/lrc/rest/downlink',
    loriotDownlinkUrl: 'https://eu1.loriot.io/1/rest',
    createLoriotOutput: false,
    sendDownlink: false,
    server: 'eu1',
    domain: 'loriot.io',
    appId: '',
    enableSecurityNew: false,
    asId: '',
    asIdNew: '',
    asKey: '',
    clientIdNew: '',
    clientSecret: '',
    maxTimeDiffInSeconds: 60,
    httpEndpoint: '',
    headersFilter: {},
    ignoreNonPrimitiveFields: ['headersFilter'],
    token: '',
    credentials: {
      type: loriotCredentialTypes.basic.value,
      email: '',
      password: '',
      token: ''
    },
    fieldValidators: {
      baseUrl: [Validators.required],
      asId: [Validators.required],
      asIdNew: [Validators.required],
      asKey: [Validators.required],
      clientIdNew: [Validators.required],
      clientSecret: [Validators.required],
      maxTimeDiffInSeconds: [Validators.required, Validators.min(0)],
      loriotDownlinkUrl: [Validators.required],
      server: [Validators.required],
      appId: [Validators.required],
      token: [Validators.required],
      'credentials.email': [],
      'credentials.password': [],
      'credentials.token': []
    }
  },
  [IntegrationType.COAP]: {
    clientConfiguration: {
      baseUrl: coapBaseUrl(false),
      dtlsBaseUrl: coapBaseUrl(true),
      securityMode: CoapSecurityMode.NO_SECURE,
      coapEndpoint: '',
      dtlsCoapEndpoint: ''
    },
    ignoreNonPrimitiveFields: [],
    fieldValidators: {
      'clientConfiguration.baseUrl': [Validators.required],
      'clientConfiguration.dtlsBaseUrl': [Validators.required],
      'clientConfiguration.securityMode': [Validators.required]
    }
  },
  [IntegrationType.MQTT]: {
    clientConfiguration: {
      host: '',
      port: 1883,
      cleanSession: true,
      ssl: false,
      connectTimeoutSec: 10,
      clientId: '',
      maxBytesInMessage: 32368,
      credentials: {
        type: mqttCredentialTypes.anonymous.value,
        username: '',
        password: '',
        caCertFileName: '',
        caCert: '',
        certFileName: '',
        cert: '',
        privateKeyFileName: '',
        privateKey: '',
        privateKeyPassword: ''
      },
    },
    downlinkTopicPattern: '${topic}',
    topicFilters: [],
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.port': [Validators.min(1), Validators.max(65535)],
      'clientConfiguration.clientId': [mqttClientIdPatternValidator, mqttClientIdMaxLengthValidator],
      'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
      'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
      'clientConfiguration.credentials.username': [Validators.required],
      'clientConfiguration.credentials.password': [Validators.required],
      'clientConfiguration.credentials.caCertFileName': [Validators.required],
      'clientConfiguration.credentials.caCert': [Validators.required],
      'clientConfiguration.credentials.certFileName': [Validators.required],
      'clientConfiguration.credentials.cert': [Validators.required],
      'clientConfiguration.credentials.privateKeyFileName': [Validators.required],
      'clientConfiguration.credentials.privateKey': [Validators.required],
      downlinkTopicPattern: [Validators.required],
      topicFilters: [Validators.required]
    }
  },
  [IntegrationType.AZURE_IOT_HUB]: {
    clientConfiguration: {
      host: '\<name\>.azure-devices.net',
      port: 8883,
      cleanSession: true,
      ssl: true,
      maxBytesInMessage: 32368,
      connectTimeoutSec: 10,
      clientId: 'device_id',
      credentials: {
        type: azureIotHubCredentialsTypes.sas.value,
        sasKey: '',
        caCertFileName: '',
        caCert: '',
        certFileName: '',
        cert: '',
        privateKeyFileName: '',
        privateKey: '',
        privateKeyPassword: ''
      },
    },
    topicFilters: [{filter: 'devices/\<device_id\>/messages/devicebound/#', qos: 0}],
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.clientId': [Validators.required],
      'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
      'clientConfiguration.credentials.sasKey': [Validators.required],
      'clientConfiguration.credentials.certFileName': [Validators.required],
      'clientConfiguration.credentials.cert': [Validators.required],
      'clientConfiguration.credentials.privateKeyFileName': [Validators.required],
      'clientConfiguration.credentials.privateKey': [Validators.required],
      topicFilters: [Validators.required]
    }
  },
  [IntegrationType.AWS_IOT]: {
    clientConfiguration: {
      host: '',
      port: 8883,
      clientId: '',
      connectTimeoutSec: 10,
      ssl: true,
      maxBytesInMessage: 32368,
      credentials: {
        type: 'cert.PEM',
        caCertFileName: '',
        caCert: '',
        certFileName: '',
        cert: '',
        privateKeyFileName: '',
        privateKey: '',
        password: ''
      }
    },
    downlinkTopicPattern: '${topic}',
    topicFilters: [],
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.port': [Validators.min(1), Validators.max(65535)],
      'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
      'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
      'clientConfiguration.credentials.caCertFileName': [Validators.required],
      'clientConfiguration.credentials.caCert': [Validators.required],
      'clientConfiguration.credentials.certFileName': [Validators.required],
      'clientConfiguration.credentials.cert': [Validators.required],
      'clientConfiguration.credentials.privateKeyFileName': [Validators.required],
      'clientConfiguration.credentials.privateKey': [Validators.required],
      downlinkTopicPattern: [Validators.required],
      topicFilters: [Validators.required]
    }
  },
  [IntegrationType.AWS_SQS]: {
    sqsConfiguration: {
      queueUrl: '',
      pollingPeriodSeconds: 5,
      region: 'us-west-2',
      accessKeyId: '',
      secretAccessKey: ''
    },
    fieldValidators: {
      'sqsConfiguration.queueUrl': [Validators.required],
      'sqsConfiguration.pollingPeriodSeconds': [Validators.required, Validators.min(1)],
      'sqsConfiguration.region': [Validators.required],
      'sqsConfiguration.accessKeyId': [Validators.required],
      'sqsConfiguration.secretAccessKey': [Validators.required]
    }
  },
  [IntegrationType.AWS_KINESIS]: {
    clientConfiguration: {
      streamName: '',
      region: '',
      accessKeyId: '',
      secretAccessKey: '',
      useCredentialsFromInstanceMetadata: false,
      applicationName: '',
      initialPositionInStream: '',
      useConsumersWithEnhancedFanOut: false,
      maxRecords: 10000,
      requestTimeout: 30
    },
    fieldValidators: {
      'clientConfiguration.streamName': [Validators.required],
      'clientConfiguration.region': [Validators.required],
      'clientConfiguration.accessKeyId': [Validators.required],
      'clientConfiguration.secretAccessKey': [Validators.required],
      'clientConfiguration.initialPositionInStream': [Validators.required],
      'clientConfiguration.maxRecords': [Validators.required, Validators.min(1), Validators.max(10000)],
      'clientConfiguration.requestTimeout': [Validators.required]
    }
  },
  [IntegrationType.IBM_WATSON_IOT]: {
    clientConfiguration: {
      connectTimeoutSec: 10,
      host: '',
      port: 8883,
      ssl: true,
      maxBytesInMessage: 32368,
      cleanSession: true,
      credentials: {
        type: 'basic',
        username: '',
        password: '',
      }
    },
    topicFilters: [{
      filter: 'iot-2/type/+/id/+/evt/+/fmt/+',
      qos: 0
    }],
    downlinkTopicPattern: 'iot-2/type/${device_type}/id/${device_id}/cmd/${command_id}/fmt/${format}',
    fieldValidators: {
      'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
      'clientConfiguration.credentials.username': [Validators.required, ibmWatsonIotApiKeyPatternValidator],
      'clientConfiguration.credentials.password': [Validators.required],
      'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
      downlinkTopicPattern: [Validators.required],
      topicFilters: [Validators.required]
    }
  },
  [IntegrationType.CHIRPSTACK]: {
    clientConfiguration: {
      baseUrl: baseUrl(),
      applicationServerUrl: '',
      replaceNoContentToOk: true,
      applicationServerAPIToken: '',
      httpEndpoint: ''
    },
    fieldValidators: {
      'clientConfiguration.baseUrl': [Validators.required],
      'clientConfiguration.applicationServerAPIToken': [Validators.required],
    }
  },
  [IntegrationType.TTN]: {
    clientConfiguration: {
      host: '',
      customHost: false,
      port: 8883,
      ssl: true,
      maxBytesInMessage: 32368,
      connectTimeoutSec: 10,
      credentials: {
        type: 'basic',
        username: '',
        password: ''
      },
      apiVersion: false,
    },
    topicFilters: [{
      filter: '+/devices/+/up',
      qos: 0
    }],
    downlinkTopicPattern: '',
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
      'clientConfiguration.credentials.username': [Validators.required],
      'clientConfiguration.credentials.password': [Validators.required],
      'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
      downlinkTopicPattern: [Validators.required],
      topicFilters: [Validators.required]
    }
  },
  [IntegrationType.TTI]: {
    clientConfiguration: {
      host: '',
        customHost: false,
        port: 8883,
        ssl: true,
        connectTimeoutSec: 10,
        maxBytesInMessage: 32368,
        credentials: {
          type: 'basic',
          username: '',
          password: ''
      }
    },
    topicFilters: [{
      filter: 'v3/+/devices/+/up',
      qos: 0
    }],
      downlinkTopicPattern: '',
      fieldValidators: {
        'clientConfiguration.host': [Validators.required],
        'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
        'clientConfiguration.maxBytesInMessage': [Validators.min(1), Validators.max(256000000)],
        'clientConfiguration.credentials.username': [Validators.required],
        'clientConfiguration.credentials.password': [Validators.required],
        downlinkTopicPattern: [Validators.required],
        topicFilters: [Validators.required]
    }
  },
  [IntegrationType.AZURE_EVENT_HUB]: {
    clientConfiguration: {
      connectTimeoutSec: 10,
      connectionString: '',
      consumerGroup: '',
      iotHubName: ''
    },
    fieldValidators: {
      'clientConfiguration.connectTimeoutSec': [Validators.required, Validators.min(1), Validators.max(200)],
      'clientConfiguration.connectionString': [Validators.required]
    }
  },
  [IntegrationType.OPC_UA]: {
    clientConfiguration: {
      applicationName: '',
      applicationUri: '',
      host: 'localhost',
      port: 49320,
      scanPeriodInSeconds: 10,
      timeoutInMillis: 5000,
      security: opcSecurityTypes.Basic128Rsa15,
      identity: {
        password: '',
        username: '',
        type: 'anonymous'
      },
      mapping: [],
      keystore: {
        location: '',
        type: '',
        fileContent: '',
        password: 'secret',
        alias: 'opc-ua-extension',
        keyPassword: 'secret',
      }
    },
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.port': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.scanPeriodInSeconds': [Validators.required],
      'clientConfiguration.timeoutInMillis': [Validators.required],
      'clientConfiguration.security': [Validators.required],
      'clientConfiguration.identity.type': [Validators.required],
      'clientConfiguration.identity.username': [Validators.required],
      'clientConfiguration.identity.password': [Validators.required],
      'clientConfiguration.mapping': [Validators.required],
      'clientConfiguration.keystore.type': [Validators.required],
      'clientConfiguration.keystore.location': [Validators.required],
      'clientConfiguration.keystore.fileContent': [Validators.required],
      'clientConfiguration.keystore.password': [Validators.required],
      'clientConfiguration.keystore.alias': [Validators.required],
      'clientConfiguration.keystore.keyPassword': [Validators.required]
    }
  },
  [IntegrationType.UDP]: {
    clientConfiguration: {
      port: 11560,
      soBroadcast: true,
      soRcvBuf: 64,
      cacheSize: 1000,
      timeToLiveInMinutes: 1440,
      handlerConfiguration: {
        handlerType: handlerConfigurationTypes.binary.value,
        charsetName: 'UTF-8',
        maxFrameLength: 128
      }
    },
    fieldValidators: {
      'clientConfiguration.port': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.soRcvBuf': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.handlerConfiguration.handlerType': [Validators.required],
      'clientConfiguration.handlerConfiguration.charsetName': [Validators.required],
      'clientConfiguration.handlerConfiguration.maxFrameLength': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.cacheSize': [Validators.min(0)],
      'clientConfiguration.timeToLiveInMinutes': [Validators.min(0), Validators.max(525600)]
    }
  },
  [IntegrationType.TCP]: {
    clientConfiguration: {
      port: 10560,
      soBacklogOption: 128,
      soRcvBuf: 64,
      soSndBuf: 64,
      soKeepaliveOption: false,
      tcpNoDelay: true,
      cacheSize: 1000,
      timeToLiveInMinutes: 1440,
      handlerConfiguration: {
        handlerType: handlerConfigurationTypes.binary.value,
        byteOrder: tcpBinaryByteOrder.littleEndian.value,
        maxFrameLength: 128,
        lengthFieldOffset: 0,
        lengthFieldLength: 2,
        lengthAdjustment: 0,
        initialBytesToStrip: 0,
        failFast: false,
        stripDelimiter: true,
        messageSeparator: tcpTextMessageSeparator.systemLineSeparator.value
      }
    },
    fieldValidators: {
      'clientConfiguration.port': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.soBacklogOption': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.soRcvBuf': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.soSndBuf': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.handlerConfiguration.handlerType': [Validators.required],
      'clientConfiguration.handlerConfiguration.maxFrameLength': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.handlerConfiguration.lengthFieldOffset': [Validators.required, Validators.min(0), Validators.max(8)],
      'clientConfiguration.handlerConfiguration.lengthFieldLength': [Validators.required, Validators.min(0), Validators.max(8)],
      'clientConfiguration.handlerConfiguration.lengthAdjustment': [Validators.required, Validators.min(0), Validators.max(8)],
      'clientConfiguration.handlerConfiguration.initialBytesToStrip': [Validators.required, Validators.min(0), Validators.max(8)],
      'clientConfiguration.cacheSize': [Validators.min(0)],
      'clientConfiguration.timeToLiveInMinutes': [Validators.min(0), Validators.max(525600)]
    }
  },
  [IntegrationType.KAFKA]: {
    clientConfiguration: {
      groupId: '',
      clientId: '',
      topics: 'my-topic-output',
      bootstrapServers: 'localhost:9092',
      pollInterval: 5000,
      autoCreateTopics: false,
      otherProperties: null
    },
    ignoreNonPrimitiveFields: ['otherProperties'],
    fieldValidators: {
      'clientConfiguration.groupId': [Validators.required],
      'clientConfiguration.clientId': [Validators.required],
      'clientConfiguration.topics': [Validators.required],
      'clientConfiguration.bootstrapServers': [Validators.required],
      'clientConfiguration.pollInterval': [Validators.required]
    }
  },
  [IntegrationType.RABBITMQ]: {
    clientConfiguration: {
      exchangeName: '',
      host: '',
      port: 5672,
      virtualHost: '',
      username: '',
      password: '',
      downlinkTopic: '',
      queues: 'my-queue',
      routingKeys: 'my-routing-key',
      connectionTimeout: 60000,
      handshakeTimeout: 10000,
      pollPeriod: 5000,
      durable: false,
      exclusive: true,
      autoDelete: true,
    },
    fieldValidators: {
      'clientConfiguration.host': [Validators.required],
      'clientConfiguration.port': [Validators.required, Validators.min(1), Validators.max(65535)],
      'clientConfiguration.queues': [Validators.required],
      'clientConfiguration.connectionTimeout': [Validators.min(0)],
      'clientConfiguration.handshakeTimeout': [Validators.min(0)],
      'clientConfiguration.pollPeriod': [Validators.min(0)]
    }
  },

  [IntegrationType.APACHE_PULSAR]: {
    clientConfiguration: {
      serviceUrl: 'pulsar://localhost:6650',
      topics: 'my-topic',
      subscriptionName: 'my-subscription',
      maxNumMessages: 1000,
      maxNumBytes: 10 * 1024 * 1024,
      timeoutInMs: 100,
      credentials: {
        type: apachePulsarCredentialsTypes.anonymous.value,
        token: ''
      }
    },
    fieldValidators: {
      'clientConfiguration.serviceUrl': [Validators.required],
      'clientConfiguration.topics': [Validators.required],
      'clientConfiguration.subscriptionName': [Validators.required],
      'clientConfiguration.maxNumMessages': [Validators.required],
      'clientConfiguration.maxNumBytes': [Validators.required],
      'clientConfiguration.timeoutInMs': [Validators.required],
    }
  },

  [IntegrationType.PUB_SUB]: {
    clientConfiguration: {
      projectId: '',
      subscriptionId: '',
      serviceAccountKey: '',
      serviceAccountKeyFileName: ''
    },
    fieldValidators: {
      'clientConfiguration.projectId': [Validators.required],
      'clientConfiguration.subscriptionId': [Validators.required],
      'clientConfiguration.serviceAccountKey': [Validators.required],
      'clientConfiguration.serviceAccountKeyFileName': [Validators.required]
    }
  },

  [IntegrationType.CUSTOM]: {
    clazz: '',
    configuration: '',
    fieldValidators: {
      clazz: [Validators.required]
    }
  }
};

export const opcUaMappingType = {
  ID: 'ID',
  FQN: 'Fully Qualified Name'
};

export const extensionKeystoreType = {
  PKCS12: 'PKCS12',
  JKS: 'JKS'
};

export enum InitialPositionInStream {
  LATEST = 'LATEST',
  TRIM_HORIZON = 'TRIM_HORIZON',
  AT_TIMESTAMP = 'AT_TIMESTAMP'
}

export const identityType = {
  anonymous: 'extension.anonymous',
  username: 'extension.username'
};

export const mqttQoSTypes = [
  {
    value: 0,
    name: 'integration.mqtt-qos-at-most-once'
  },
  {
    value: 1,
    name: 'integration.mqtt-qos-at-least-once'
  },
  {
    value: 2,
    name: 'integration.mqtt-qos-exactly-once'
  }];
