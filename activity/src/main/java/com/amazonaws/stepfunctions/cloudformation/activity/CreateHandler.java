package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateActivityRequest;
import com.amazonaws.services.stepfunctions.model.CreateActivityResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class CreateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        logger.log("INFO Activity CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            List<Tag> tags = TaggingHelper.consolidateTags(request);

            CreateActivityRequest createActivityRequest = new CreateActivityRequest()
                    .withName(model.getName())
                    .withTags(tags);

            CreateActivityResult createActivityResult = proxy.injectCredentialsAndInvoke(createActivityRequest, sfnClient::createActivity);
            model.setArn(createActivityResult.getActivityArn());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Creating Activity, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}
