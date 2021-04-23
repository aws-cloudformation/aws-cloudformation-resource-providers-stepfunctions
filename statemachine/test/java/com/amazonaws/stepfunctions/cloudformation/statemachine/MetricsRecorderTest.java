package com.amazonaws.stepfunctions.cloudformation.statemachine;

import org.junit.jupiter.api.Test;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.Constants.METRICS_LOGGING_PREFIX;
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
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsRecorderTest {

    @Test
    public void testOnlyOperationTypeAndStatusLogged_whenOtherPropertiesAreDefaultValues_CreateType_Failure() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);

        String expectedDefaultLoggingString = String.format("%s - %s: %s, %s: %s, ",
                METRICS_LOGGING_PREFIX,
                OPERATION_TYPE.loggingKey,
                HandlerOperationType.CREATE.toString(),
                OPERATION_STATUS.loggingKey,
                OPERATION_FAILURE.loggingKey
        );

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).isEqualTo(expectedDefaultLoggingString);
    }

    @Test
    public void testOnlyOperationTypeAndStatusLogged_whenOtherPropertiesAreDefaultValues_DeleteType_Success() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.DELETE);
        metricsRecorder.setOperationSuccessful(true);

        String expectedDefaultLoggingString = String.format("%s - %s: %s, %s: %s, ",
                METRICS_LOGGING_PREFIX,
                OPERATION_TYPE.loggingKey,
                HandlerOperationType.DELETE.toString(),
                OPERATION_STATUS.loggingKey,
                OPERATION_SUCCESS.loggingKey
        );

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).isEqualTo(expectedDefaultLoggingString);
    }

    @Test
    public void testSingleKeyLogged_whenSinglePropertyChangedFromDefault_DefinitionObject() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setDefinitionObjectProvided(true);

        String expectedDefaultLoggingString = String.format("%s - %s: %s, %s: %s, %s",
                METRICS_LOGGING_PREFIX,
                OPERATION_TYPE.loggingKey,
                HandlerOperationType.CREATE.toString(),
                OPERATION_STATUS.loggingKey,
                OPERATION_FAILURE.loggingKey,
                DEFINITION_OBJECT_PROVIDED.loggingKey
        );

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).isEqualTo(expectedDefaultLoggingString);
    }

    @Test
    public void testSingleKeyLogged_whenSinglePropertyChangedFromDefault_StandardType() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineStandardType(true);

        String expectedDefaultLoggingString = String.format("%s - %s: %s, %s: %s, %s",
                METRICS_LOGGING_PREFIX,
                OPERATION_TYPE.loggingKey,
                HandlerOperationType.CREATE.toString(),
                OPERATION_STATUS.loggingKey,
                OPERATION_FAILURE.loggingKey,
                STATE_MACHINE_STANDARD_TYPE.loggingKey
        );

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).isEqualTo(expectedDefaultLoggingString);
    }

    @Test
    public void testMultipleKeysLogged_whenMultiplePropertiesChangedFromDefault_DefinitionObject_S3Yaml() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineStandardType(true);
        metricsRecorder.setS3DefinitionYaml(true);

        String expectedDefaultLoggingString = String.format("%s - %s: %s, %s: %s, %s, %s",
                METRICS_LOGGING_PREFIX,
                OPERATION_TYPE.loggingKey,
                HandlerOperationType.CREATE.toString(),
                OPERATION_STATUS.loggingKey,
                OPERATION_FAILURE.loggingKey,
                S3_DEFINITION_YAML.loggingKey,
                STATE_MACHINE_STANDARD_TYPE.loggingKey
        );

        String generatedLoggingString = metricsRecorder.generateMetricsString();

        assertThat(generatedLoggingString).isEqualTo(expectedDefaultLoggingString);
    }

    @Test
    public void testMultipleKeysLogged_whenMultiplePropertiesChangedFromDefault_StandardType_S3Json() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineStandardType(true);
        metricsRecorder.setS3DefinitionJson(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(STATE_MACHINE_STANDARD_TYPE.loggingKey);
        assertThat(generatedLoggingString).contains(S3_DEFINITION_JSON.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_definitionStringProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setDefinitionStringProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(DEFINITION_STRING_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_definitionS3LocationProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setDefinitionS3LocationProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(DEFINITION_S3_LOCATION_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_s3DefinitionJson() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setS3DefinitionJson(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(S3_DEFINITION_JSON.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_s3DefinitionYaml() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setS3DefinitionYaml(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(S3_DEFINITION_YAML.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_stateMachineExpressType() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineExpressType(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(STATE_MACHINE_EXPRESS_TYPE.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_stateMachineStandardType() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineStandardType(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(STATE_MACHINE_STANDARD_TYPE.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_stateMachineNameProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setStateMachineNameGenerated(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(STATE_MACHINE_NAME_GENERATED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_loggingConfigurationProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setLoggingConfigurationProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(LOGGING_CONFIGURATION_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_tracingConfigurationProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setTracingConfigurationProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(TRACING_CONFIGURATION_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_areDefinitionSubstitutionsProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setAreDefinitionSubstitutionsProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(DEFINITION_SUBSTITUTIONS_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_templateMissingDefinition() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setTemplateMissingDefinition(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(TEMPLATE_MISSING_DEFINITION.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_areMultipleDefinitionsProvided() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setAreMultipleDefinitionsProvided(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(MULTIPLE_DEFINITIONS_PROVIDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_s3DefinitionSizeLimitExceeded() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setS3DefinitionSizeLimitExceeded(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(S3_DEFINITION_SIZE_LIMIT_EXCEEDED.loggingKey);
    }

    @Test
    public void testKeyLogged_whenPropertyChangedFromDefault_definitionInvalidFormat() {
        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setDefinitionInvalidFormat(true);

        String generatedLoggingString = metricsRecorder.generateMetricsString();
        assertThat(generatedLoggingString).contains(DEFINITION_INVALID_FORMAT.loggingKey);
    }

}
