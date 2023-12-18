package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ActivityListItem;
import com.amazonaws.services.stepfunctions.model.ListActivitiesRequest;
import com.amazonaws.services.stepfunctions.model.ListActivitiesResult;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> resourceHandlerRequest,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO Activity ListHandler with clientRequestToken: " + resourceHandlerRequest.getClientRequestToken());

        try {
            final AWSStepFunctions sfnClient = ClientBuilder.getClient();
            final List<ResourceModel> models = new ArrayList<>();

            final ListActivitiesRequest listActivitiesRequest = new ListActivitiesRequest();
            listActivitiesRequest.setNextToken(resourceHandlerRequest.getNextToken());

            final ListActivitiesResult listActivitiesResult = proxy.injectCredentialsAndInvoke(
                    listActivitiesRequest,
                    sfnClient::listActivities
            );

            listActivitiesResult.getActivities()
                    .stream()
                    .map(this::buildResourceModelFromActivityListItem)
                    .forEach(models::add);

            final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                    ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(listActivitiesResult.getNextToken())
                            .status(OperationStatus.SUCCESS)
                            .build();

            return progressEvent;
        } catch (final Exception e) {
            logger.log("ERROR Listing Activities, caused by " + e.toString());

            return handleDefaultError(resourceHandlerRequest, e);
        }
    }

    private ResourceModel buildResourceModelFromActivityListItem(final ActivityListItem activityListItem) {
        return ResourceModel.builder()
                .arn(activityListItem.getActivityArn())
                .name(activityListItem.getName())
                .build();
    }
}
