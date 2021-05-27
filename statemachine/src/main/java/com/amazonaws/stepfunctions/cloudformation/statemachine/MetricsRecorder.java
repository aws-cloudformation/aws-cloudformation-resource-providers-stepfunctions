package com.amazonaws.stepfunctions.cloudformation.statemachine;

import lombok.Setter;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.TerminalException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_INVALID_FORMAT;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_OBJECT_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_S3_LOCATION_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_STRING_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_SUBSTITUTIONS_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.LOGGING_CONFIGURATION_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.MULTIPLE_DEFINITIONS_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_FAILURE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_STATUS;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_SUCCESS;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_TYPE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_JSON;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_SIZE_LIMIT_EXCEEDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_YAML;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_EXPRESS_TYPE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_NAME_GENERATED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_STANDARD_TYPE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.TEMPLATE_MISSING_DEFINITION;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.TRACING_CONFIGURATION_PROVIDED;

/**
 *  Class to encapsulate the logic behind tracking resource property usage
 *  Properties that are changed from their default values are included in
 *  the string generated for metrics tracking.
 */
@Setter
public class MetricsRecorder {

    // CloudFormation operation type and status
    private final HandlerOperationType operationType;
    private boolean isOperationSuccessful;

    // Definition source
    private boolean isDefinitionObjectProvided;
    private boolean isDefinitionStringProvided;
    private boolean isDefinitionS3LocationProvided;

    // S3 Definition type
    private boolean isS3DefinitionJson;
    private boolean isS3DefinitionYaml;

    // State Machine type
    private boolean isStateMachineExpressType;
    private boolean isStateMachineStandardType;

    // Other properties
    private boolean isStateMachineNameGenerated;
    private boolean isLoggingConfigurationProvided;
    private boolean isTracingConfigurationProvided;
    private boolean areDefinitionSubstitutionsProvided;

    // Template failure causes
    private boolean isTemplateMissingDefinition;
    private boolean areMultipleDefinitionsProvided;
    private boolean isS3DefinitionSizeLimitExceeded;
    private boolean isDefinitionInvalidFormat;

    public MetricsRecorder(HandlerOperationType operationType) {
        this.operationType = operationType;
    }

    public void setMetricsFromResourceModel(final ResourceModel model) {
        if (model.getDefinitionString() != null) {
            setDefinitionStringProvided(true);
        }

        if (model.getDefinitionS3Location() != null) {
            setDefinitionS3LocationProvided(true);
        }

        if (model.getDefinition() != null) {
            setDefinitionObjectProvided(true);
        }

        if (model.getDefinitionSubstitutions() != null) {
            setAreDefinitionSubstitutionsProvided(true);
        }

        // State Machine type is STANDARD by default
        if (model.getStateMachineType() == null || model.getStateMachineType().equals(Constants.STANDARD_STATE_MACHINE_TYPE)) {
            setStateMachineStandardType(true);
        } else if (model.getStateMachineType().equals(Constants.EXPRESS_STATE_MACHINE_TYPE)) {
            setStateMachineExpressType(true);
        }

        if (model.getLoggingConfiguration() != null) {
            setLoggingConfigurationProvided(true);
        }

        if (model.getTracingConfiguration() != null && model.getTracingConfiguration().getEnabled()) {
            setTracingConfigurationProvided(true);
        }

        if (model.getStateMachineName() == null) {
            setStateMachineNameGenerated(true);
        }
    }

    public void setMetricsFromException(final Exception e) {
        if (e instanceof TerminalException) {
            switch (e.getMessage()) {
                case Constants.DEFINITION_INVALID_FORMAT_ERROR_MESSAGE: {
                    setDefinitionInvalidFormat(true);
                    break;
                }
                case Constants.DEFINITION_MISSING_ERROR_MESSAGE: {
                    setTemplateMissingDefinition(true);
                    break;
                }
                case Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE: {
                    setAreMultipleDefinitionsProvided(true);
                    break;
                }
            }
        } else if (e instanceof CfnInvalidRequestException) {
            // CfnInvalidRequestExceptions prepend a string to the error message
            if (e.getMessage().contains(Constants.DEFINITION_SIZE_LIMIT_ERROR_MESSAGE)) {
                setS3DefinitionSizeLimitExceeded(true);
            }
        }
    }

