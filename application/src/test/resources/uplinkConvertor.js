function convertUplinkInternal(bytes, metadata) {
    var payload = [];
    for (var i = 0; i < bytes.length; i++) {
        payload.push(bytes[i]);
    }
    return convertUplink(payload, metadata);
}

function convertUplink(payload, metadata) {
    var deviceName = String.fromCharCode.apply(String, payload);
    var telemetryKeyName = metadata.telemetryKeyName;
    return JSON.stringify(
        {
            deviceName: deviceName,
            telemetry: {
                telemetryKeyName: 42
            }
        }
    );
}