package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.stepfunctions.model.CreateStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.ListStateMachineAliasesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineAliasesResult;
import com.amazonaws.services.stepfunctions.model.RoutingConfigurationListItem;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineAliasRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create an alias
     *
     * @param model resource model
     * @return the CreateStateMachineAlias request to create an alias
     */
    static CreateStateMachineAliasRequest translateToCreateRequest(final ResourceModel model) {
        return new CreateStateMachineAliasRequest()
                .withName(model.getName())
                .withDescription(model.getDescription())
                .withRoutingConfiguration(translateToSdkRoutingConfiguration(model.getRoutingConfiguration()));
    }

    /**
     * Request to read a alias
     *
     * @param model resource model
     * @return the DescribeStateMachineAlias request to describe an alias
     */
    static DescribeStateMachineAliasRequest translateToReadRequest(final ResourceModel model) {
        return new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(model.getArn());
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param describeResult the DescribeStateMachineAlias result
     * @return resource model
     */
    static ResourceModel translateFromReadResponse(final DescribeStateMachineAliasResult describeResult) {
        return ResourceModel.builder()
                .arn(describeResult.getStateMachineAliasArn())
                .description(describeResult.getDescription())
                .routingConfiguration(translateToCfnRoutingConfiguration(describeResult.getRoutingConfiguration()))
                .name(describeResult.getName())
                .build();
    }

    /**
     * Request to delete an alias
     *
     * @param model resource model
     * @return the DeleteStateMachineAlias request to delete an alias
     */
    static DeleteStateMachineAliasRequest translateToDeleteRequest(final ResourceModel model) {
        return new DeleteStateMachineAliasRequest()
                .withStateMachineAliasArn(model.getArn());
    }

    /**
     * Request to update properties of a previously created alias
     *
     * @param model resource model
     * @return the UpdateStateMachineAlias request to modify an alias
     */
    static UpdateStateMachineAliasRequest translateToUpdateRequest(final ResourceModel model) {
        return new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(model.getArn())
                .withDescription(model.getDescription())
                .withRoutingConfiguration(translateToSdkRoutingConfiguration(model.getRoutingConfiguration()));
    }

    /**
     * Request to list aliases
     *
     * @param stateMachineVersionArn the state machine version ARN to list aliases for
     * @param nextToken token passed to the ListStateMachineAliases request
     * @return the ListStateMachineAliases request to list aliases within aws account
     */
    static ListStateMachineAliasesRequest translateToListRequest(final String stateMachineVersionArn, final String nextToken) {
        final String stateMachineArn = stateMachineVersionArn.substring(0, stateMachineVersionArn.lastIndexOf(":"));
        return new ListStateMachineAliasesRequest()
                .withStateMachineArn(stateMachineArn)
                .withNextToken(nextToken);
    }

    /**
     * Translates resource objects from sdk into a list of resource models (primary identifier only)
     *
     * @param listResult the ListStateMachineAliases response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResult(final ListStateMachineAliasesResult listResult) {
        return streamOfOrEmpty(listResult.getStateMachineAliases())
                .map(item -> ResourceModel.builder()
                        .arn(item.getStateMachineAliasArn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    // Translates a RoutingConfiguration from the CloudFormation model to the SDK model
    public static Set<RoutingConfigurationListItem> translateToSdkRoutingConfiguration(
            final Set<RoutingConfigurationVersion> cfnRoutingConfigurationVersions) {
        return cfnRoutingConfigurationVersions.stream()
                .map(cfnRoutingConfigurationVersion -> new RoutingConfigurationListItem()
                        .withStateMachineVersionArn(cfnRoutingConfigurationVersion.getStateMachineVersionArn())
                        .withWeight(cfnRoutingConfigurationVersion.getWeight()))
                .collect(Collectors.toSet());
    }

    // Translates a RoutingConfiguration from the SDK model to the CloudFormation model
    public static Set<RoutingConfigurationVersion> translateToCfnRoutingConfiguration(
            final List<RoutingConfigurationListItem> sdkRoutingConfigurationItems) {
        return sdkRoutingConfigurationItems.stream()
                .map(sdkRoutingConfigurationItem -> new RoutingConfigurationVersion(
                        sdkRoutingConfigurationItem.getStateMachineVersionArn(),
                        sdkRoutingConfigurationItem.getWeight()
                ))
                .collect(Collectors.toSet());
    }
}
