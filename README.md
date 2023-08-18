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

or a single test by `./gradlew integration-test:test -Pintegration-test --tests 'MySqlEntryStorageTest'`

## Database schema

The expected schema is specified on a [liquibase](https://docs.liquibase.com) changelog under [./liquibase](./liquibase)
folder. Besides the schema, it also includes a bootstrap runner of liquibase where one can simply specify the correct
arguments, and it runs against a specific DB instance.

The changelog file is configurable with some properties, set by prefixing them with `-D` after the command:

| Name              | Description                         | Default           |
|-------------------|-------------------------------------|-------------------|
| LOCKS_TABLE_NAME  | Name of the application locks table | application_locks |
| OUTBOX_TABLE_NAME | Name of the outbox records table    | stored_entries    |

The [docker-compose](./liquibase/docker-compose.yaml) file should be an easy way to test or apply the schemas
to a long running database.

### Database support

Currently, the supported databases are:
- H2
- Postgres
- MySql

Adding support for more databases is not hard. To do so we just need to add the required changeset to the liquibase
schema, add the integration tests for the specific database (should be a simple copy from the existing ones) and
add the driver as a test dependency.

## Resources

- https://mockk.io/
- https://www.ktorm.org/

### TODO list
