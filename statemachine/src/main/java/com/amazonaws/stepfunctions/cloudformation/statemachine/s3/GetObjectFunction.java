package com.amazonaws.stepfunctions.cloudformation.statemachine.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class GetObjectFunction {

    private AmazonS3 client;

    public GetObjectFunction(AmazonS3 s3Client) {
        this.client = s3Client;
    }

    public GetObjectResult get(GetObjectRequest request) {
        S3Object object = client.getObject(request);
        return new GetObjectResult(object);
    }

}
