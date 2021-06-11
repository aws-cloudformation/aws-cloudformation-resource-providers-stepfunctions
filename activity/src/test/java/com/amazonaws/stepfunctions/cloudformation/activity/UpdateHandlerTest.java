package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.TagResourceRequest;
import com.amazonaws.services.stepfunctions.model.TagResourceResult;
import com.amazonaws.services.stepfunctions.model.UntagResourceRequest;
import com.amazonaws.services.stepfunctions.model.UntagResourceResult;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateHandlerTest extends HandlerTestBase {

    private UpdateHandler handler = new UpdateHandler();

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
                .previousResourceState(ResourceModel.builder()
                        .arn(ACTIVITY_ARN)
                        .name(ACTIVITY_NAME)
                        .build()
                )
                .build();
    }

    @Test
    public void testSuccess() {
        UntagResourceRequest untagResourceRequest = new UntagResourceRequest();
        untagResourceRequest.setResourceArn(ACTIVITY_ARN);
        untagResourceRequest.setTagKeys(Lists.newArrayList("K1", "K2"));

        TagResourceRequest tagResourceRequest = new TagResourceRequest();
        tagResourceRequest.setResourceArn(ACTIVITY_ARN);
        tagResourceRequest.setTags(Lists.newArrayList(
                new Tag().withKey("K3").withValue("V3")
        ));

        Map<String, String> resourceTags = new HashMap<>();
        Map<String, String> previousResourceTags = new HashMap<>();

        resourceTags.put("K3", "V3");
        previousResourceTags.put("K2", "V2");
        previousResourceTags.put("K1", "V1");

        request.setPreviousResourceTags(previousResourceTags);
        request.setDesiredResourceTags(resourceTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(untagResourceRequest), Mockito.any(Function.class))).thenReturn(new UntagResourceResult());
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(tagResourceRequest), Mockito.any(Function.class))).thenReturn(new TagResourceResult());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void test500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(UntagResourceRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        Map<String, String> previousResourceTags = new HashMap<>();
        previousResourceTags.put("K3", "V3");
        request.setPreviousResourceTags(previousResourceTags);

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

    @Test
    public void testResourceDoesNotExist_returnsNotFound() {
        List<TagsEntry> previousTags = new ArrayList<>();
        previousTags.add(new TagsEntry("K1", "V1"));
        previousTags.add(new TagsEntry("K3", "V3"));

        List<TagsEntry> currentTags = new ArrayList<>();
        currentTags.add(new TagsEntry("K1", "V1"));
        currentTags.add(new TagsEntry("K2", "V2"));
        currentTags.add(new TagsEntry("K4", "V4"));

        request.setPreviousResourceState(ResourceModel.builder()
                .arn(ACTIVITY_ARN)
                .name(ACTIVITY_NAME)
                .tags(previousTags)
                .build()
        );
        request.setDesiredResourceState(ResourceModel.builder()
                .arn(ACTIVITY_ARN)
                .name(ACTIVITY_NAME)
                .tags(currentTags)
                .build()
        );

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(UntagResourceRequest.class), Mockito.any(Function.class))).thenThrow(resourceNotFoundException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).isEqualTo(resourceNotFoundException.getMessage());
    }

}
