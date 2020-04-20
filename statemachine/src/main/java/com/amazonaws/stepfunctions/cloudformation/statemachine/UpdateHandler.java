package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineRequest;
import com.google.common.collect.Sets;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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

        logger.log("INFO StateMachine UpdateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = AWSStepFunctionsClientBuilder.defaultClient();

            // Fetch S3 definition and apply resource mappings.
            if (model.getDefinitionS3Location() == null && model.getDefinitionString() == null) {
                throw new CfnInvalidRequestException(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
            }

            if (model.getDefinitionS3Location() != null) {
                model.setDefinitionString(fetchS3Definition(model.getDefinitionS3Location(), proxy));
            }

            if (model.getDefinitionSubstitutions() != null) {
                model.setDefinitionString(transformDefinition(model.getDefinitionString(), model.getDefinitionSubstitutions()));
            }

            UpdateStateMachineRequest updateStateMachineRequest = new UpdateStateMachineRequest();
            updateStateMachineRequest.setStateMachineArn(model.getId());
            updateStateMachineRequest.setRoleArn(model.getRoleArn());
            updateStateMachineRequest.setDefinition(model.getDefinitionString());

            if (model.getLoggingConfiguration() != null) {
                updateStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(model.getLoggingConfiguration()));
            }

            proxy.injectCredentialsAndInvoke(updateStateMachineRequest, sfnClient::updateStateMachine);

            updateTags(request, proxy, sfnClient);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Updating StateMachine, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

    private void updateTags(ResourceHandlerRequest<ResourceModel> request, AmazonWebServicesClientProxy proxy, AWSStepFunctions sfnClient) {
        String stateMachineArn = request.getDesiredResourceState().getId();

        List<Tag> previousUserTags = TaggingHelper.listTagsForResource(stateMachineArn, proxy, sfnClient);

        Set<Tag> currentUserTags = new HashSet<>();
        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceState().getTags()));
        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceTags()));

        TaggingHelper.updateTags(stateMachineArn, Sets.newHashSet(previousUserTags), currentUserTags, proxy, sfnClient);
    }

}