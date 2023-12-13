package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

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

        logger.log("INFO StateMachineVersion ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            verifyStateMachineVersionArnIsPresent(model.getArn());

            final AWSStepFunctions sfnClient = ClientBuilder.getClient();

            final DescribeStateMachineRequest describeStateMachineVersionRequest = new DescribeStateMachineRequest();
            describeStateMachineVersionRequest.setStateMachineArn(model.getArn());

            final DescribeStateMachineResult describeStateMachineVersionResult = proxy.injectCredentialsAndInvoke(
                    describeStateMachineVersionRequest,
                    sfnClient::describeStateMachine
            );

            final ResourceModel updatedModel = new ResourceModel();
            updatedModel.setArn(describeStateMachineVersionResult.getStateMachineArn());
            updatedModel.setStateMachineArn(model.getStateMachineArn());
            updatedModel.setStateMachineRevisionId(describeStateMachineVersionResult.getRevisionId() != null ? describeStateMachineVersionResult.getRevisionId() : Constants.STATE_MACHINE_INITIAL_REVISION_ID);
            updatedModel.setDescription(describeStateMachineVersionResult.getDescription());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (final Exception e) {
            logger.log("ERROR Reading StateMachineVersion, caused by " + e);
            return handleDefaultError(e);
        }
    }
}
