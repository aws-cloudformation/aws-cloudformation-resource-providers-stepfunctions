package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
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
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends HandlerTestBase {

    private UpdateHandler handler = new UpdateHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().arn(ACTIVITY_ARN).name(ACTIVITY_NAME).build())
                .build();
    }

    @Test
    public void testSuccess() {
        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(Lists.newArrayList(new Tag().withKey("K1").withValue("V1"), new Tag().withKey("K2").withValue("V2")));
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(listTagsForResourceResult);

        UntagResourceRequest untagResourceRequest = new UntagResourceRequest();
        untagResourceRequest.setResourceArn(ACTIVITY_ARN);
        untagResourceRequest.setTagKeys(Lists.newArrayList("K1", "K2"));

        TagResourceRequest tagResourceRequest = new TagResourceRequest();
        tagResourceRequest.setResourceArn(ACTIVITY_ARN);
        tagResourceRequest.setTags(Lists.newArrayList(
                new Tag().withKey("K3").withValue("V3")
        ));

        Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("K3", "V3");

        request.setDesiredResourceTags(resourceTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(untagResourceRequest), Mockito.any())).thenReturn(new UntagResourceResult());
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(tagResourceRequest), Mockito.any())).thenReturn(new TagResourceResult());

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
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

}
