#### Decoder example TBEL function

```javascript
{:code-style="max-height: 500px;"}

var data = decodeToJson(payload);

var name = data.serialNumber;

function decodePayload(input) {
  var output = {
    attributes: {},
    telemetry: []
  };

  var timestamp = metadata.ts;

  var decoded = {};
  decoded.battery = data.battery;
  decoded.temperature = data.temperature;
  decoded.saturation = data.saturation;

  output.telemetry = [{
    ts: timestamp,
    values: decoded
  }];
  
  return output;
}

var telemetry = [];
var attributes = {};

var customDecoding = decodePayload(payload);

if (customDecoding.?telemetry.size() > 0) {
  if (customDecoding.telemetry instanceof java.util.ArrayList) {
    foreach(telemetryObj: customDecoding.telemetry) {
      if (telemetryObj.ts != null && telemetryObj.values != null) {
        telemetry.add(telemetryObj);
      }
    }
  } else {
    telemetry.putAll(customDecoding.telemetry);
  }
}

if (customDecoding.?attributes.size() > 0) {
  attributes.putAll(customDecoding.attributes);
}

var result = {
  name: name,
  attributes: attributes,
  telemetry: telemetry
};

return result;

{:copy-code}
```

<br>
<br>
