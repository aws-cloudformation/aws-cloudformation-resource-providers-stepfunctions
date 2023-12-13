package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public class ResourceModelUtils {
    public static void generateAliasNameIfNotProvided(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
        if (model.getName() == null) {
            final String generatedName = IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    Constants.STATE_MACHINE_ALIAS_NAME_MAXLEN
            );

            model.setName(generatedName);
        }
    }

    public static boolean isSimpleUpdate(final ResourceModel model) {
        return model.getDeploymentPreference() == null;
    }

    public static Set<RoutingConfigurationVersion> getUpdatedLinearDeploymentRoutingConfig(final Set<RoutingConfigurationVersion> currentRoutingConfig,
                                                                                           final DeploymentPreference deploymentPreference,
                                                                                           final String oldVersionArn,
                                                                                           final String newVersionArn) {
        final boolean isInitialTrafficShift = currentRoutingConfig.size() < 2;
        if (isInitialTrafficShift) {
            // Begin shifting traffic
            return new HashSet<>(Arrays.asList(
                    new RoutingConfigurationVersion(newVersionArn, deploymentPreference.getPercentage()),
                    getUpdatedRoutingConfigurationVersion(oldVersionArn, 100, weight -> weight - deploymentPreference.getPercentage())
            ));
        }

        final int currentTargetVersionWeight = getRoutingConfigVersionForVersionArn(currentRoutingConfig, newVersionArn).getWeight();
        if (Math.min(100, currentTargetVersionWeight + deploymentPreference.getPercentage()) == 100) {
            // Finish shifting traffic
            return new HashSet<>(Collections.singletonList(
                    getUpdatedRoutingConfigurationVersion(newVersionArn, currentTargetVersionWeight, weight -> Math.min(100, weight + deploymentPreference.getPercentage()))
            ));
        }

        final int currentOriginVersionWeight = getRoutingConfigVersionForVersionArn(currentRoutingConfig, oldVersionArn).getWeight();
        // Continue shifting traffic
        return new HashSet<>(Arrays.asList(
                getUpdatedRoutingConfigurationVersion(newVersionArn, currentTargetVersionWeight, weight -> weight + deploymentPreference.getPercentage()),
                getUpdatedRoutingConfigurationVersion(oldVersionArn, currentOriginVersionWeight, weight -> weight - deploymentPreference.getPercentage())
        ));
    }

    public static Set<RoutingConfigurationVersion> getUpdatedCanaryDeploymentRoutingConfig(final Set<RoutingConfigurationVersion> currentRoutingConfig,
                                                                                           final DeploymentPreference deploymentPreference,
                                                                                           final String oldVersionArn,
                                                                                           final String newVersionArn) {
        final boolean isInitialTrafficShift = currentRoutingConfig.size() < 2;
        return isInitialTrafficShift ?
                // Begin shifting traffic
                new HashSet<>(Arrays.asList(
                        new RoutingConfigurationVersion(newVersionArn, deploymentPreference.getPercentage()),
                        getUpdatedRoutingConfigurationVersion(oldVersionArn, 100, weight -> weight - deploymentPreference.getPercentage())
                ))
                :
                // Finish shifting traffic
                new HashSet<>(Collections.singletonList(
                        new RoutingConfigurationVersion(newVersionArn, 100)
                ));
    }

    public static void validateDeploymentPreference(final DeploymentPreference deploymentPreference) {
        switch(DeploymentType.valueOf(deploymentPreference.getType())) {
            case LINEAR:
                validateRequiredDeploymentConfigurationProperties(deploymentPreference);
                validateLinearDeploymentConfiguration(deploymentPreference);
                break;
            case CANARY:
                validateRequiredDeploymentConfigurationProperties(deploymentPreference);
                break;
            case ALL_AT_ONCE:
                return;
            default:
                // This should never happen because the deployment types are enumerated in the resource type schema
                throw new IllegalStateException(String.format(
                        "Unknown deployment type '%s'",
                        deploymentPreference.getType()
                ));
        }
    }

    public static Set<RoutingConfigurationVersion> getSingleVersionRoutingConfig(String stateMachineVersionArn) {
        final RoutingConfigurationVersion routingConfigurationVersion = new RoutingConfigurationVersion(stateMachineVersionArn, 100);
        return new HashSet<>(Collections.singletonList(routingConfigurationVersion));
    }

    public static Set<RoutingConfigurationVersion> getRoutingConfigFromResourceState(final ResourceModel model) {
        return model.getRoutingConfiguration() != null ?
                model.getRoutingConfiguration()
                :
                new HashSet<>(Collections.singletonList(
                        new RoutingConfigurationVersion(model.getDeploymentPreference().getStateMachineVersionArn(), 100)
                ));
    }

    /////////////////
    /// Helpers
    /////////////////

    private static RoutingConfigurationVersion getRoutingConfigVersionForVersionArn(final Set<RoutingConfigurationVersion> routingConfiguration,
                                                                                    final String versionArn) {
        final Optional<RoutingConfigurationVersion> routingConfigurationListItem = routingConfiguration.stream()
                .filter(r -> r.getStateMachineVersionArn().equals(versionArn))
                .findFirst();
        if (!routingConfigurationListItem.isPresent()) {
            throw new IllegalStateException();
        }
        return routingConfigurationListItem.get();
    }

    private static RoutingConfigurationVersion getUpdatedRoutingConfigurationVersion(final String versionArn,
                                                                                     final int currentWeight,
                                                                                     final IntUnaryOperator calculateUpdatedWeight) {
        final int updatedWeight = calculateUpdatedWeight.applyAsInt(currentWeight);
        return new RoutingConfigurationVersion(versionArn, updatedWeight);
    }

    private static void validateRequiredDeploymentConfigurationProperties(final DeploymentPreference deploymentPreference) {
        if (deploymentPreference.getInterval() == null && deploymentPreference.getPercentage() == null) {
            throw ResourceHandler.getValidationException(String.format(
                    "Deployments of type '%s' require an interval and percentage for traffic shifting. " +
                    "Configure the deployment preference with the 'interval' and 'percentage' properties and try again.",
                    deploymentPreference.getType()
            ));
        }
        if (deploymentPreference.getInterval() == null) {
            throw ResourceHandler.getValidationException(String.format(
                    "Deployments of type '%s' require an interval for traffic shifting. " +
                    "Configure the deployment preference with the 'interval' property and try again.",
                    deploymentPreference.getType()
            ));
        }
        if (deploymentPreference.getPercentage() == null) {
            throw ResourceHandler.getValidationException(String.format(
                    "Deployments of type '%s' require a percentage for traffic shifting. " +
                    "Configure the deployment preference with the 'percentage' property and try again.",
                    deploymentPreference.getType()
            ));
        }
    }

    private static void validateLinearDeploymentConfiguration(final DeploymentPreference deploymentPreference) {
        final int interval = deploymentPreference.getInterval();
        final int percentage = deploymentPreference.getPercentage();

        // We round up the division to get the correct number of updates
        // E.g. if `percentage = 33` then there will be 4 updates: 0 -> 33 -> 66 -> 99 -> 100.
        final int estimatedDeploymentTime =  (int) Math.ceil( (double) 100 / percentage) * interval;

        if (estimatedDeploymentTime > Constants.MAX_DEPLOYMENT_TIME_MINUTES) {
            throw ResourceHandler.getValidationException(String.format(
                    "The linear deployment configured is estimated to take %d minutes, which exceeds the maximum allowable deployment time of %d minutes. " +
                    "Configure the deployment preference to use a higher shift percentage or lower time interval and try again.",
                    estimatedDeploymentTime, Constants.MAX_DEPLOYMENT_TIME_MINUTES
            ));
        }
    }
}
