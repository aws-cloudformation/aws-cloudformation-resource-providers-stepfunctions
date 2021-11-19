package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import com.amazonaws.services.stepfunctions.model.Tag;

import java.util.List;

/**
 * Encapsulates the logic behind generating the ResourceModel for the request's desiredResourceState
 */
public class ResourceModelUtils {

    private ResourceModelUtils() {
    }

    /**
     * Generates a resource model containing all activity resource properties
     *
     * @param describeActivityResult The result of calling DescribeActivity for the activity
     * @param activityTags           A list of tags associated with the activity
     * @return A resource model containing the ARN, Name, and list of tags for the activity
     */
    public static ResourceModel getUpdatedResourceModelFromReadResults(final DescribeActivityResult describeActivityResult,
                                                                       final List<Tag> activityTags) {
        final ResourceModel model = ResourceModel.builder()
                .arn(describeActivityResult.getActivityArn())
                .name(describeActivityResult.getName())
                .build();

        if (activityTags != null) {
            model.setTags(Translator.getTagsEntriesFromTags(activityTags));
        }

        return model;
    }
}
