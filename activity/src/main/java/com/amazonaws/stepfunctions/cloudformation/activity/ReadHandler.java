package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeActivityRequest;
import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
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

        logger.log("INFO Activity ReadHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            verifyActivityArnIsPresent(model.getArn());

            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            DescribeActivityRequest describeActivityRequest = new DescribeActivityRequest();
            describeActivityRequest.setActivityArn(model.getArn());
            DescribeActivityResult describeActivityResult = proxy.injectCredentialsAndInvoke(describeActivityRequest, sfnClient::describeActivity);

            List<Tag> activityTags = TaggingHelper.listTagsForResource(model.getArn(), proxy, sfnClient);

            ResourceModel updatedModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(describeActivityResult, activityTags);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Reading Activity, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}
