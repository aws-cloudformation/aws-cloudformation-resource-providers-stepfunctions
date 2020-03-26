package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceRequest;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.TagResourceRequest;
import com.amazonaws.services.stepfunctions.model.UntagResourceRequest;
import com.google.common.collect.Sets;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaggingHelper {

    public static List<Tag> consolidateTags(ResourceHandlerRequest<ResourceModel> request) {
        List<TagsEntry> customerTags = request.getDesiredResourceState().getTags();
        Map<String, String> resourceTags = request.getDesiredResourceTags();

        Map<String, String> tags = new HashMap<>();

        if (resourceTags != null) {
            tags.putAll(resourceTags);
        }

        if (customerTags != null) {
            for (TagsEntry e : customerTags) {
                tags.put(e.getKey(), e.getValue());
            }
        }

        return tags.entrySet().stream().map(e -> new Tag().withKey(e.getKey()).withValue(e.getValue())).collect(Collectors.toList());
    }

    public static List<Tag> listTagsForResource(String resourceArn,
                                                AmazonWebServicesClientProxy proxy,
                                                AWSStepFunctions client) {
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest();
        listTagsForResourceRequest.setResourceArn(resourceArn);

        return proxy.injectCredentialsAndInvoke(listTagsForResourceRequest, client::listTagsForResource).getTags();
    }

    public static void addTags(String resourceArn,
                               Set<Tag> tags,
                               AmazonWebServicesClientProxy proxy,
                               AWSStepFunctions client) {
        TagResourceRequest tagResourceRequest = new TagResourceRequest();
        tagResourceRequest.setResourceArn(resourceArn);
        tagResourceRequest.setTags(tags);

        proxy.injectCredentialsAndInvoke(tagResourceRequest, client::tagResource);
    }

    public static void removeTags(String resourceArn,
                                  Set<Tag> tags,
                                  AmazonWebServicesClientProxy proxy,
                                  AWSStepFunctions client) {
        Collection<String> tagKeys = tags.stream().map(Tag::getKey).collect(Collectors.toSet());
        UntagResourceRequest untagResourceRequest = new UntagResourceRequest();
        untagResourceRequest.setResourceArn(resourceArn);
        untagResourceRequest.setTagKeys(tagKeys);

        proxy.injectCredentialsAndInvoke(untagResourceRequest, client::untagResource);
    }

    public static void updateTags(String resourceArn,
                                  Set<Tag> previousTags,
                                  Set<Tag> currentTags,
                                  AmazonWebServicesClientProxy proxy,
                                  AWSStepFunctions client) {
        Set<Tag> tagsToAdd = Sets.difference(currentTags, previousTags);
        Set<Tag> tagsToRemove = Sets.difference(previousTags, currentTags);
        if (!tagsToRemove.isEmpty()) {
            removeTags(resourceArn, tagsToRemove, proxy, client);
        }
        if (!tagsToAdd.isEmpty()) {
            addTags(resourceArn, tagsToAdd, proxy, client);
        }
    }

    public static Set<Tag> transformTags(List<TagsEntry> tags) {
        Set<Tag> filteredTags = new HashSet<>();
        if (tags != null) {
            for (TagsEntry e : tags) {
                if (!e.getKey().toLowerCase().startsWith("aws:")) {
                    filteredTags.add(new Tag().withKey(e.getKey()).withValue(e.getValue()));
                }
            }
        }

        return filteredTags;
    }

    public static Set<Tag> transformTags(Map<String, String> tags) {
        Set<Tag> filteredTags = new HashSet<>();
        if (tags != null) {
            for (Map.Entry<String, String> e : tags.entrySet()) {
                if (!e.getKey().toLowerCase().startsWith("aws:")) {
                    filteredTags.add(new Tag().withKey(e.getKey()).withValue(e.getValue()));
                }
            }
        }

        return filteredTags;
    }

}
