package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.ListTagsForResourceRequest;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.TagResourceRequest;
import com.amazonaws.services.stepfunctions.model.TagResourceResult;
import com.amazonaws.services.stepfunctions.model.UntagResourceRequest;
import com.amazonaws.services.stepfunctions.model.UntagResourceResult;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TaggingHelperTest extends HandlerTestBase {

    @Test
    public void testConsolidateTagsWithEmptyTags() {
        ResourceModel model = new ResourceModel();
        Map<String, String> resourceTags = new HashMap<>();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(resourceTags)
                .build();

        List<Tag> tags = TaggingHelper.consolidateTags(request);
        List<Tag> expectedTags = new ArrayList<>();

        assertThat(tags).isEqualTo(expectedTags);
    }

    @Test
    public void testConsolidateTagsWithTags() {
        ResourceModel model = new ResourceModel();
        model.setTags(Lists.newArrayList(new TagsEntry("K1", "V1"), new TagsEntry("K2", "V2")));
        Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("K2", "V2");
        resourceTags.put("K3", "V3");
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(resourceTags)
                .build();

        List<Tag> tags = TaggingHelper.consolidateTags(request);
        List<Tag> expectedTags = Lists.newArrayList(
                new Tag().withKey("K1").withValue("V1"),
                new Tag().withKey("K2").withValue("V2"),
                new Tag().withKey("K3").withValue("V3")
        );

        assertThat(Sets.newHashSet(tags)).isEqualTo(Sets.newHashSet(expectedTags));
    }

    @Test
    public void testListTagsForResource() {
        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(Lists.newArrayList(new Tag().withKey("K1").withValue("V1")));
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(ListTagsForResourceRequest.class), Mockito.any(Function.class))).thenReturn(listTagsForResourceResult);

        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest();
        listTagsForResourceRequest.setResourceArn(STATE_MACHINE_ARN);

        TaggingHelper.listTagsForResource(STATE_MACHINE_ARN, proxy, client);

        Mockito.verify(proxy, Mockito.times(1))
                .injectCredentialsAndInvoke(Mockito.eq(listTagsForResourceRequest), Mockito.any(Function.class));
    }

    @Test
    public void testTransformTagsFromMap() {
        Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("K2", "V2");
        resourceTags.put("K3", "V3");

        Set<Tag> tags = TaggingHelper.transformTags(resourceTags);
        Set<Tag> expectedTags = Sets.newHashSet(
                new Tag().withKey("K2").withValue("V2"),
                new Tag().withKey("K3").withValue("V3")
        );

        assertThat(tags).isEqualTo(expectedTags);
    }

    @Test
    public void testTransformTagsFromTagsEntry() {
        List<TagsEntry> resourceTags = Lists.newArrayList(
                new TagsEntry("K2", "V2"),
                new TagsEntry("K3", "V3")
        );

        Set<Tag> tags = TaggingHelper.transformTags(resourceTags);
        Set<Tag> expectedTags = Sets.newHashSet(
                new Tag().withKey("K2").withValue("V2"),
                new Tag().withKey("K3").withValue("V3")
        );

        assertThat(tags).isEqualTo(expectedTags);
    }

    @Test
    public void testUpdateTags() {
        Set<Tag> previousTags = Sets.newHashSet(
                new Tag().withKey("K1").withValue("V1"),
                new Tag().withKey("K3").withValue("V3")
        );
        Set<Tag> currentTags = Sets.newHashSet(
                new Tag().withKey("K1").withValue("V1"),
                new Tag().withKey("K2").withValue("V2"),
                new Tag().withKey("K4").withValue("V4")
        );

        UntagResourceRequest untagResourceRequest = new UntagResourceRequest();
        untagResourceRequest.setResourceArn(STATE_MACHINE_ARN);
        untagResourceRequest.setTagKeys(Lists.newArrayList("K3"));

        TagResourceRequest tagResourceRequest = new TagResourceRequest();
        tagResourceRequest.setResourceArn(STATE_MACHINE_ARN);
        tagResourceRequest.setTags(Lists.newArrayList(
                new Tag().withKey("K2").withValue("V2"),
                new Tag().withKey("K4").withValue("V4")
        ));

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(new UntagResourceResult(), new TagResourceResult());

        TaggingHelper.updateTags(STATE_MACHINE_ARN, previousTags, currentTags, proxy, client);

        Mockito.verify(proxy, Mockito.times(1))
                .injectCredentialsAndInvoke(Mockito.eq(tagResourceRequest), Mockito.any(Function.class));
        Mockito.verify(proxy, Mockito.times(1))
                .injectCredentialsAndInvoke(Mockito.eq(untagResourceRequest), Mockito.any(Function.class));
    }

}
