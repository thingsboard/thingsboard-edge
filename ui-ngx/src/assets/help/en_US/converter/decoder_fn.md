#### Uplink data converter decoder function

<div class="divider"></div>
<br/>

*function Decoder(payload, metadata): {msg: object, metadata: object, msgType: string}*

JavaScript function used to parse payload of the incoming message and transform it to format that platform uses.

**Parameters:**

<ul>
  <li><b>payload:</b> <code>any</code> - is a payload of the incoming message produced by the corresponding Integration.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a metadata key/value map with some integration specific fields.
  </li>
</ul>

**Returns:**

Should return a valid JSON document with the following structure:

```json
{
  "deviceName": "Device A",
  "deviceType": "thermostat",
  "customerName": "Company Name",
  "groupName": "Thermostats",
  "attributes": {
    "model": "Model A",
    "serialNumber": "SN-111",
    "integrationName": "Test integration"
  },
  "telemetry": {
    "temperature": 42,
    "humidity": 80
  }
}
```

<div class="divider"></div>

##### Examples



