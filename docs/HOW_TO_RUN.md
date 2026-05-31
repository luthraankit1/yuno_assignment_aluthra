# How to run

## System Requirements
- Docker
- Docker compose
- Windows batch file execution capabilities

## Running the project

The project is dockerized for ease of startup and execution.

Current scripts only support windows based bat file executions.

### Execution
- Run the gradle task "deploy-jar" under group "deploy-local". This will build the app and place the jar in the required docker volume. 

- Open the windows cmd to execute, or double-click on the [docker application start script](../docker/start-app.bat). This will create the local docker images required to run the application and fire up the compose project. 

Given below is a curl command to execute the API call

```
curl --request POST \
  --url http://localhost:8080/api/v1/payments \
  --header 'Content-Type: application/json' \
  --header 'PAYMENT-IDENTIFIER: a92218fb-44a8-4c64-9dd5-b7369844ad14' \
  --data '{
	"customerId": "customer01",
	"amount": "249.00",
	"currency": "EUR",
	"country": "IND",
	"paymentMethodToken": "daf5012c-1891-4616-8b8c-8d85a7849ff1"
}'
```