package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.cloudwatch.model.CompositeAlarm;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.StateValue;
import com.amazonaws.services.stepfunctions.model.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TrafficShiftingUtilsTest extends HandlerTestBase {
    private final String STATE_MACHINE_VERSION_1_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:MyStateMachine:1";
    private final String STATE_MACHINE_VERSION_2_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:MyStateMachine:2";
    private final String STATE_MACHINE_VERSION_3_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:MyStateMachine:3";
    private final Collection<String> ALARM_TYPES = Arrays.asList("CompositeAlarm", "MetricAlarm");
    private final Set<String> alarms = new HashSet<>(Arrays.asList("alarm1", "alarm2", "alarm3"));

    @Test
    public void testAreCloudWatchAlarmsOK_whenAllAlarmsAreOK_thenReturnsTrue() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withMetricAlarms(
                new MetricAlarm().withStateValue(StateValue.OK),
                new MetricAlarm().withStateValue(StateValue.OK),
                new MetricAlarm().withStateValue(StateValue.OK)
        );
        describeAlarmsResult.withCompositeAlarms(
                new CompositeAlarm().withStateValue(StateValue.OK),
                new CompositeAlarm().withStateValue(StateValue.OK),
                new CompositeAlarm().withStateValue(StateValue.OK)
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>();
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCloudWatchAlarmsOK_whenCloudWatchReturnsEmptyList_thenReturnsTrue() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withMetricAlarms();

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>();
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCloudWatchAlarmsOK_whenAllMetricAlarmsAreInAlarm_thenReturnsFalse() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withMetricAlarms(
                new MetricAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm1"),
                new MetricAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm2"),
                new MetricAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm3")
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>(Arrays.asList("alarm1", "alarm2", "alarm3"));
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCloudWatchAlarmsOK_whenAllCompositeAlarmsAreInAlarm_thenReturnsFalse() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withCompositeAlarms(
                new CompositeAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm1"),
                new CompositeAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm2"),
                new CompositeAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm3")
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>(Arrays.asList("alarm1", "alarm2", "alarm3"));
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);

        assertThat(expected).isEqualTo(actual);
    }


    @Test
    public void testAreCloudWatchAlarmsOK_whenOneMetricAlarmIsInAlarm_thenReturnsFalse() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withMetricAlarms(
                new MetricAlarm().withStateValue(StateValue.OK).withAlarmName("alarm1"),
                new MetricAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm2"),
                new MetricAlarm().withStateValue(StateValue.OK).withAlarmName("alarm3")
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>(Collections.singletonList("alarm2"));
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCloudWatchAlarmsOK_whenOneCompositeAlarmIsInAlarm_thenReturnsFalse() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withCompositeAlarms(
                new CompositeAlarm().withStateValue(StateValue.OK).withAlarmName("alarm1"),
                new CompositeAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm2"),
                new CompositeAlarm().withStateValue(StateValue.OK).withAlarmName("alarm3")
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>(Collections.singletonList("alarm2"));
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCloudWatchAlarmsOK_whenOneCompositeAlarmAndOneMetricAlarmAreInAlarm_thenReturnsFalse() {
        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
        describeAlarmsResult.withMetricAlarms(
                new MetricAlarm().withStateValue(StateValue.OK).withAlarmName("alarm1"),
                new MetricAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm2")
        );
        describeAlarmsResult.withCompositeAlarms(
                new CompositeAlarm().withStateValue(StateValue.ALARM).withAlarmName("alarm3")
        );

        awsRequest = new DescribeAlarmsRequest().withAlarmNames(alarms).withAlarmTypes(ALARM_TYPES);
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeAlarmsResult);

        final Set<String> expected = new HashSet<>(Arrays.asList("alarm2", "alarm3"));
        final Set<String> actual = TrafficShiftingUtils.getActiveAlarms(alarms, proxy);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testPerformPreflightCheck_withSingleVersionRoutingConfiguration_thenDoesNothing() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        );

        TrafficShiftingUtils.performPreflightCheck(routingConfig, DeploymentType.ALL_AT_ONCE);
    }

    @Test
    public void testPerformPreflightCheck_withDoubleVersionRoutingConfiguration_andAllAtOnceDeploymentType_thenDoesNothing() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        );
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        );

        TrafficShiftingUtils.performPreflightCheck(routingConfig, DeploymentType.ALL_AT_ONCE);
    }

    @Test
    public void testPerformPreflightCheck_withDoubleVersionRoutingConfiguration_andLinearDeploymentType_thenThrowsValidationException() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        );
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        );

        final ValidationException expectedException = ResourceHandler.getValidationException(
                "Failed to start deployment of type 'LINEAR', invalid initial state detected. " +
                        "Expected the alias to be routing 100% of traffic towards one version, but it is currently routing traffic towards two. " +
                        "Update the alias to route 100% of traffic towards one version and try the deployment again."
        );

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> TrafficShiftingUtils.performPreflightCheck(
                        routingConfig,
                        DeploymentType.LINEAR
                )
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }

    @Test
    public void testPerformPreflightCheck_withDoubleVersionRoutingConfiguration_andCanaryDeploymentType_thenThrowsValidationException() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        );
        routingConfig.add(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        );

        final ValidationException expectedException = ResourceHandler.getValidationException(
                "Failed to start deployment of type 'CANARY', invalid initial state detected. " +
                        "Expected the alias to be routing 100% of traffic towards one version, but it is currently routing traffic towards two. " +
                        "Update the alias to route 100% of traffic towards one version and try the deployment again."
        );

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> TrafficShiftingUtils.performPreflightCheck(
                        routingConfig,
                        DeploymentType.CANARY
                )
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }

    @Test
    public void testPerformInflightCheck_whenRoutingConfigMatchesContext_thenDoesNothing() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        ));

        final CallbackContext context = CallbackContext.builder()
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(50)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(50)
                .build();

        TrafficShiftingUtils.performInflightCheck(
                routingConfig,
                context,
                DeploymentType.LINEAR
        );
    }

    @Test
    public void testPerformInflightCheck_whenRoutingConfigHasSingleVersion_thenThrowsValidationException() {
        final Set<RoutingConfigurationVersion> actualRoutingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));

        final Set<RoutingConfigurationVersion> expectedRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        ));

        final CallbackContext context = CallbackContext.builder()
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(50)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(50)
                .build();

        final ValidationException expectedException = ResourceHandler.getValidationException(
                TrafficShiftingUtils.getErrorMessageForRoutingConfigDrift(
                        DeploymentType.LINEAR,
                        expectedRoutingConfig,
                        actualRoutingConfig
                )
        );

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> TrafficShiftingUtils.performInflightCheck(
                        actualRoutingConfig,
                        context,
                        DeploymentType.LINEAR
                )
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }

    @Test
    public void testPerformInflightCheck_whenRoutingConfigHasDifferentVersionArns_thenThrowsValidationException() {
        final Set<RoutingConfigurationVersion> actualRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_3_ARN, 50)
        ));

        final Set<RoutingConfigurationVersion> expectedRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        ));


        final CallbackContext context = CallbackContext.builder()
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(50)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(50)
                .build();

        final ValidationException expectedException = ResourceHandler.getValidationException(
                TrafficShiftingUtils.getErrorMessageForRoutingConfigDrift(
                        DeploymentType.LINEAR,
                        expectedRoutingConfig,
                        actualRoutingConfig
                )
        );

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> TrafficShiftingUtils.performInflightCheck(
                        actualRoutingConfig,
                        context,
                        DeploymentType.LINEAR
                )
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }

    @Test
    public void testPerformInflightCheck_whenRoutingConfigHasDifferentWeights_thenThrowsValidationException() {
        final Set<RoutingConfigurationVersion> actualRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 70),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 30)
        ));

        final Set<RoutingConfigurationVersion> expectedRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50)
        ));

        final CallbackContext context = CallbackContext.builder()
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(50)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(50)
                .build();

        final ValidationException expectedException = ResourceHandler.getValidationException(
                TrafficShiftingUtils.getErrorMessageForRoutingConfigDrift(
                        DeploymentType.LINEAR,
                        expectedRoutingConfig,
                        actualRoutingConfig
                )
        );

        final ValidationException actualException = assertThrows(
                ValidationException.class,
                () -> TrafficShiftingUtils.performInflightCheck(
                        actualRoutingConfig,
                        context,
                        DeploymentType.LINEAR
                )
        );

        assertThat(expectedException.getMessage()).isEqualTo(actualException.getMessage());
    }

    @Test
    public void testAreCurrentAndDesiredTargetVersionArnsTheSame_whenVersionsAreNotTheSame_returnsFalse() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));

        final DeploymentPreference deploymentPreference = DeploymentPreference.builder()
                .stateMachineVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .build();

        final ResourceModel resourceModel = ResourceModel.builder()
                .routingConfiguration(routingConfig)
                .deploymentPreference(deploymentPreference)
                .build();

        final boolean expected = false;
        final boolean actual = TrafficShiftingUtils.areCurrentAndDesiredTargetVersionArnsTheSame(resourceModel);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testAreCurrentAndDesiredTargetVersionArnsTheSame_whenVersionsAreTheSame_returnsTrue() {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));

        final DeploymentPreference deploymentPreference = DeploymentPreference.builder()
                .stateMachineVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .build();

        final ResourceModel resourceModel = ResourceModel.builder()
                .routingConfiguration(routingConfig)
                .deploymentPreference(deploymentPreference)
                .build();

        final boolean expected = true;
        final boolean actual = TrafficShiftingUtils.areCurrentAndDesiredTargetVersionArnsTheSame(resourceModel);

        assertThat(expected).isEqualTo(actual);
    }
}
