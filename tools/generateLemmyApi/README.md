To generate:

1. Go to https://join-lemmy.org/api/main and download the api json.
2. Copy to root of this folder (should be named openapi.json).
3. Run 

```bash
java -jar ../generatePieFedApi/openapi-generator-cli-7.14.0.jar generate -i openapi.json -g kotlin --config api-config.json -o output
```