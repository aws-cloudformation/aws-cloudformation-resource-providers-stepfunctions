package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslatorTest extends HandlerTestBase {

    @Test
    public void testGetUpdatedResourceModelFromReadResults_returnsUpdatedModelWithAllProperties() {
        DescribeActivityResult describeActivityResult = new DescribeActivityResult();
        describeActivityResult.setName(ACTIVITY_NAME);
        describeActivityResult.setActivityArn(ACTIVITY_ARN);

        List<Tag> activityTags = new ArrayList<>();
        activityTags.add(new Tag().withKey("Key1").withValue("Value1"));
        activityTags.add(new Tag().withKey("Key2").withValue("Value2"));

        List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(activityTags);

        ResourceModel outputModel = ResourceModelUtils.getUpdatedResourceModelFromReadResults(describeActivityResult, activityTags);

        assertThat(outputModel.getArn()).isEqualTo(ACTIVITY_ARN);
        assertThat(outputModel.getName()).isEqualTo(ACTIVITY_NAME);
        assertThat(outputModel.getTags()).isEqualTo(expectedTagEntries);
    }

}
