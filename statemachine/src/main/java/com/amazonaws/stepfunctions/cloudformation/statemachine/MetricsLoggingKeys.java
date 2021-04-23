package com.amazonaws.stepfunctions.cloudformation.statemachine;

/**
 *  Enum to represent the logging keys used in the metrics string generated
 *  by the MetricsRecorder class.
 */
public enum MetricsLoggingKeys {
    OPERATION_TYPE("OperationType"),
    OPERATION_STATUS("OperationStatus"),
    OPERATION_SUCCESS("SUCCESS"),
    OPERATION_FAILURE("FAILURE"),
    DEFINITION_OBJECT_PROVIDED("DefinitionObjectProvided"),
    DEFINITION_STRING_PROVIDED("DefinitionStringProvided"),
    DEFINITION_S3_LOCATION_PROVIDED("DefinitionS3LocationProvided"),
    S3_DEFINITION_YAML("S3DefinitionYaml"),
    S3_DEFINITION_JSON("S3DefinitionJson"),
    STATE_MACHINE_STANDARD_TYPE("StateMachineStandardType"),
    STATE_MACHINE_EXPRESS_TYPE("StateMachineExpressType"),
    STATE_MACHINE_NAME_GENERATED("StateMachineNameGenerated"),
    LOGGING_CONFIGURATION_PROVIDED("LoggingConfigurationProvided"),
    TRACING_CONFIGURATION_PROVIDED("TracingConfigurationProvided"),
    DEFINITION_SUBSTITUTIONS_PROVIDED("DefinitionSubstitutionsProvided"),
    TEMPLATE_MISSING_DEFINITION("TemplateMissingDefinition"),
    MULTIPLE_DEFINITIONS_PROVIDED("MultipleDefinitionsProvided"),
    S3_DEFINITION_SIZE_LIMIT_EXCEEDED("S3DefinitionSizeLimitExceeded"),
    DEFINITION_INVALID_FORMAT("DefinitionInvalidFormat");

    public final String loggingKey;

    MetricsLoggingKeys(String loggingKey) {
        this.loggingKey = loggingKey;
    }
}
