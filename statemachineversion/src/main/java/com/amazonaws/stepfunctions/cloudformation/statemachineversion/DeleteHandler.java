package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineVersionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
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

        logger.log("INFO StateMachineVersion DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            verifyStateMachineVersionArnIsPresent(model.getArn());

            final AWSStepFunctions sfnClient = ClientBuilder.getClient();

            // Existence check
            final DescribeStateMachineRequest describeStateMachineVersionRequest = new DescribeStateMachineRequest();
            describeStateMachineVersionRequest.setStateMachineArn(model.getArn());
            proxy.injectCredentialsAndInvoke(describeStateMachineVersionRequest, sfnClient::describeStateMachine);

            // Delete version
            final DeleteStateMachineVersionRequest deleteStateMachineVersionRequest = new DeleteStateMachineVersionRequest();
            deleteStateMachineVersionRequest.setStateMachineVersionArn(model.getArn());
            proxy.injectCredentialsAndInvoke(deleteStateMachineVersionRequest, sfnClient::deleteStateMachineVersion);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Deleting StateMachineVersion, caused by " + e);

            return handleDefaultError(e);
        }
    }
}
