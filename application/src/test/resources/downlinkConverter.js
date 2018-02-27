
var result = {
    temperature: payload.updatedAttributes.temperature.value
};

return {
    contentType: "JSON",
    data: JSON.stringify(result),
    metadata: {
        topic: metadata["topicPrefix"] + "/upload"
    }
};
