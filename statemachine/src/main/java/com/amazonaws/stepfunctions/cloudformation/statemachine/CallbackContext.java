package com.amazonaws.stepfunctions.cloudformation.statemachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext {
    @Builder.Default
    private boolean deletionStarted = false;
}
