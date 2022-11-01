# Contributing Guidelines

Thank you for your interest in contributing to our project. Whether it's a bug report, new feature, correction, or additional
documentation, we greatly value feedback and contributions from our community.

Please read through this document before submitting any issues or pull requests to ensure we have all the necessary
information to effectively respond to your bug report or contribution.


## Package Setup

1. Follow the CloudFormation CLI environment setup instructions [here](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/what-is-cloudformation-cli.html#resource-type-setup).
2. Install version `3.8.1` of Apache Maven [here](https://maven.apache.org/install.html).
3. Install Java 8 [here](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html).
4. Install pre-commit [here](https://pre-commit.com/#install)
5. Create a new fork of the main repository by following [these instructions](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo).
6. Open the project in Intellij:
    1. From the File menu, choose New, then choose Project From Existing Sources.
    2. Select the folder for the resource that you will be developing (eg. statemachine).
    3. In the Import Project dialog box, choose Import project from external model and then choose Maven.
    4. Choose Next and accept any defaults to complete importing the project.
    5. From the Build menu, choose Build Project
7. From the root of the resource package run `mvn clean && cfn generate && mvn package`

## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest features.

When filing an issue, please check [existing open](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions/issues), or [recently closed](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20), issues to make sure somebody else hasn't already
reported the issue. Please try to include as much information as you can. Details like these are incredibly useful:

* A reproducible test case or series of steps
* The version of our code being used
* Any modifications you've made relevant to the bug
* Anything unusual about your environment or deployment


## Contributing via Pull Requests
Contributions via pull requests are much appreciated. Before sending us a pull request, please ensure that:

1. You are working against the latest source on the *main* branch.
2. You check existing open, and recently merged, pull requests to make sure someone else hasn't addressed the problem already.
3. You open an issue to discuss any significant work - we would hate for your time to be wasted.

To send us a pull request from your fork of the repository, please:

1. Modify the source; please focus on the specific change you are contributing. If you also reformat all the code, it will be hard for us to focus on your change.
2. Ensure local tests pass by running `mvn test` from the root of the resource package.
3. Commit to your fork using clear commit messages.
4. Before pushing to your local fork, run `pre-commit run --all-files`. Commit any changes made by pre-commit.
5. Send us a pull request, answering any default questions in the pull request interface.
6. Pay attention to any automated CI failures reported in the pull request, and stay involved in the conversation.

GitHub provides additional document on [creating a pull request](https://help.github.com/articles/creating-a-pull-request/).


## Finding contributions to work on
Looking at the existing issues is a great way to find something to contribute on. As our projects, by default, use the default GitHub issue labels (enhancement/bug/duplicate/help wanted/invalid/question/wontfix), looking at any ['help wanted'](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions/labels/help%20wanted) issues is a great place to start.


## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct).
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact
opensource-codeofconduct@amazon.com with any additional questions or comments.


## Security issue notifications
If you discover a potential security issue in this project we ask that you notify AWS/Amazon Security via our [vulnerability reporting page](http://aws.amazon.com/security/vulnerability-reporting/). Please do **not** create a public github issue.


## Licensing
See the [LICENSE](LICENSE) file for our project's licensing. We will ask you to confirm the licensing of your contribution.
