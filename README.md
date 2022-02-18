# es4x-multithreaded-access
Reproducer for graal multithreaded access exceptions during verticle deployment

## How to use
This reproducer runs es4x verticles on a jvm instantiated by gradle using the following steps:
1. From the command line, type `./gradlew test`
2. The build script will retrieve an npm distribution, install rxjs 7.2.5 and @vertx/core 4.2.4 from the `package.json` file in this project, and setup a node_modules directory containing two identical es4x verticle implementations 
3. Gradle will then execute the junit 5 test cases in the `DeployTests` junit test class
4. There are 2 unit tests:
   1. `deploysOne` deploys just one of the verticles successfully to demonstrate that there is no problem with any of the modules used in the es4x verticles
   2. `deploysTwo` attempts to deploy both of the verticles, with a delay of 1s between the deployment attempts, both of which attempt to load a common dependency that pulls in functions from the public rxjs package. This test case is currently always failing on the windows system where it is run, and it fails during the module load process for at least one of the verticles, before the deployment event is processed. The error is usually `org.graalvm.polyglot.PolyglotException: Multi threaded access requested by thread`, but there have been some cases where it has failed with a graal vm `Language Assertion`, though this seems to be less frequent.

## verticle implementation
The source code for verticle-01 and verticle-02 is identical except for the logger names used to log their activity. On deployment, each verticle sends an event bus message to some address and waits for a response before it will signal that its deployment is complete.

The junit test cases provide the address configuration to the verticles, and in `deploysOne`, the junit test case implements a consumer that will immediately respond as soon as the es4x verticle starts its deployment and sends its message. If the verticle loads and completes deployment, that means it was able to process all of its require statements and complete its messaging.

In the `deploysTwo` test case, the junit driver provides address configuration to the two verticles, expecting both of them to deploy successfully, which can only happen if both verticles successfully process all of their require statements and complete their messaging during the `deploy` event.
