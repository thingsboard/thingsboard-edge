import { Validators } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';

export const templates = {
  [IntegrationType.HTTP]: {
    baseUrl: ['', Validators.required],
    replaceNoContentToOk: [],
    enableSecurity: [],
    downlinkUrl: [],
    enableSecurityNew: [],
    asId: [],
    asIdNew: [],
    asKey: [],
    clientIdNew: [],
    clientSecret: [],
    maxTimeDiffInSeconds: [],
    httpEndpoint: []
  },
  [IntegrationType.OCEANCONNECT]: {},
  [IntegrationType.SIGFOX]: {},
  [IntegrationType.THINGPARK]: {},
  [IntegrationType.TPE]: {},
  [IntegrationType.TMOBILE_IOT_CDP]: {},
  [IntegrationType.MQTT]: {
    host: [],
    port: [],
    cleanSession: [],
    ssl: [],
    connectTimeoutSec: [],
    clientId: [],
    credentialsType: [],
    mqttUsername: [],
    mqttPassword: [],
    privateKeyPassword: []
  },
  [IntegrationType.AWS_IOT]: {
    host: [],
    number: [],
    clientId: [],
    connectTimeoutSec: [],
    credentials: {
      type: [],
      caCertFileName: [],
      caCert: [],
      certFileName: [],
      cert: [],
      privateKeyFileName: [],
      privateKey: [],
      password: []
    }
  },
  [IntegrationType.AWS_SQS]: {
    queueUrl: [],
    pollingPeriodSeconds: [],
    region: [],
    accessKeyId: [],
    secretAccessKey: []
  },
  [IntegrationType.AWS_KINESIS]: {
    streamName: [],
    region: [],
    accessKeyId: [],
    secretAccessKey: [],
    useCredentialsFromInstanceMetadata: [],
    applicationName: [],
    initialPositionInStream: [],
    useConsumersWithEnhancedFanOut: []
  },
  [IntegrationType.IBM_WATSON_IOT]: {
    connectTimeoutSec: [],
    credentials: {
      username: [],
      password: [],
    },
    configuration: {
      topicFilters: [],
      downlinkTopicPattern: []
    }
  },
  [IntegrationType.TTN]: {
    currentHostType: [],
    $parent: {
      hostRegion: [],
      hostCustom: []
    },
    connectTimeoutSec: [],
    credentials: {
      username: [],
      password: []
    },
    configuration: {
      topicFilters: [],
      downlinkTopicPattern: []
    }
  },
  [IntegrationType.AZURE_EVENT_HUB]: {
    connectTimeoutSec: [],
    namespaceName: [],
    eventHubName: [],
    sasKeyName: [],
    sasKey: [],
    iotHubName: [],
  },
  [IntegrationType.OPC_UA]: {
    applicationName: [],
    applicationUri: [],
    host: [],
    port: [],
    scanPeriodInSeconds: [],
    timeoutInMillis: [],
    security: [],
    identityType: [],
    identity: {
      password: [],
      username: [],
      security: [],
      type: []
    },
    keystore: {
      fileContent: [],
      type: [],
      location: [],
      password: [],
      alias: [],
      keyPassword: []
    }
  },
  [IntegrationType.UDP]: {
    currentHostType: [],
    $parent: {
      hostRegion: [],
      hostCustom: []
    },
    connectTimeoutSec: [],
    credentials: {
      username: [],
      password: [],
      topicFilters: [],
      downlinkTopicPattern: []
    }
  },
  [IntegrationType.TCP]: {
    port: [],
    soBacklogOption: [],
    soRcvBuf: [],
    soSndBuf: [],
    soKeepaliveOption: [],
    tcpNoDelay: [],
    handlerConfiguration: {
      handlerType: [],
      maxFrameLength: [],
      stripDelimiter: [],
      messageSeparator: [],
      byteOrder: [],
      lengthFieldOffset: [],
      lengthFieldLength: [],
      lengthAdjustment: [],
      initialBytesToStrip: [],
      failFast: []
    }
  },
  [IntegrationType.KAFKA]: {
    groupId: [],
    clientId: [],
    topics: [],
    bootstrapServers: [],
    pollInterval: [],
    autoCreateTopics: [],
  },
  [IntegrationType.CUSTOM]: {}
}

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
}

export const opcUaMappingType = {
  ID: 'ID',
  FQN: 'Fully Qualified Name'
}

export const extensionKeystoreType = {
  PKCS12: 'PKCS12',
  JKS: 'JKS'
}

export const initialPositionInStream = {
  latest: 'LATEST',
  trim_horizon: 'TRIM_HORIZON',
  at_timestamp: 'AT_TIMESTAMP'
}
