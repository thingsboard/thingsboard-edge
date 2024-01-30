#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Example

Let's assume that a tenant manages temperature sensors.
When a temperature sensor reports a high temperature, the platform creates an alarm.

In addition, let's assume that each sensor has a group of alarm notification receivers
associated with it and a primary user that responsible for updating the alarm status.

Imagine that after alarm creation we fetched information about notification receivers and
primary users with the help of enrichment rule nodes and after that our message looks like this:

```json
{
  "msg": {
    "temperature": 32
  },
  "metadata": {
    "deviceType": "Thermostat",
    "deviceName": "TH-001",
    "ts": "1685379440000",
    "sender": "+12025550193",
    "receivers": "+12025550172,+12025550178,+12025550185"
  }
}
```
<br>

Here is a node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/twilio-voice-phone-ft.png)

After message evaluation by a rule node the outgoing message will be looks like this:

```json
{
  "msg": {
    "numberFrom": "+12025550193",
    "numbersTo": "+12025550172,+12025550178,+12025550185",
    "accountSid": "ACCede1F56C3eb61BA8c3FB87a9B66E2cd",
    "accountToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTS3h4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4LTE0NTA0NzExNDciLCJpc3MiOiJTS3h4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4Iiwic3ViIjoiQUN4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eCIsIm5iZiI6MTQ1MDQ3MTE0NywiZXhwIjoxNDUwNDc0NzQ3LCJncmFudHMiOnsiaWRlbnRpdHkiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaXBfbWVzc2FnaW5nIjp7InNlcnZpY2Vfc2lkIjoiSVN4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eCIsImVuZHBvaW50X2lkIjoiSGlwRmxvd1NsYWNrRG9ja1JDOnVzZXJAZXhhbXBsZS5jb206c29tZWlvc2RldmljZSJ9fX0.IHx8KeH1acIfwnd8EIin3QBGPbfnF-yVnSFp5NpQJi0",
    "provider": "Alice",
    "language": "en-US",
    "voice": "alice",
    "pitch": "100",
    "rate": "100",
    "volume": "70",
    "startPause": "5"
  },
  "metadata": {
    "deviceType": "Thermostat",
    "deviceName": "TH-001",
    "ts": "1685379440000",
    "sender": "+12025550193",
    "receivers": "+12025550172,+12025550178,+12025550185"
  }
}
```

<br>

This example show-case using the **twilio** node with dynamic configuration based on the substitution of message and message metadata fields.

<br>
<br>
