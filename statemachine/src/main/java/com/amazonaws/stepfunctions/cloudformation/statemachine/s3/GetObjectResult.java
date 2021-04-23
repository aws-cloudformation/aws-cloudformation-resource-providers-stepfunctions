package com.amazonaws.stepfunctions.cloudformation.statemachine.s3;

import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.s3.model.S3Object;

import java.util.Objects;

public class GetObjectResult extends AmazonWebServiceResult<ResponseMetadata> {

    private S3Object s3Object;

    public GetObjectResult(S3Object s3Object) {
        this.s3Object = s3Object;
    }

    public S3Object getS3Object() {
        return s3Object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetObjectResult that = (GetObjectResult) o;
        return Objects.equals(s3Object, that.s3Object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(s3Object);
    }

}
