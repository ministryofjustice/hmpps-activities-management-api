# HTTP Client

## Setup

Create a file called `http-client.private.env.json` like this:

```json
{
  "local": {
    "api-client-id": "<<api-client-id>>",
    "api-client-secret": "<<api-client-secret>>"
  },
  "dev": {
    "api-client-id": "<<api-client-id>>",
    "api-client-secret": "<<api-client-secret>>"
  }
}
```

where `<<api-client-id>>` and `<<api-client-secret>>` can be obtained from Kubernetes secrets.

See IntelliJ HTTP client docs [here](https://www.jetbrains.com/help/idea/http-client-variables.html#example-working-with-environment-files).