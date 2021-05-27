package com.amazonaws.stepfunctions.cloudformation.statemachine.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GetObjectFunctionTest {

    @Mock
    private AmazonS3 client;

    private GetObjectFunction getObjectFunction;

    @BeforeEach
    public void setup() {
        getObjectFunction = new GetObjectFunction(client);
    }

    @Test
    public void testGet_returnsResultFromS3Client() {
        S3Object s3Object = new S3Object();
        GetObjectRequest request = new GetObjectRequest("Bucket", "Key");

        Mockito.when(client.getObject(Mockito.any(GetObjectRequest.class))).thenReturn(s3Object);
        GetObjectResult result = new GetObjectResult(s3Object);

        assertThat(getObjectFunction.get(request)).isEqualTo(result);
    }
}
