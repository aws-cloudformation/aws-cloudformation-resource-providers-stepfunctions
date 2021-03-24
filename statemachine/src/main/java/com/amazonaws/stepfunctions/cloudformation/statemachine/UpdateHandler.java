package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.processDefinition;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.validateDefinitionCount;

public class UpdateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine UpdateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.UPDATE);
        metricsRecorder.setMetricsFromResourceModel(model);

        try {
            AWSStepFunctions sfnClient = AWSStepFunctionsClientBuilder.defaultClient();

            validateDefinitionCount(model);
            processDefinition(proxy, model, metricsRecorder);

            UpdateStateMachineRequest updateStateMachineRequest = buildUpdateStateMachineRequestFromModel(model);

            proxy.injectCredentialsAndInvoke(updateStateMachineRequest, sfnClient::updateStateMachine);
            updateTags(request, proxy, sfnClient);

            metricsRecorder.setOperationSuccessful(true);

            ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (Exception e) {
            logger.log("ERROR Updating StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

    private UpdateStateMachineRequest buildUpdateStateMachineRequestFromModel(ResourceModel model) {
        UpdateStateMachineRequest updateStateMachineRequest = new UpdateStateMachineRequest();
        updateStateMachineRequest.setStateMachineArn(model.getArn());
        updateStateMachineRequest.setRoleArn(model.getRoleArn());
        updateStateMachineRequest.setDefinition(model.getDefinitionString());

        if (model.getLoggingConfiguration() != null) {
            updateStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(model.getLoggingConfiguration()));
        }

        if (model.getTracingConfiguration() != null) {
            updateStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(model.getTracingConfiguration()));
        }

        return updateStateMachineRequest;
    }

    private void updateTags(ResourceHandlerRequest<ResourceModel> request, AmazonWebServicesClientProxy proxy, AWSStepFunctions sfnClient) {
        String stateMachineArn = request.getDesiredResourceState().getArn();

        Set<Tag> currentUserTags = new HashSet<>();
        Set<Tag> previousTags = new HashSet<>();

        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceState().getTags()));
        currentUserTags.addAll(TaggingHelper.transformTags(request.getDesiredResourceTags()));
        previousTags.addAll(TaggingHelper.transformTags(request.getPreviousResourceState().getTags()));
        previousTags.addAll(TaggingHelper.transformTags(request.getPreviousResourceTags()));

        TaggingHelper.updateTags(stateMachineArn, previousTags, currentUserTags, proxy, sfnClient);
    }

}
