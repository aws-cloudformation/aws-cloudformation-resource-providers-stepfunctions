package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.services.stepfunctions.model.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResourceModelUtilsTest extends HandlerTestBase {
    private final String STATE_MACHINE_VERSION_1_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:MyStateMachine:1";
    private final String STATE_MACHINE_VERSION_2_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:MyStateMachine:2";

    @Test
    public void testGenerateAliasNameIfNotProvided_withModelThatHasNoAliasName_setsRandomAliasName() {
        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .logicalResourceIdentifier("StateMachineAlias")
                .clientRequestToken("8e6e2221-86b0-4b7d-bfe6-d2e67c42047a")
                .build();

        final ResourceModel model = ResourceModel.builder().build();

        assertThat(model.getName()).isNull();
        ResourceModelUtils.generateAliasNameIfNotProvided(cfnRequest, model);
        assertThat(model.getName()).isNotNull();
    }

    @Test
    public void testValidateDeploymentPreference_withLinearDeploymentConfiguration_succeeds() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        ResourceModelUtils.validateDeploymentPreference(deploymentPreference);
    }

    @Test
    public void testValidateDeploymentPreference_withLinearDeploymentConfiguration_exceedsMaxDeploymentTime_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(Constants.MAX_DEPLOYMENT_TIME_MINUTES);
        deploymentPreference.setPercentage(1);

        final ResourceModel desiredResourceState = new ResourceModel();
        desiredResourceState.setDeploymentPreference(deploymentPreference);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "The linear deployment configured is estimated to take 210000 minutes, which exceeds the maximum allowable deployment time of 2100 minutes. " +
                "Configure the deployment preference to use a higher shift percentage or lower time interval and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withLinearDeploymentConfiguration_missingIntervalAndPercentage_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'LINEAR' require an interval and percentage for traffic shifting. " +
                "Configure the deployment preference with the 'interval' and 'percentage' properties and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withLinearDeploymentConfiguration_missingInterval_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setPercentage(1);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'LINEAR' require an interval for traffic shifting. " +
                "Configure the deployment preference with the 'interval' property and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withLinearDeploymentConfiguration_missingPercentage_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(1);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'LINEAR' require a percentage for traffic shifting. " +
                "Configure the deployment preference with the 'percentage' property and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withCanaryDeploymentConfiguration_succeeds() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        ResourceModelUtils.validateDeploymentPreference(deploymentPreference);
    }

    @Test
    public void testValidateDeploymentPreference_withCanaryDeploymentConfiguration_missingIntervalAndPercentage_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'CANARY' require an interval and percentage for traffic shifting. " +
                "Configure the deployment preference with the 'interval' and 'percentage' properties and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withCanaryDeploymentConfiguration_missingInterval_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setPercentage(1);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'CANARY' require an interval for traffic shifting. " +
                "Configure the deployment preference with the 'interval' property and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withCanaryDeploymentConfiguration_missingPercentage_throwsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setInterval(1);

        assertDeploymentPreferenceValidationFailure(
                deploymentPreference,
                "Deployments of type 'CANARY' require a percentage for traffic shifting. " +
                "Configure the deployment preference with the 'percentage' property and try again."
        );
    }

    @Test
    public void testValidateDeploymentPreference_withAllAtOnceDeploymentConfiguration_succeeds() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_ALL_AT_ONCE);
        ResourceModelUtils.validateDeploymentPreference(deploymentPreference);
    }

    @Test
    public void testValidateDeploymentPreference_withUnknownDeploymentType_throwsIllegalStateException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType("UnknownDeploymentType");

        final IllegalArgumentException thrownException = assertThrows(
                IllegalArgumentException.class,
                () -> ResourceModelUtils.validateDeploymentPreference(deploymentPreference)
        );

        assertThat(thrownException.getMessage()).isEqualTo("No enum constant com.amazonaws.stepfunctions.cloudformation.statemachinealias.DeploymentType.UnknownDeploymentType");
    }

    @Test
    public void testGetUpdatedLinearDeploymentRoutingConfig_forInitialTrafficShift_returnsInitiallyShiftedConfig() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));

        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        final Set<RoutingConfigurationVersion> expected = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 10),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 90)
        ));

        final Set<RoutingConfigurationVersion> actual = ResourceModelUtils.getUpdatedLinearDeploymentRoutingConfig(
                routingConfig,
                deploymentPreference,
                STATE_MACHINE_VERSION_1_ARN,
                STATE_MACHINE_VERSION_2_ARN
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetUpdatedLinearDeploymentRoutingConfig_forSecondTrafficShift_returnsPartiallyShiftedConfig() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 90),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 10)
        ));

        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        final Set<RoutingConfigurationVersion> expected = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 20),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 80)
        ));

        final Set<RoutingConfigurationVersion> actual = ResourceModelUtils.getUpdatedLinearDeploymentRoutingConfig(
                routingConfig,
                deploymentPreference,
                STATE_MACHINE_VERSION_1_ARN,
                STATE_MACHINE_VERSION_2_ARN
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetUpdatedLinearDeploymentRoutingConfig_forFinalTrafficShift_returnsFullyShiftedConfig() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 10),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 90)
        ));

        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(50);

        final Set<RoutingConfigurationVersion> expected = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 100)
        ));

        final Set<RoutingConfigurationVersion> actual = ResourceModelUtils.getUpdatedLinearDeploymentRoutingConfig(
                routingConfig,
                deploymentPreference,
                STATE_MACHINE_VERSION_1_ARN,
                STATE_MACHINE_VERSION_2_ARN
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetUpdatedCanaryDeploymentRoutingConfig_forInitialTrafficShift_returnsInitiallyShiftedConfig() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));

        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        final Set<RoutingConfigurationVersion> expected = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 10),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 90)
        ));

        final Set<RoutingConfigurationVersion> actual = ResourceModelUtils.getUpdatedCanaryDeploymentRoutingConfig(
                routingConfig,
                deploymentPreference,
                STATE_MACHINE_VERSION_1_ARN,
                STATE_MACHINE_VERSION_2_ARN
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetUpdatedCanaryDeploymentRoutingConfig_forSecondTrafficShift_returnsFullyShiftedConfig() {
        final Set<RoutingConfigurationVersion> currentRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 90),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 10)
        ));

        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setInterval(10);
        deploymentPreference.setPercentage(10);

        final Set<RoutingConfigurationVersion> expected = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 100)
        ));

        final Set<RoutingConfigurationVersion> actual = ResourceModelUtils.getUpdatedCanaryDeploymentRoutingConfig(
                currentRoutingConfig,
                deploymentPreference,
                STATE_MACHINE_VERSION_1_ARN,
                STATE_MACHINE_VERSION_2_ARN
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testIsSimpleUpdate_whenDeploymentPreferencesAreProvided_returnFalse() {
        final ResourceModel model = new ResourceModel();
        model.setDeploymentPreference(new DeploymentPreference());

        final boolean expected = false;
        final boolean actual = ResourceModelUtils.isSimpleUpdate(model);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testIsSimpleUpdate_whenDeploymentPreferenceAreNotProvided_returnTrue() {
        final ResourceModel model = new ResourceModel();

        final boolean expected = true;
        final boolean actual = ResourceModelUtils.isSimpleUpdate(model);

        assertThat(expected).isEqualTo(actual);
    }

    /////////////////
    /// Helpers
    /////////////////

    private void assertDeploymentPreferenceValidationFailure(final DeploymentPreference deploymentPreference, final String errorMessage) {
        final ValidationException expectedException = ResourceHandler.getValidationException(errorMessage);

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> ResourceModelUtils.validateDeploymentPreference(deploymentPreference)
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }
}
