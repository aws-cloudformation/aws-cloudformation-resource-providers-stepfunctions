package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;

public class CreateHandler extends ResourceHandler {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineAlias CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();
        ResourceModelUtils.generateAliasNameIfNotProvided(request, model);

        try {
            final DeploymentPreference deploymentPreference = model.getDeploymentPreference();
            if (deploymentPreference != null) {
                ResourceModelUtils.validateDeploymentPreference(deploymentPreference);

                final String stateMachineVersionArn = deploymentPreference.getStateMachineVersionArn();
                final RoutingConfigurationVersion routingConfigVersion = new RoutingConfigurationVersion(stateMachineVersionArn, 100);
                final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
                routingConfig.add(routingConfigVersion);

                model.setRoutingConfiguration(routingConfig);
            }

            final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();

            if (doesAliasAlreadyExist(sfnClient, proxy, model.getRoutingConfiguration(), model.getName())) {
                throw getStateMachineAliasAlreadyExistException();
            }

            final CreateStateMachineAliasRequest createStateMachineAliasRequest = Translator.translateToCreateRequest(model);
            final CreateStateMachineAliasResult createStateMachineAliasResult = proxy.injectCredentialsAndInvoke(
                    createStateMachineAliasRequest, sfnClient::createStateMachineAlias
            );

            model.setArn(createStateMachineAliasResult.getStateMachineAliasArn());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Creating StateMachineAlias, caused by " + e);
            return handleDefaultError(e);
        }
    }

    private String parseStateMachineArnFromVersionArn(final String stateMachineVersionArn) {
        final String stateMachineArn = stateMachineVersionArn.substring(0, stateMachineVersionArn.lastIndexOf(":"));
        return stateMachineArn;
    }

    private DescribeStateMachineAliasResult describeStateMachineAlias(final AWSStepFunctions sfnClient, final AmazonWebServicesClientProxy proxy,
                                                                      final String stateMachineAliasArn) {
        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest();
        describeStateMachineAliasRequest.withStateMachineAliasArn(stateMachineAliasArn);
        return proxy.injectCredentialsAndInvoke(
                describeStateMachineAliasRequest, sfnClient::describeStateMachineAlias
        );
    }

    private boolean doesAliasAlreadyExist(final AWSStepFunctions sfnClient, final AmazonWebServicesClientProxy proxy,
                                          final String stateMachineArn, final String aliasName) {
        final String stateMachineAliasArn = String.format("%s:%s", stateMachineArn, aliasName);
        try {
            describeStateMachineAlias(sfnClient, proxy, stateMachineAliasArn);
        } catch (final AmazonServiceException e) {
            if (e.getErrorCode().equals(Constants.RESOURCE_NOT_FOUND_ERROR_CODE)) {
                return false;
            }
            else {
                throw e;
            }
        }
        return true;
    }

    // This is a four-step existence check that determines whether the alias already exists.
    // Steps:
    //  1. Extract the state machine ARN from the state machine version ARNs in the routing configuration
    //  2. Create the expected state machine alias ARN using the ^state machine ARN^ and the alias name
    //  3. Describe the state machine alias
    //  4. Return true if the state machine alias already exists
    private boolean doesAliasAlreadyExist(final AWSStepFunctions sfnClient, final AmazonWebServicesClientProxy proxy,
                                          final Set<RoutingConfigurationVersion> routingConfiguration, final String aliasName) {
        return routingConfiguration.stream()
                .map(RoutingConfigurationVersion::getStateMachineVersionArn)
                .map(this::parseStateMachineArnFromVersionArn)
                .anyMatch(stateMachineArn -> doesAliasAlreadyExist(sfnClient, proxy, stateMachineArn, aliasName));
    }
}
