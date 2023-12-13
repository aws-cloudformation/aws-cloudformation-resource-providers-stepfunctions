package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineAliasRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;

public class UpdateHandler extends ResourceHandler {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        final CallbackContext context = (callbackContext == null) ? new CallbackContext() : callbackContext;
        logger.log("INFO StateMachineAlias UpdateHandler with clientRequestToken: " + request.getClientRequestToken());
        try {
            final ResourceModel model = request.getDesiredResourceState();
            verifyStateMachineAliasArnIsPresent(model.getArn());

            if (ResourceModelUtils.isSimpleUpdate(model)) {
                logger.log("INFO StateMachineAlias UpdateHandler performing simple update");
                return handleSimpleDeployment(proxy, model);
            } else {
                logger.log(String.format(
                        "INFO StateMachineAlias UpdateHandler performing %s gradual deployment",
                        model.getDeploymentPreference().getType())
                );
                return handleGradualDeployment(proxy,  request, context);
            }
        } catch (Exception e) {
            logger.log("ERROR Updating StateMachineAlias, caused by " + e.toString());
            return handleDefaultError(e);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleSimpleDeployment(final AmazonWebServicesClientProxy proxy,
                                                                                 final ResourceModel model) {
        updateStateMachineAlias(proxy, model);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleGradualDeployment(final AmazonWebServicesClientProxy proxy,
                                                                                  final ResourceHandlerRequest<ResourceModel> request,
                                                                                  final CallbackContext context) {
        final ResourceModel model = request.getDesiredResourceState();

        if (BooleanUtils.isTrue(request.getRollback())) {
            return handleAllAtOnceUpdate(proxy, model);
        }

        final DeploymentPreference deploymentPreference = model.getDeploymentPreference();
        final DeploymentType deploymentType = DeploymentType.valueOf(deploymentPreference.getType());

        if (!context.isTrafficShifting()) {
            ResourceModelUtils.validateDeploymentPreference(deploymentPreference);

            final Set<RoutingConfigurationVersion> currRoutingConfig = ResourceModelUtils.getRoutingConfigFromResourceState(request.getPreviousResourceState());
            TrafficShiftingUtils.performPreflightCheck(currRoutingConfig, deploymentType);
            model.setRoutingConfiguration(currRoutingConfig);

            if (TrafficShiftingUtils.areCurrentAndDesiredTargetVersionArnsTheSame(model)) {
                return handleSimpleDeployment(proxy, model);
            }

            initializeTrafficShiftingContext(context, model);
        } else {
            final Set<RoutingConfigurationVersion> currRoutingConfig = TrafficShiftingUtils.getCurrRoutingConfig(model.getArn(), proxy);
            TrafficShiftingUtils.performInflightCheck(currRoutingConfig, context, deploymentType);
            model.setRoutingConfiguration(currRoutingConfig);
        }

        switch (deploymentType) {
            case ALL_AT_ONCE:
                return handleAllAtOnceUpdate(proxy, model);
            case LINEAR:
                return handleLinearUpdate(proxy, context, model);
            case CANARY:
                return handleCanaryUpdate(proxy, context, model);
            default:
                // This should never happen because the deployment types are enumerated in the resource type schema
                throw new IllegalStateException();
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> abortDeployment(final AmazonWebServicesClientProxy proxy,
                                                                          final CallbackContext context,
                                                                          final ResourceModel model,
                                                                          final Set<String> activeAlarms) {
        model.setRoutingConfiguration(ResourceModelUtils.getSingleVersionRoutingConfig(context.getOriginVersionArn()));
        updateStateMachineAlias(proxy, model);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(context)
                .status(OperationStatus.FAILED)
                .message(String.format(
                        "Aborting deployment. The following CloudWatch alarms are in an 'ALARM' state: %s.",
                        activeAlarms
                ))
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> skipUpdate(final CallbackContext context,
                                                                     final ResourceModel model) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(context)
                .callbackDelaySeconds(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS)
                .status(OperationStatus.IN_PROGRESS)
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleAllAtOnceUpdate(final AmazonWebServicesClientProxy proxy,
                                                                                final ResourceModel model) {
        final String targetVersionArn = model.getDeploymentPreference().getStateMachineVersionArn();
        final Set<RoutingConfigurationVersion> updatedRoutingConfig = ResourceModelUtils.getSingleVersionRoutingConfig(targetVersionArn);
        model.setRoutingConfiguration(updatedRoutingConfig);
        return handleSimpleDeployment(proxy, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleLinearUpdate(final AmazonWebServicesClientProxy proxy,
                                                                             final CallbackContext context,
                                                                             final ResourceModel model) {
        final Set<String> alarmsToMonitor = model.getDeploymentPreference().getAlarms();
        final Set<String> activeAlarms = TrafficShiftingUtils.getActiveAlarms(alarmsToMonitor, proxy);
        if (activeAlarms.size() > 0) {
            return abortDeployment(proxy, context, model, activeAlarms);
        }
        if (!TrafficShiftingUtils.shouldPerformTrafficShift(context.getLastShiftedTime(), model.getDeploymentPreference().getInterval())) {
            return skipUpdate(context, model);
        }

        final Set<RoutingConfigurationVersion> updatedRoutingConfig = ResourceModelUtils.getUpdatedLinearDeploymentRoutingConfig(
                model.getRoutingConfiguration(),
                model.getDeploymentPreference(),
                context.getOriginVersionArn(),
                context.getTargetVersionArn()
        );

        model.setRoutingConfiguration(updatedRoutingConfig);
        updateStateMachineAlias(proxy, model);
        context.setLastShiftedTime(Instant.now());
        model.getRoutingConfiguration().forEach(routingConfig -> {
            if (routingConfig.getStateMachineVersionArn().equals(context.getOriginVersionArn())) {
                context.setOriginVersionWeight(routingConfig.getWeight());
            } else {
                context.setTargetVersionWeight(routingConfig.getWeight());
            }
        });

        return model.getRoutingConfiguration().size() == 1 ?
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build()
                :
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .callbackContext(context)
                        .callbackDelaySeconds(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS)
                        .status(OperationStatus.IN_PROGRESS)
                        .build();
    }


    private ProgressEvent<ResourceModel, CallbackContext> handleCanaryUpdate(final AmazonWebServicesClientProxy proxy,
                                                                             final CallbackContext context,
                                                                             final ResourceModel model) {
        final Set<String> alarmsToMonitor = model.getDeploymentPreference().getAlarms();
        final Set<String> activeAlarms = TrafficShiftingUtils.getActiveAlarms(alarmsToMonitor, proxy);
        if (activeAlarms.size() > 0) {
            return abortDeployment(proxy, context, model, activeAlarms);
        }
        if (!TrafficShiftingUtils.shouldPerformTrafficShift(context.getLastShiftedTime(), model.getDeploymentPreference().getInterval())) {
            return skipUpdate(context, model);
        }

        final Set<RoutingConfigurationVersion> updatedRoutingConfig = ResourceModelUtils.getUpdatedCanaryDeploymentRoutingConfig(
                model.getRoutingConfiguration(),
                model.getDeploymentPreference(),
                context.getOriginVersionArn(),
                context.getTargetVersionArn()
        );

        model.setRoutingConfiguration(updatedRoutingConfig);
        updateStateMachineAlias(proxy, model);
        context.setLastShiftedTime(Instant.now());
        model.getRoutingConfiguration().forEach(routingConfig -> {
            if (routingConfig.getStateMachineVersionArn().equals(context.getOriginVersionArn())) {
                context.setOriginVersionWeight(routingConfig.getWeight());
            } else {
                context.setTargetVersionWeight(routingConfig.getWeight());
            }
        });

        return model.getRoutingConfiguration().size() == 1 ?
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build()
                :
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .callbackContext(context)
                        .callbackDelaySeconds(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS)
                        .status(OperationStatus.IN_PROGRESS)
                        .build();
    }

    private void initializeTrafficShiftingContext(final CallbackContext context,
                                                  final ResourceModel model) {
        final String originVersionArn = TrafficShiftingUtils.getCurrentTargetVersion(model);
        final String targetVersionArn = model.getDeploymentPreference().getStateMachineVersionArn();
        context.setOriginVersionArn(originVersionArn);
        context.setOriginVersionWeight(100);
        context.setTargetVersionArn(targetVersionArn);
        context.setTargetVersionWeight(0);
        context.setTrafficShifting(true);
    }

    private void updateStateMachineAlias(final AmazonWebServicesClientProxy proxy, final ResourceModel model) {
        final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();
        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = Translator.translateToUpdateRequest(model);
        proxy.injectCredentialsAndInvoke(updateStateMachineAliasRequest, sfnClient::updateStateMachineAlias);
    }
}
