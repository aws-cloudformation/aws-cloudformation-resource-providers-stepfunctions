package com.amazonaws.stepfunctions.cloudformation.statemachine.s3;

import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GetObjectResultTest {

    private final S3Object s3Object = new S3Object();

    private final S3Object secondS3Object = new S3Object();

    private GetObjectResult getObjectResult;
    private GetObjectResult secondGetObjectResult;


    @BeforeEach
    public void setup() {
        getObjectResult = new GetObjectResult(s3Object);
        s3Object.setKey("a");
        secondS3Object.setKey("b");
    }

    @Test
    public void testEquals_isTrue_whenSameObject() {
        assertThat(getObjectResult.equals(getObjectResult)).isTrue();
    }

    @Test
    public void testEquals_isFalse_whenNull() {
        assertThat(getObjectResult.equals(null)).isFalse();
    }

    @Test
    public void testEquals_isFalse_whenDifferentClass() {
        assertThat(getObjectResult.equals("")).isFalse();
    }

    @Test
    public void testEquals_isFalse_whenS3ObjectNotEqual() {
        secondGetObjectResult = new GetObjectResult(secondS3Object);
        assertThat(getObjectResult.equals(secondGetObjectResult)).isFalse();
    }

    @Test
    public void testEquals_isTrue_whenS3ObjectEqual() {
        secondGetObjectResult = new GetObjectResult(s3Object);
        assertThat(getObjectResult.equals(secondGetObjectResult)).isTrue();
    }

}
