package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsResult;
import com.amazonaws.services.stepfunctions.model.StateMachineVersionListItem;

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
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineVersion ListHandler with clientRequestToken: " + request.getClientRequestToken());

        final List<ResourceModel> models = new ArrayList<>();

        try {
            final AWSStepFunctions sfnClient = ClientBuilder.getClient();

            final ListStateMachineVersionsRequest listStateMachineVersionsRequest = new ListStateMachineVersionsRequest();
            listStateMachineVersionsRequest.setStateMachineArn(request.getDesiredResourceState().getStateMachineArn());
            listStateMachineVersionsRequest.setNextToken(request.getNextToken());

            final ListStateMachineVersionsResult listStateMachineVersionsResult = proxy.injectCredentialsAndInvoke(
                    listStateMachineVersionsRequest,
                    sfnClient::listStateMachineVersions
            );

            listStateMachineVersionsResult.getStateMachineVersions()
                    .stream()
                    .map(this::buildResourceModelFromStateMachineVersionListItem)
                    .forEach(models::add);

            final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                    ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(listStateMachineVersionsResult.getNextToken())
                            .status(OperationStatus.SUCCESS)
                            .build();

            return progressEvent;
        } catch (final Exception e) {
            logger.log("ERROR Listing StateMachineVersions, caused by " + e);

            return handleDefaultError(e);
        }
    }

    private ResourceModel buildResourceModelFromStateMachineVersionListItem(final StateMachineVersionListItem stateMachineVersionListItem) {
        return ResourceModel.builder()
                .arn(stateMachineVersionListItem.getStateMachineVersionArn())
                .build();
    }
}
