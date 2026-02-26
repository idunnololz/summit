
get latest swagger from: https://stable.wjs018.xyz/api/alpha/swagger
https://stable.wjs018.xyz/api/alpha/swagger.json

config docs: https://openapi-generator.tech/docs/generators/kotlin

```bash
java -jar openapi-generator-cli-7.14.0.jar generate -i swagger.json -g kotlin --config api-config.json -o output
```