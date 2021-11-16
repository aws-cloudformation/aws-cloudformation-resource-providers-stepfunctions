package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ReadHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.READ);

        try {
            verifyStateMachineArnIsPresent(model.getArn());

            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            DescribeStateMachineRequest describeStateMachineRequest = buildDescribeStateMachineRequestFromModel(model);
            DescribeStateMachineResult describeStateMachineResult = proxy.injectCredentialsAndInvoke(
                    describeStateMachineRequest, sfnClient::describeStateMachine);

            List<Tag> stateMachineTags = TaggingHelper.listTagsForResource(model.getArn(), proxy, sfnClient);

            ResourceModel updatedModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(
                    describeStateMachineResult, stateMachineTags);

            ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (Exception e) {
            logger.log("ERROR Reading StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

    private DescribeStateMachineRequest buildDescribeStateMachineRequestFromModel(ResourceModel model) {
        DescribeStateMachineRequest describeStateMachineRequest = new DescribeStateMachineRequest();
        describeStateMachineRequest.setStateMachineArn(model.getArn());

        return describeStateMachineRequest;
    }

}
