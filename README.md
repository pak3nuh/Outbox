# Outbox

Generic implementation of the Outbox pattern.

## Testing

Testing is done mainly on unit tests, but since some parts depend on SQL, the integration tests module tests
specific databases to ensure SQL compatibility.

The integration module is using TestContainers and requires docker installed to create containers for the different 
supported databases. For now, tests include:
- Postgres 15
- MySql 8
- H2

The integration test module doesn't run by default. To run you need to add the property `integration-test` like so
`gradle integration-test:test -Pintegration-test`.

### TODO list

- document database schema or provide a liquibase patch
