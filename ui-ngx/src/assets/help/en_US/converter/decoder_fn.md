#### Uplink data converter decoder function

<div class="divider"></div>
<br/>

*function Decoder(payload, metadata): object | object[]*

JavaScript function used to parse payload of the incoming message and transform it to format that platform uses.

**Parameters:**

<ul>
  <li><b>payload:</b> <code>any</code> - is a payload of the incoming message produced by the corresponding Integration.<br>
     Payload is one of the following content types: JSON, TEXT, Binary(Base64) and is specific to your Integration type.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a metadata key/value map with some integration specific fields.<br>
     You can configure additional metadata for each integration in the integration details. 
  </li>
</ul>

**Returns:**

Should return a valid JSON document with the following structure:

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Example json output">
</div>

<br>

**NOTE**: The only mandatory parameters in the output JSON are **deviceName** and **deviceType**.

Function can also return array of device values and/or contain timestamps in the telemetry values. For example:

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/json_array_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Example json array output">
</div>

<div class="divider"></div>

##### Examples

###### Complex decoder example with hex encoded value field of the payload 

Let’s assume a complex example where payload is encoded in hex “value” field and there is a timestamp<br>
associated with each record. First two bytes of “value” field contain battery and second two bytes contain temperature.

<table style="max-width: 400px;">
<thead>
<tr>
<th style="max-width: 200px; padding-left: 22px;">
<b>Input arguments</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Decoder function</b>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<span tb-help-popup="converter/examples/decoder/example1/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="payload"></span><br><br>
<span tb-help-popup="converter/examples/decoder/example1/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/example1/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
</tr>
</tbody>
</table>

<br>
<br>
