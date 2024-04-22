package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteActivityRequest;
import com.amazonaws.services.stepfunctions.model.DescribeActivityRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.CALLBACK_DELAY_SECONDS_FOR_STABILIZATION;

public class DeleteHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO Activity DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        final CallbackContext context = (callbackContext == null) ? new CallbackContext() : callbackContext;

        if (context.isPropagationDelayDone()) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build();
        }

        try {
            verifyActivityArnIsPresent(model.getArn());

            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            // Validate that the activity exists
            DescribeActivityRequest describeActivityRequest = new DescribeActivityRequest();
            describeActivityRequest.setActivityArn(model.getArn());
            proxy.injectCredentialsAndInvoke(describeActivityRequest, sfnClient::describeActivity);

            DeleteActivityRequest deleteActivityRequest = new DeleteActivityRequest();
            deleteActivityRequest.setActivityArn(model.getArn());

            proxy.injectCredentialsAndInvoke(deleteActivityRequest, sfnClient::deleteActivity);

            context.setPropagationDelayDone(true);
            return ProgressEvent.defaultInProgressHandler(context, CALLBACK_DELAY_SECONDS_FOR_STABILIZATION, model);
        } catch (Exception e) {
            logger.log("ERROR Deleting Activity, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}
