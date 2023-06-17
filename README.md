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
`./gradlew integration-test:test -Pintegration-test`.

## Database schema

The expected schema is specified on a [liquibase](https://docs.liquibase.com) changelog under [./liquibase](./liquibase)
folder. Besides the schema, it also includes a bootstrap runner of liquibase where one can simply specify the correct
arguments, and it runs against a specific DB instance.

The changelog file is configurable with some properties, set by prefixing them with `-D` after the command:

| Name              | Description                         | Default           |
|-------------------|-------------------------------------|-------------------|
| LOCKS_TABLE_NAME  | Name of the application locks table | application_locks |
| OUTBOX_TABLE_NAME | Name of the outbox records table    | stored_entries    |

## Resources

- https://mockk.io/
- https://www.ktorm.org/

### TODO list

- builder pattern to help instantiation
