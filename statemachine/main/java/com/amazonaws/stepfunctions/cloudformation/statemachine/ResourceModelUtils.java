package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

/**
 * Encapsulates the logic behind transforming the ResourceModel for the request's desiredResourceState
 */
public class ResourceModelUtils {

    // Auto-generate a state machine name if one is not provided in the template.
    public static void processStateMachineName(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
        if (model.getStateMachineName() == null) {
            final String generatedName = IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(), request.getClientRequestToken(), Constants.STATE_MACHINE_NAME_MAXLEN);

            model.setStateMachineName(generatedName);
        }
    }

    public static void updateModelFromResult(final ResourceModel model, final CreateStateMachineResult result) {
        model.setArn(result.getStateMachineArn());
    }

    public static void updateModelFromResult(final ResourceModel model, final DescribeStateMachineResult result) {
        model.setName(result.getName());
    }

}
