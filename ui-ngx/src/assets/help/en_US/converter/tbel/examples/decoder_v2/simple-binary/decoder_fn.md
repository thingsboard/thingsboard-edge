#### Decoder example TBEL function

```javascript
{:code-style="max-height: 500px;"}
var name = "SN-" + parseBytesToInt(payload, 0, 4);

function decodePayload(input) {
  var output = {
    attributes: {},
    telemetry: []
  };

  var timestamp = metadata.ts;

  var decoded = {};
  decoded.battery = parseBytesToInt(payload, 4, 1);
  decoded.temperature = parseBytesToInt(payload, 5, 2) / 100.0;
  decoded.saturation = parseBytesToInt(payload, 7, 1);

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
