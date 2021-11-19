package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
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

    private static DescribeStateMachineRequest buildDescribeStateMachineRequestFromModel(final ResourceModel model) {
        final DescribeStateMachineRequest describeStateMachineRequest = new DescribeStateMachineRequest();
        describeStateMachineRequest.setStateMachineArn(model.getArn());

        return describeStateMachineRequest;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        final MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.READ);

        try {
            verifyStateMachineArnIsPresent(model.getArn());

            final AWSStepFunctions sfnClient = ClientBuilder.getClient();

            final DescribeStateMachineRequest describeStateMachineRequest =
                    buildDescribeStateMachineRequestFromModel(model);
            final DescribeStateMachineResult describeStateMachineResult =
                    proxy.injectCredentialsAndInvoke(describeStateMachineRequest, sfnClient::describeStateMachine);

            List<Tag> stateMachineTags = null;
            try {
                stateMachineTags = TaggingHelper.listTagsForResource(model.getArn(), proxy, sfnClient);
            } catch (final AmazonServiceException e) {
                // To provide backwards compatibility, do not fail the request if ListTagsForResource
                // permissions are not present
                if (!Constants.ACCESS_DENIED_ERROR_CODE.equals(e.getErrorCode())) {
                    throw e;
                }

                logger.log("INFO ListTagsForResource permission not present, excluding tags from resource model");
            }

            final ResourceModel updatedModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(
                    describeStateMachineResult, stateMachineTags);

            final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                    ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(updatedModel)
                            .status(OperationStatus.SUCCESS)
                            .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (final Exception e) {
            logger.log("ERROR Reading StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

}
