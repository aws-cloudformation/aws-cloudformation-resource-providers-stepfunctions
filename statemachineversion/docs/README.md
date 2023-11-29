# AWS::StepFunctions::StateMachineVersion

Resource schema for StateMachineVersion

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::StepFunctions::StateMachineVersion",
    "Properties" : {
        "<a href="#statemachinearn" title="StateMachineArn">StateMachineArn</a>" : <i>String</i>,
        "<a href="#statemachinerevisionid" title="StateMachineRevisionId">StateMachineRevisionId</a>" : <i>String</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::StepFunctions::StateMachineVersion
Properties:
    <a href="#statemachinearn" title="StateMachineArn">StateMachineArn</a>: <i>String</i>
    <a href="#statemachinerevisionid" title="StateMachineRevisionId">StateMachineRevisionId</a>: <i>String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
</pre>

## Properties

#### StateMachineArn

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### StateMachineRevisionId

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Description

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

Returns the <code>Arn</code> value.
