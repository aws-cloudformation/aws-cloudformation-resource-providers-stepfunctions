package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends ResourceHandler {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineAlias ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            verifyStateMachineAliasArnIsPresent(model.getArn());

            final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();

            final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = Translator.translateToReadRequest(model);

            final DescribeStateMachineAliasResult describeStateMachineAliasResult = proxy.injectCredentialsAndInvoke(
                    describeStateMachineAliasRequest, sfnClient::describeStateMachineAlias
            );

            final ResourceModel updatedModel = Translator.translateFromReadResponse(describeStateMachineAliasResult);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Reading StateMachineAlias, caused by " + e);
            return handleDefaultError(e);
        }
    }
}
