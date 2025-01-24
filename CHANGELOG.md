# Changelog

All notable changes to this project will be documented in this file.

## [0.0.1-RC14]

- LeiaMessageProduceClient
    - Introduced a TargetValidator for dynamically validating transformation targets.
    - Both the messageProcessor and the target validator can be runtime args along with initialzied values
- SchemaDetails :
    - Introduced JsonNode along with SchemaDetails, clients can keep custom data with their schema
      definitions if required
    - Supporting the latest version match. In the SchemaDefinition latest version could be provided instead of the
      exact version.
- Multiple Version Support:
    - Support to handle multiple schema versions. Introduced histories at a SchemaDetails level to incubate the
      necessary
      information.
- Aerospike Repository:
    - Support for AS data store, along with the dw bundle

## [0.0.1-RC13]

- LeiaClientBundle: Use dropwizard lifecycle to initialize the validator, refresher and producer client
- LeiaRefresher: Add a start method to initialize data instead of doing it in constructor

## [0.0.1-RC12]

- LeiaMessageProduceClient
    - Introduced a flag to control whether the source message should be included during multiplexing.
    - Modified the message datatype to JsonNode from byte[] to reduce serialization overhead.

## [0.0.1-RC11]

- MessageProcessor
    - Introduced MessageProcessor in LeiaMessageProduceClient instead of a lambda
    - GetMessages from LeiaMessageProducerClient is now public

## [0.0.1-RC10]

- LeiaSchemaViolation
    - Introduced SchemaViolation & ViolationContext for generating violations as part of schema validation
- SchemaValidationUtils: Returns list of LeiaSchemaViolations instead of boolean
- StaticSchemaValidator: Updated to return all schema validation violations instead of stopping at the first error
  occurrence
- LeiaClientMarshaller: Bug fix in serialization
- Adds tests for `leia-schema-validator` module

## [0.0.1-RC9]

- RollOverAndUpdate
    - Introduced rollOverAndUpdate, fixing a bug of multiple approved records.

## [0.0.1-RC8]

- SchemaDetails:
    - Moved the nested `SchemaKey` attributes to the class level
    - made the getReferenceId() uniform with `SchemaKey` implementation
- FieldUtils: Excluding non-serializable fields(static, transient, @JsonIgnore) for building the schema attributes
- SchemaResource: Bug fix in SchemaValidation resource
- Removed the unused `LeiaCompiledPath` class

## [0.0.1-RC7]

- Introduced a `leia-common` module to host all the common utils classes
- Added annotation classes for the Qualifiers( PII, Encrypted, ShortLived) that can be added on the members of
  the Schema class
- Replaced the `SchemaValidatable` annotation and moved it to a generic `SchemaDefinition`
- Introduced `SchemaBuilder`: For building the schema request against a class annotated with SchemaDefinition
- Introduced `SchemaPayloadValidator`: For validating a schema json payload against a specified SchemaKey
- Addressed issues in handling plain `Object` and Primitive Class types in `SchemaValidationUtils`

## [0.0.1-RC6]

- LeiaClient - Introduced AuthHeaderSupplier in LeiaClientSupplier to support authentication on leia-server
- ElasticRepository query NPE fixes

## [0.0.1-RC5]

- Introduced a PermissionValidator and introduced suppliers for versionGenerator, SchemaUpdater and PermissionValidator,
  to facilitate clients implement them via Dependency Injection
- Removed the unnecessary `LeiaMessages` data-model and converged to a Map Structure
- A few linting fixes, along the way

## [0.0.1-RC4]

- Periodic Refresh - Added support to enable/disable periodicRefresh. Default value is true
- Event Multiplexing - Added support for event transformations. Can multiplex one event into multiple events.

## [0.0.1-RC3]

- SchemaValidationUtils - Add support for nested attributes. Refactor to validate schema and individual attributes
  separately.
- Simplified NamespaceDataSource to use a supplier instead of hierarchy for static and dynamic source
- LeiaBundle: Fixed duplicate instantiation of schema repository
- TimeBasedDataProvider: Fixed the initial delay of executor to prevent multiple invocation of update on startup
- Schema Validator: Removed instance creation of schema class. It's not required for static validations.

## [0.0.1-RC2]

- Added tests to the core functionality to schemaValidator, refresher, client, core and models
- Fixed the validation bug in SchemaValidationUtils. field.getType().isInstanceOf is a miss, Class.isAssignable is the
  correct way to check for class assignments.
- Some minor code formatting and linting fixes

## [0.0.1-RC1]

- A versioned schema registry, to register various schemas with all primitive and custom data-types, bound by a
  maker-checker process, with customizable RBAC.
- Ability to dynamically create classifications, PII, TIME_SENSITIVE etc
- A RESTful interface and a console to manage the said schemas, to allow for easier integrations
- A client to help serialize and de-serialize the data, at production and consumption levels respectively, with a fluid
  interface that can work with any infrastructure component.
- Supports json to begin with. (Avro and Protobuf shall be accommodated later on)