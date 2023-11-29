package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.RoutingConfigurationListItem;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrafficShiftingUtils {
    public static void performPreflightCheck(final Set<RoutingConfigurationVersion> currRoutingConfig,
                                             final DeploymentType deploymentType) {
        if (!deploymentType.equals(DeploymentType.ALL_AT_ONCE) && currRoutingConfig.size() > 1) {
            throw ResourceHandler.getValidationException(String.format(
                    "Failed to start deployment of type '%s', invalid initial state detected. " +
                    "Expected the alias to be routing 100%% of traffic towards one version, but it is currently routing traffic towards two. " +
                    "Update the alias to route 100%% of traffic towards one version and try the deployment again.",
                    deploymentType
            ));
        }
    }

    public static void performInflightCheck(final Set<RoutingConfigurationVersion> actualRoutingConfig,
                                            final CallbackContext context,
                                            final DeploymentType deploymentType) {
        final Set<RoutingConfigurationVersion> expectedRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(context.getTargetVersionArn(), context.getTargetVersionWeight()),
                new RoutingConfigurationVersion(context.getOriginVersionArn(), context.getOriginVersionWeight())
        ));

        final boolean actualAndExpectedRoutingConfigsAreTheSame = actualRoutingConfig.equals(expectedRoutingConfig);

        if (!actualAndExpectedRoutingConfigsAreTheSame) {
            throw ResourceHandler.getValidationException(getErrorMessageForRoutingConfigDrift(
                    deploymentType, expectedRoutingConfig, actualRoutingConfig
            ));
        }
    }

    public static String getCurrentTargetVersion(final ResourceModel model) {
        return new ArrayList<>(model.getRoutingConfiguration()).get(0).getStateMachineVersionArn();
    }

    public static Set<RoutingConfigurationVersion> getCurrRoutingConfig(final String aliasArn, final AmazonWebServicesClientProxy proxy) {
        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest();
        describeStateMachineAliasRequest.withStateMachineAliasArn(aliasArn);

        final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();
        final DescribeStateMachineAliasResult describeStateMachineAliasResult = proxy.injectCredentialsAndInvoke(
                describeStateMachineAliasRequest, sfnClient::describeStateMachineAlias
        );

        final List<RoutingConfigurationListItem> currRoutingConfigSdk = describeStateMachineAliasResult.getRoutingConfiguration();
        return Translator.translateToCfnRoutingConfiguration(currRoutingConfigSdk);
    }

    public static Set<String> getActiveAlarms(final Set<String> alarms, final AmazonWebServicesClientProxy proxy) {
        if (alarms == null) {
            return new HashSet<>();
        }

        final DescribeAlarmsRequest describeAlarmsRequest = new DescribeAlarmsRequest();
        describeAlarmsRequest.setAlarmNames(alarms);

        final AmazonCloudWatch cwClient = ClientBuilder.getCwClient();
        final DescribeAlarmsResult describeAlarmsResult = proxy.injectCredentialsAndInvoke(
                describeAlarmsRequest, (Function<DescribeAlarmsRequest, DescribeAlarmsResult>) cwClient::describeAlarms
        );

        return describeAlarmsResult.getMetricAlarms().stream()
                .filter(alarm -> alarm.getStateValue().equals(StateValue.ALARM.toString()))
                .map(MetricAlarm::getAlarmName)
                .collect(Collectors.toSet());
    }

    public static boolean shouldPerformTrafficShift(final Instant lastShifted, final int shiftIntervalMinutes) {
        if (lastShifted == null) {
            return true;
        }
        final Instant nextShiftTime = lastShifted.plusSeconds(60L * shiftIntervalMinutes);
        final Instant now = Instant.now();
        return now.equals(nextShiftTime) || now.isAfter(nextShiftTime);
    }

    public static boolean areCurrentAndDesiredTargetVersionArnsTheSame(final ResourceModel resourceModel) {
        return resourceModel.getRoutingConfiguration().stream()
                .map(RoutingConfigurationVersion::getStateMachineVersionArn)
                .allMatch(r -> r.equals(resourceModel.getDeploymentPreference().getStateMachineVersionArn()));
    }


    /////////////////
    /// Helpers
    /////////////////

    protected static String getErrorMessageForRoutingConfigDrift(final DeploymentType deploymentType,
                                                                 final Set<RoutingConfigurationVersion> expectedRoutingConfig,
                                                                 final Set<RoutingConfigurationVersion> actualRoutingConfig) {
        return String.format(
                "Failed to continue deployment of type '%s', drift detected. " +
                "Expected routing configuration: '%s', actual routing configuration: '%s'. " +
                "Ensure that UpdateStateMachineAlias is not being invoked elsewhere and try the deployment again.",
                deploymentType, expectedRoutingConfig, actualRoutingConfig
        );
    }
}
