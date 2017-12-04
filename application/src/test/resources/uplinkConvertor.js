function convertToBytes(bytes) {
    var result = [];
    for (var i = 0; i < bytes.length; i++) {
        result.push(bytes[i]);
    }
    return result;
}

function convertUplinkInternal(bytes, metadata) {
    return convertUplink(convertToBytes(bytes), metadata);
}

function convertUplink(bytes, metadata) {
    var deviceName = String.fromCharCode.apply(String, bytes);
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