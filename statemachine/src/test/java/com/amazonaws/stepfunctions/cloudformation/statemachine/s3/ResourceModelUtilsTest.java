package com.amazonaws.stepfunctions.cloudformation.statemachine.s3;

import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.stepfunctions.cloudformation.statemachine.HandlerTestBase;
import com.amazonaws.stepfunctions.cloudformation.statemachine.LoggingConfiguration;
import com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModel;
import com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModelUtils;
import com.amazonaws.stepfunctions.cloudformation.statemachine.TagsEntry;
import com.amazonaws.stepfunctions.cloudformation.statemachine.TracingConfiguration;
import com.amazonaws.stepfunctions.cloudformation.statemachine.Translator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceModelUtilsTest extends HandlerTestBase {

    @Test
    public void testGetUpdatedResourceModelFromReadResults_returnsUpdatedModelWithAllProperties() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);
        describeStateMachineResult.setName(STATE_MACHINE_NAME);
        describeStateMachineResult.setDefinition(DEFINITION);
        describeStateMachineResult.setRoleArn(ROLE_ARN);
        describeStateMachineResult.setType(EXPRESS_TYPE);

        LoggingConfiguration loggingConfiguration = createLoggingConfiguration();
        describeStateMachineResult.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));

        TracingConfiguration tracingConfiguration = createTracingConfiguration(true);
        describeStateMachineResult.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));

        List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        ResourceModel outputModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(
                describeStateMachineResult, stateMachineTags);

        List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        ResourceModel expectedModel = new ResourceModel(
                STATE_MACHINE_ARN,
                STATE_MACHINE_NAME,
                DEFINITION,
                ROLE_ARN,
                STATE_MACHINE_NAME,
                EXPRESS_TYPE,
                loggingConfiguration,
                tracingConfiguration,
                null,
                null,
                null,
                expectedTagEntries);

        assertThat(outputModel).isEqualTo(expectedModel);
    }

}
