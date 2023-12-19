# AWS::StepFunctions::StateMachine

Resource schema for StateMachine

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::StepFunctions::StateMachine",
    "Properties" : {
        "<a href="#definitionstring" title="DefinitionString">DefinitionString</a>" : <i>String</i>,
        "<a href="#rolearn" title="RoleArn">RoleArn</a>" : <i>String</i>,
        "<a href="#statemachinename" title="StateMachineName">StateMachineName</a>" : <i>String</i>,
        "<a href="#statemachinetype" title="StateMachineType">StateMachineType</a>" : <i>String</i>,
        "<a href="#loggingconfiguration" title="LoggingConfiguration">LoggingConfiguration</a>" : <i><a href="loggingconfiguration.md">LoggingConfiguration</a></i>,
        "<a href="#tracingconfiguration" title="TracingConfiguration">TracingConfiguration</a>" : <i><a href="tracingconfiguration.md">TracingConfiguration</a></i>,
        "<a href="#definitions3location" title="DefinitionS3Location">DefinitionS3Location</a>" : <i><a href="s3location.md">S3Location</a></i>,
        "<a href="#definitionsubstitutions" title="DefinitionSubstitutions">DefinitionSubstitutions</a>" : <i><a href="definitionsubstitutions.md">DefinitionSubstitutions</a></i>,
        "<a href="#definition" title="Definition">Definition</a>" : <i>Map</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tagsentry.md">TagsEntry</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::StepFunctions::StateMachine
Properties:
    <a href="#definitionstring" title="DefinitionString">DefinitionString</a>: <i>String</i>
    <a href="#rolearn" title="RoleArn">RoleArn</a>: <i>String</i>
    <a href="#statemachinename" title="StateMachineName">StateMachineName</a>: <i>String</i>
    <a href="#statemachinetype" title="StateMachineType">StateMachineType</a>: <i>String</i>
    <a href="#loggingconfiguration" title="LoggingConfiguration">LoggingConfiguration</a>: <i><a href="loggingconfiguration.md">LoggingConfiguration</a></i>
    <a href="#tracingconfiguration" title="TracingConfiguration">TracingConfiguration</a>: <i><a href="tracingconfiguration.md">TracingConfiguration</a></i>
    <a href="#definitions3location" title="DefinitionS3Location">DefinitionS3Location</a>: <i><a href="s3location.md">S3Location</a></i>
    <a href="#definitionsubstitutions" title="DefinitionSubstitutions">DefinitionSubstitutions</a>: <i><a href="definitionsubstitutions.md">DefinitionSubstitutions</a></i>
    <a href="#definition" title="Definition">Definition</a>: <i>Map</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tagsentry.md">TagsEntry</a></i>
</pre>

## Properties

#### DefinitionString

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>1048576</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RoleArn

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StateMachineName

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>80</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### StateMachineType

_Required_: No

_Type_: String

_Allowed Values_: <code>STANDARD</code> | <code>EXPRESS</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### LoggingConfiguration

_Required_: No

_Type_: <a href="loggingconfiguration.md">LoggingConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TracingConfiguration

_Required_: No

_Type_: <a href="tracingconfiguration.md">TracingConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DefinitionS3Location

_Required_: No

_Type_: <a href="s3location.md">S3Location</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DefinitionSubstitutions

_Required_: No

_Type_: <a href="definitionsubstitutions.md">DefinitionSubstitutions</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Definition

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tagsentry.md">TagsEntry</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

Returns the <code>Arn</code> value.

#### Name

Returns the <code>Name</code> value.

#### StateMachineRevisionId

Returns the <code>StateMachineRevisionId</code> value.
