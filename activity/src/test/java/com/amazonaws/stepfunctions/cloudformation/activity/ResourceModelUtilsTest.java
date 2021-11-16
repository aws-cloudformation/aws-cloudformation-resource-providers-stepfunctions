package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceModelUtilsTest extends HandlerTestBase {

    @Test
    public void testGetUpdatedResourceModelFromReadResults_returnsUpdatedModelWithAllProperties() {
        DescribeActivityResult describeActivityResult = new DescribeActivityResult();
        describeActivityResult.setName(ACTIVITY_NAME);
        describeActivityResult.setActivityArn(ACTIVITY_ARN);

        List<Tag> activityTags = new ArrayList<>();
        activityTags.add(new Tag().withKey("Key1").withValue("Value1"));
        activityTags.add(new Tag().withKey("Key2").withValue("Value2"));

        ResourceModel outputModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(describeActivityResult, activityTags);

        List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        ResourceModel expectedModel = new ResourceModel(ACTIVITY_ARN, ACTIVITY_NAME, expectedTagEntries);

        assertThat(outputModel).isEqualTo(expectedModel);
    }

}
