package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineAlias DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            verifyStateMachineAliasArnIsPresent(model.getArn());

            final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();

            // Existence check
            final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = Translator.translateToReadRequest(model);
            proxy.injectCredentialsAndInvoke(describeStateMachineAliasRequest, sfnClient::describeStateMachineAlias);

            // Delete alias
            final DeleteStateMachineAliasRequest deleteStateMachineAliasRequest = Translator.translateToDeleteRequest(model);
            proxy.injectCredentialsAndInvoke(deleteStateMachineAliasRequest, sfnClient::deleteStateMachineAlias);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Deleting StateMachineAlias, caused by " + e);

            return handleDefaultError(e);
        }
    }
}
