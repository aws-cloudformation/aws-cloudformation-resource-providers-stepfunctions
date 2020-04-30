package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
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

        logger.log("INFO StateMachine ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            DescribeStateMachineRequest describeStateMachineRequest = new DescribeStateMachineRequest();
            describeStateMachineRequest.setStateMachineArn(model.getArn());

            DescribeStateMachineResult describeStateMachineResult = proxy.injectCredentialsAndInvoke(describeStateMachineRequest, sfnClient::describeStateMachine);
            model.setName(describeStateMachineResult.getName());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Reading StateMachine, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}
