package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientBuilderTest {

    @Test
    public void testRetryOnThrottling() {
        RetryPolicy retryPolicy = ClientBuilder.getRetryPolicy();

        AmazonWebServiceRequest request = new CreateStateMachineRequest();
        AmazonServiceException e = new AmazonServiceException("Throttling");
        e.setStatusCode(429);

        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();
    }

    @Test
    public void testRetryOnAccessDeniedException() {
        RetryPolicy retryPolicy = ClientBuilder.getRetryPolicy();

        AmazonWebServiceRequest request = new CreateStateMachineRequest();
        AmazonServiceException e = new AmazonServiceException("AccessDeniedException");
        e.setErrorCode("AccessDeniedException");

        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();
    }

    @Test
    public void testRetryOnConflictException() {
        RetryPolicy retryPolicy = ClientBuilder.getRetryPolicy();

        AmazonWebServiceRequest request = new CreateStateMachineRequest();
        AmazonServiceException e = new AmazonServiceException("ConflictException");
        e.setErrorCode("ConflictException");

        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();
    }

}
