import { Validators } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { baseUrl } from '@app/core/utils';

export const templates = {
  http: {
    baseUrl: baseUrl(),
    replaceNoContentToOk: '',
    enableSecurity: '',
    downlinkUrl: 'https://api.thingpark.com/thingpark/lrc/rest/downlink',
    enableSecurityNew: '',
    asId: '',
    asIdNew: '',
    asKey: '',
    clientIdNew: '',
    clientSecret: '',
    maxTimeDiffInSeconds: '',
    httpEndpoint: '',
    headersFilter: ''
  },
  [IntegrationType.MQTT]: {
    clientConfiguration: {
      host: '',
      port: '',
      cleanSession: '',
      ssl: '',
      connectTimeoutSec: '',
      clientId: '',
      credentialsType: '',
      mqttUsername: '',
      mqttPassword: '',
      privateKeyPassword: ''
    },
    downlinkTopicPattern: '${topic}',
    topicFilters: []
  },
  [IntegrationType.AWS_IOT]: {
    host: '',
    number: '',
    clientId: '',
    connectTimeoutSec: '',
    credentials: {
      type: '',
      caCertFileName: '',
      caCert: '',
      certFileName: '',
      cert: '',
      privateKeyFileName: '',
      privateKey: '',
      password: ''
    }
  },
  [IntegrationType.AWS_SQS]: {
    sqsConfiguration: {
      queueUrl: '',
      pollingPeriodSeconds: '',
      region: '',
      accessKeyId: '',
      secretAccessKey: ''
    }
  },
  [IntegrationType.AWS_KINESIS]: {
    streamName: '',
    region: '',
    accessKeyId: '',
    secretAccessKey: '',
    useCredentialsFromInstanceMetadata: '',
    applicationName: '',
    initialPositionInStream: '',
    useConsumersWithEnhancedFanOut: ''
  },
  [IntegrationType.IBM_WATSON_IOT]: {
    connectTimeoutSec: '',
    credentials: {
      username: '',
      password: '',
    },
    configuration: {
      topicFilters: '',
      downlinkTopicPattern: ''
    }
  },
  [IntegrationType.TTN]: {
    currentHostType: '',
    $parent: {
      hostRegion: '',
      hostCustom: ''
    },
    connectTimeoutSec: '',
    credentials: {
      username: '',
      password: ''
    },
    configuration: {
      topicFilters: '',
      downlinkTopicPattern: ''
    }
  },
  [IntegrationType.AZURE_EVENT_HUB]: {
    connectTimeoutSec: '',
    namespaceName: '',
    eventHubName: '',
    sasKeyName: '',
    sasKey: '',
    iotHubName: '',
  },
  [IntegrationType.OPC_UA]: {
    applicationName: '',
    applicationUri: '',
    host: '',
    port: '',
    scanPeriodInSeconds: '',
    timeoutInMillis: '',
    security: '',
    identity: {
      password: '',
      username: '',
      type: 'anonymous'
    },
    mapping: [],
    keystore: {
      fileContent: '',
      type: '',
      location: '',
      password: '',
      alias: '',
      keyPassword: ''
    }
  },
  [IntegrationType.UDP]: {
    currentHostType: '',
    $parent: {
      hostRegion: '',
      hostCustom: ''
    },
    connectTimeoutSec: '',
    credentials: {
      username: '',
      password: '',
      topicFilters: '',
      downlinkTopicPattern: ''
    }
  },
  [IntegrationType.TCP]: {
    port: '',
    soBacklogOption: '',
    soRcvBuf: '',
    soSndBuf: '',
    soKeepaliveOption: '',
    tcpNoDelay: '',
    handlerConfiguration: {
      handlerType: '',
      maxFrameLength: '',
      stripDelimiter: '',
      messageSeparator: '',
      byteOrder: '',
      lengthFieldOffset: '',
      lengthFieldLength: '',
      lengthAdjustment: '',
      initialBytesToStrip: '',
      failFast: ''
    }
  },
  [IntegrationType.KAFKA]: {
    groupId: '',
    clientId: '',
    topics: '',
    bootstrapServers: '',
    pollInterval: '',
    autoCreateTopics: '',
  },
  [IntegrationType.CUSTOM]: {
    clazz: '',
    configuration: ''
  }
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

export const topicFilters = {

}

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
}

export const tcpTextMessageSeparator = {
  systemLineSeparator: {
    value: 'SYSTEM_LINE_SEPARATOR'
  },
  nulDelimiter: {
    value: 'NUL_DELIMITER'
  }
}

export const tcpBinaryByteOrder = {
  littleEndian: {
    value: 'LITTLE_ENDIAN'
  },
  bigEndian: {
    value: 'BIG_ENDIAN'
  }
}

export const opcSecurityTypes = {
  Basic128Rsa15: 'Basic128Rsa15',
  Basic256: 'Basic256',
  Basic256Sha256: 'Basic256Sha256',
  None: 'None'
}

export const identityType = {
  anonymous: 'extension.anonymous',
  username: 'extension.username'
}

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
  }]