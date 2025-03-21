# async-order-api
Order API accepts request for an order which is created asynchronously.
1. Upon receiving request to create Order, it adds entry into SQL DB with status as `ACCEPTED`, sends a message on Kafka for further processing and responds with a `202`
2. Eventually when it hears back on a separate Kafka topic about the status of the Order it updates the status of that Order in the SQL DB
3. The service also exposes a `GET` endpoint so that API consumers can view the status of the Order

## Specmatic Contract Test Setup
1. We use OpenAPI spec to contract test on the HTTP interface which results in the application posting to a Kafka Broker
2. And Specmatic Kafka Mock uses AsyncAPI spec to spin up a Mock Kafka Broker to received, validate and respond with appropriate messages
  a. We are leveraging `request-reply` pattern in AsyncAPI here
