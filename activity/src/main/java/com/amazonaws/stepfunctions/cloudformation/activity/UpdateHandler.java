package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.google.common.collect.Sets;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        logger.log("INFO Activity UpdateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = AWSStepFunctionsClientBuilder.defaultClient();

            updateTags(request, proxy, sfnClient);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Updating Activity, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

    private void updateTags(ResourceHandlerRequest<ResourceModel> request, AmazonWebServicesClientProxy proxy, AWSStepFunctions sfnClient) {
        String activityArn = request.getDesiredResourceState().getId();

        List<Tag> previousUserTags = TaggingHelper.listTagsForResource(activityArn, proxy, sfnClient);

        Set<Tag> currentUserTags = new HashSet<>();
        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceState().getTags()));
        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceTags()));

        TaggingHelper.updateTags(activityArn, Sets.newHashSet(previousUserTags), currentUserTags, proxy, sfnClient);
    }

}