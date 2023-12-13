# AWS::StepFunctions::StateMachineAlias

Resource schema for StateMachineAlias

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::StepFunctions::StateMachineAlias",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#routingconfiguration" title="RoutingConfiguration">RoutingConfiguration</a>" : <i>[ <a href="routingconfigurationversion.md">RoutingConfigurationVersion</a>, ... ]</i>,
        "<a href="#deploymentpreference" title="DeploymentPreference">DeploymentPreference</a>" : <i><a href="deploymentpreference.md">DeploymentPreference</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::StepFunctions::StateMachineAlias
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#routingconfiguration" title="RoutingConfiguration">RoutingConfiguration</a>: <i>
      - <a href="routingconfigurationversion.md">RoutingConfigurationVersion</a></i>
    <a href="#deploymentpreference" title="DeploymentPreference">DeploymentPreference</a>: <i><a href="deploymentpreference.md">DeploymentPreference</a></i>
</pre>

## Properties

#### Name

The alias name.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>80</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Description

An optional description of the alias.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RoutingConfiguration

The routing configuration of the alias. One or two versions can be mapped to an alias to split StartExecution requests of the same state machine.

_Required_: No

_Type_: List of <a href="routingconfigurationversion.md">RoutingConfigurationVersion</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DeploymentPreference

The settings to enable gradual state machine deployments.

_Required_: No

_Type_: <a href="deploymentpreference.md">DeploymentPreference</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The ARN of the alias.
