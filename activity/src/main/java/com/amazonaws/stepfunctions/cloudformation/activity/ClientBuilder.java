package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;

public class ClientBuilder {

    public static AWSStepFunctions getClient() {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(10));

        return AWSStepFunctionsClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .build();
    }

}
