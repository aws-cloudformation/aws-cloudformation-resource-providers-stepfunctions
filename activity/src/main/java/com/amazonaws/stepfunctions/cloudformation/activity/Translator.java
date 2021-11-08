package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import com.amazonaws.services.stepfunctions.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

public class Translator {

    /**
     * Generates a resource model containing all activity resource properties
     * @param describeActivityResult The result of calling DescribeActivity for the activity
     * @param activityTags A list of tags associated with the activity
     * @return A resource model containing the ARN, Name, and list of tags for the activity
     */
    public static ResourceModel getUpdatedResourceModelFromReadResults(DescribeActivityResult describeActivityResult,
                                                          List<Tag> activityTags) {
        ResourceModel model = new ResourceModel();

        model.setArn(describeActivityResult.getActivityArn());
        model.setName(describeActivityResult.getName());
        model.setTags(getTagsEntriesFromTags(activityTags));

        return model;
    }

    private static List<TagsEntry> getTagsEntriesFromTags(List<Tag> activityTags) {
        return activityTags.stream().map(e -> new TagsEntry(
                        e.getKey(),
                        e.getValue()))
                .collect(Collectors.toList());
    }
}
