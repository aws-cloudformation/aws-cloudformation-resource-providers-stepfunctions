package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsResult;
import com.amazonaws.services.stepfunctions.model.PublishStateMachineVersionRequest;
import com.amazonaws.services.stepfunctions.model.PublishStateMachineVersionResult;
import com.amazonaws.services.stepfunctions.model.StateMachineVersionListItem;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Optional;

public class CreateHandler extends ResourceHandler {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineVersion CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            final AWSStepFunctions sfnClient = ClientBuilder.getClient();

            // Existence check
            if (doesVersionAlreadyExist(sfnClient, proxy, model.getStateMachineArn())) {
                throw getStateMachineAlreadyExistException();
            }

            final PublishStateMachineVersionRequest publishStateMachineVersionRequest = new PublishStateMachineVersionRequest();
            publishStateMachineVersionRequest.setStateMachineArn(model.getStateMachineArn());
            publishStateMachineVersionRequest.setRevisionId(model.getStateMachineRevisionId());
            publishStateMachineVersionRequest.setDescription(model.getDescription());

            final PublishStateMachineVersionResult publishStateMachineVersionResult = proxy.injectCredentialsAndInvoke(
                    publishStateMachineVersionRequest, sfnClient::publishStateMachineVersion
            );

            model.setArn(publishStateMachineVersionResult.getStateMachineVersionArn());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Creating StateMachineVersion, caused by " + e);
            return handleDefaultError(e);
        }
    }

    private ListStateMachineVersionsResult listLatestVersion(final AWSStepFunctions sfnClient, final AmazonWebServicesClientProxy proxy,
                                                             final String stateMachineArn) {
        final ListStateMachineVersionsRequest listStateMachineVersionsRequest = new ListStateMachineVersionsRequest();
        listStateMachineVersionsRequest.setStateMachineArn(stateMachineArn);
        listStateMachineVersionsRequest.setMaxResults(1); // Limit result to the latest version
        return proxy.injectCredentialsAndInvoke(listStateMachineVersionsRequest, sfnClient::listStateMachineVersions);
    }

    private Optional<StateMachineVersionListItem> extractVersionListItem(final ListStateMachineVersionsResult listStateMachineVersionsResult) {
        final List<StateMachineVersionListItem> stateMachineVersionListItems = listStateMachineVersionsResult.getStateMachineVersions();
        if (stateMachineVersionListItems.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stateMachineVersionListItems.get(0));
    }

    private Optional<String> getLatestVersionArn(final AWSStepFunctions sfnClient,  final AmazonWebServicesClientProxy proxy,
                                                 final String stateMachineArn) {
        final ListStateMachineVersionsResult listStateMachineVersionsResult = listLatestVersion(sfnClient, proxy, stateMachineArn);
        final Optional<StateMachineVersionListItem> stateMachineVersionListItem = extractVersionListItem(listStateMachineVersionsResult);
        if (stateMachineVersionListItem.isPresent()) {
            final String stateMachineVersionArn = stateMachineVersionListItem.get().getStateMachineVersionArn();
            return Optional.of(stateMachineVersionArn);
        }
        return Optional.empty();
    }

    private DescribeStateMachineResult describeStateMachine(final AWSStepFunctions sfnClient,  final AmazonWebServicesClientProxy proxy,
                                                            final String stateMachineArn) {
        final DescribeStateMachineRequest describeStateMachineVersionRequest = new DescribeStateMachineRequest();
        describeStateMachineVersionRequest.setStateMachineArn(stateMachineArn);
        return proxy.injectCredentialsAndInvoke(describeStateMachineVersionRequest, sfnClient::describeStateMachine);
    }

    private String getStateMachineRevision(final AWSStepFunctions sfnClient,  final AmazonWebServicesClientProxy proxy,
                                           final String stateMachineArn) {
        final DescribeStateMachineResult describeStateMachineResult = describeStateMachine(sfnClient, proxy, stateMachineArn);
        return (describeStateMachineResult.getRevisionId() != null) ? describeStateMachineResult.getRevisionId() : Constants.STATE_MACHINE_INITIAL_REVISION_ID;
    }

    private boolean isCurrentRevisionPinnedByVersion(final AWSStepFunctions sfnClient, final AmazonWebServicesClientProxy proxy,
                                                     final String stateMachineArn, final String latestStateMachineVersionArn) {
        final String currentRevision = getStateMachineRevision(sfnClient, proxy, stateMachineArn);
        final String latestPublishedRevision = getStateMachineRevision(sfnClient, proxy, latestStateMachineVersionArn);
        return currentRevision.equals(latestPublishedRevision);
    }

    // This is a four-step existence check that determines whether the current state machine revision is pinned by a version.
    // Steps:
    //  1. List the state machine's versions to get the latest version
    //  2. Describe the latest version to get its revision
    //  3. Describe the state machine to get its current revision
    //  4. Return true if the latest version's revision is the same as the state machine's current revision
    private boolean doesVersionAlreadyExist(final AWSStepFunctions sfnClient,  final AmazonWebServicesClientProxy proxy, final String stateMachineArn) {
        final Optional<String> latestVersionArn = getLatestVersionArn(sfnClient, proxy, stateMachineArn);
        return latestVersionArn.filter(stateMachineVersionArn -> isCurrentRevisionPinnedByVersion(sfnClient, proxy, stateMachineArn, stateMachineVersionArn)).isPresent();
    }
}
