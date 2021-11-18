package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the logic behind converting between CloudFormation resource
 * model properties and Step Functions model properties
 */
public class Translator {

    private Translator() {
    }

    /**
     * Converts a list of Step Functions model tags to a list of CloudFormation
     * resource model tag entries
     */
    public static List<TagsEntry> getTagsEntriesFromTags(final List<Tag> activityTags) {
        return activityTags.stream().map(e -> new TagsEntry(
                        e.getKey(),
                        e.getValue()))
                .collect(Collectors.toList());
    }
}
