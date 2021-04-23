package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.processDefinition;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.validateDefinitionCount;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModelUtils.processStateMachineName;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModelUtils.updateModelFromResult;

public class CreateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setMetricsFromResourceModel(model);

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            List<Tag> tags = TaggingHelper.consolidateTags(request);

            processStateMachineName(request, model);
            validateDefinitionCount(model);
            processDefinition(proxy, model, metricsRecorder);

            CreateStateMachineRequest createStateMachineRequest = buildCreateStateMachineRequestFromModel(model, tags);

            CreateStateMachineResult createStateMachineResult = proxy.injectCredentialsAndInvoke(createStateMachineRequest, sfnClient::createStateMachine);

            updateModelFromResult(model, createStateMachineResult);
            // The model's name is only required if the handler operation is successful.
            model.setName(model.getStateMachineName());

            ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (Exception e) {
            logger.log("ERROR Creating StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

    private CreateStateMachineRequest buildCreateStateMachineRequestFromModel(ResourceModel model, List<Tag> tags) {
        CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
        createStateMachineRequest.setRoleArn(model.getRoleArn());
        createStateMachineRequest.setName(model.getStateMachineName());
        createStateMachineRequest.setTags(tags);
        createStateMachineRequest.setDefinition(model.getDefinitionString());
        createStateMachineRequest.setType(model.getStateMachineType());

        if (model.getLoggingConfiguration() != null) {
            createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(model.getLoggingConfiguration()));
        }

        if (model.getTracingConfiguration() != null) {
            createStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(model.getTracingConfiguration()));
        }

        return createStateMachineRequest;
    }

}