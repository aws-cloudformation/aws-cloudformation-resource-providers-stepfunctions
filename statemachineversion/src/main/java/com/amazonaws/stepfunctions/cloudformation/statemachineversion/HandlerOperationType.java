package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

/**
 *  Enum to represent the CloudFormation handler operations supported by this resource
 *  Used to identify the operation being performed when logging resource property usage metrics
*/
public enum HandlerOperationType {
    CREATE,
    DELETE,
    READ,
    LIST
}
