package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineRequest;
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

        logger.log("INFO StateMachine DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            DeleteStateMachineRequest deleteStateMachineRequest = new DeleteStateMachineRequest();
            deleteStateMachineRequest.setStateMachineArn(model.getId());

            proxy.injectCredentialsAndInvoke(deleteStateMachineRequest, sfnClient::deleteStateMachine);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Deleting StateMachine, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}