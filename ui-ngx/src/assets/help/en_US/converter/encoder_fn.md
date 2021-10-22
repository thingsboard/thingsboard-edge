#### Downlink data converter encoder function

<div class="divider"></div>
<br/>

*function Encoder(msg, metadata, msgType, integrationMetadata): {msg: object, metadata: object, msgType: string}*

JavaScript function used to transform the incoming rule engine message and its metadata to the format that is used by corresponding Integration.

**Parameters:**

<ul>
  <li><b>msg:</b> <code>{[key: string]: any}</code> - JSON with rule engine msg.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - list of key-value pairs with additional data about the message (produced by the rule engine).
  </li>
  <li><b>msgType:</b> <code>{[key: string]: string}</code> - Rule Engine message type.
        See <a href="${baseUrl}/docs/user-guide/rule-engine-2-0/overview/#predefined-message-types" target="_blank">predefined message types</a> for more details..
  </li>
  <li><b>integrationMetadata:</b> <code>{[key: string]: string}</code> - key-value map with some integration specific fields. You can configure additional metadata for each integration in the integration details.
  </li>
</ul>


**Returns:**

Should return a valid JSON document with the following structure:

```json
{
  "contentType": "JSON",
  "data": "{\"tempFreq\":60,\"firmwareVersion\":\"1.2.3\"}",
  "metadata": {
    "topic": "temp-sensor/sensorA/upload"
  }
}
```

<div class="divider"></div>

##### Examples



