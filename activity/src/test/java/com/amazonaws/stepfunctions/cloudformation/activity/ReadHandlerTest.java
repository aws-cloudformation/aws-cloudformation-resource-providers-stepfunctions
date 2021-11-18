package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.DescribeActivityRequest;
import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestBase {

    private final ReadHandler handler = new ReadHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(ACTIVITY_ARN)
                        .name(ACTIVITY_NAME)
                        .build()
                )
                .build();
    }

    @Test
    public void testSuccess() {
        final DescribeActivityResult describeActivityResult = new DescribeActivityResult();
        describeActivityResult.setName(ACTIVITY_NAME);
        describeActivityResult.setActivityArn(ACTIVITY_ARN);

        final List<Tag> activityTags = new ArrayList<>();
        activityTags.add(new Tag().withKey("Key1").withValue("Value1"));
        activityTags.add(new Tag().withKey("Key2").withValue("Value2"));

        final ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(activityTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeActivityResult)
                .thenReturn(listTagsForResourceResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        final ResourceModel expectedModel = new ResourceModel(ACTIVITY_ARN, ACTIVITY_NAME, expectedTagEntries);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testReturnsFailed_whenDescribeActivityThrows500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeActivityRequest.class),
                Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testRedactsTags_whenListTagsForResourceThrowsAccessDenied() {
        final DescribeActivityResult describeActivityResult = new DescribeActivityResult();
        describeActivityResult.setName(ACTIVITY_NAME);
        describeActivityResult.setActivityArn(ACTIVITY_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeActivityResult)
                .thenThrow(accessDeniedException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final ResourceModel expectedModel = new ResourceModel(ACTIVITY_ARN, ACTIVITY_NAME, null);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testReturnsFailed_whenListTagsForResourceThrows500() {
        final DescribeActivityResult describeActivityResult = new DescribeActivityResult();
        describeActivityResult.setName(ACTIVITY_NAME);
        describeActivityResult.setActivityArn(ACTIVITY_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeActivityResult)
                .thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testResourceArnIsNull_returnsNotFound() {
        request.setDesiredResourceState(ResourceModel.builder()
                .name(ACTIVITY_NAME)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(Constants.ACTIVITY_ARN_NOT_FOUND_MESSAGE);
    }

}