    public String generateMetricsString() {
        StringBuilder sb = new StringBuilder();

        // Add prefix for identifying logging entries used for metrics
        sb.append(String.format("%s - ", Constants.METRICS_LOGGING_PREFIX));

        // Add key val metric pairs.
        Map<String, String> loggingPairsToAdd = getMetricsLoggingKeyValMap();
        for (Map.Entry<String, String> loggingPair : loggingPairsToAdd.entrySet()) {
            sb.append(String.format("%s: %s, ", loggingPair.getKey(), loggingPair.getValue()));
        }

        // Add listed metric keys
        List<String> loggingKeysToAdd = getMetricsLoggingKeyList();
        sb.append(String.join(", ", loggingKeysToAdd));

        return sb.toString();
    }

    private Map<String, String> getMetricsLoggingKeyValMap() {
        Map<String, String> loggingPairsToAdd = new LinkedHashMap<>();

        loggingPairsToAdd.put(OPERATION_TYPE.loggingKey, operationType.toString());
        loggingPairsToAdd.put(OPERATION_STATUS.loggingKey, isOperationSuccessful ? OPERATION_SUCCESS.loggingKey : OPERATION_FAILURE.loggingKey);

        return loggingPairsToAdd;
    }

    private List<String> getMetricsLoggingKeyList() {
        List<String> loggingKeysToAdd = new ArrayList<>();

        if (isDefinitionObjectProvided) {
            loggingKeysToAdd.add(DEFINITION_OBJECT_PROVIDED.loggingKey);
        }

        if (isDefinitionStringProvided) {
            loggingKeysToAdd.add(DEFINITION_STRING_PROVIDED.loggingKey);
        }

        if (isDefinitionS3LocationProvided) {
            loggingKeysToAdd.add(DEFINITION_S3_LOCATION_PROVIDED.loggingKey);
        }

        if (isS3DefinitionJson) {
            loggingKeysToAdd.add(S3_DEFINITION_JSON.loggingKey);
        }

        if (isS3DefinitionYaml) {
            loggingKeysToAdd.add(S3_DEFINITION_YAML.loggingKey);
        }

        if (isStateMachineExpressType) {
            loggingKeysToAdd.add(STATE_MACHINE_EXPRESS_TYPE.loggingKey);
        }

        if (isStateMachineStandardType) {
            loggingKeysToAdd.add(STATE_MACHINE_STANDARD_TYPE.loggingKey);
        }

        if (isStateMachineNameGenerated) {
            loggingKeysToAdd.add(STATE_MACHINE_NAME_GENERATED.loggingKey);
        }

        if (isLoggingConfigurationProvided) {
            loggingKeysToAdd.add(LOGGING_CONFIGURATION_PROVIDED.loggingKey);
        }

        if (isTracingConfigurationProvided) {
            loggingKeysToAdd.add(TRACING_CONFIGURATION_PROVIDED.loggingKey);
        }

        if (areDefinitionSubstitutionsProvided) {
            loggingKeysToAdd.add(DEFINITION_SUBSTITUTIONS_PROVIDED.loggingKey);
        }

        if (isTemplateMissingDefinition) {
            loggingKeysToAdd.add(TEMPLATE_MISSING_DEFINITION.loggingKey);
        }

        if (areMultipleDefinitionsProvided) {
            loggingKeysToAdd.add(MULTIPLE_DEFINITIONS_PROVIDED.loggingKey);
        }

        if (isS3DefinitionSizeLimitExceeded) {
            loggingKeysToAdd.add(S3_DEFINITION_SIZE_LIMIT_EXCEEDED.loggingKey);
        }

        if (isDefinitionInvalidFormat) {
            loggingKeysToAdd.add(DEFINITION_INVALID_FORMAT.loggingKey);
        }

        return loggingKeysToAdd;
    }
}
