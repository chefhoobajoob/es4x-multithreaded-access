# es4x-multithreaded-access
Reproducer for graal multithreaded access exceptions during verticle deployment

## How to use
This reproducer runs es4x verticles on a jvm instantiated by gradle using the following steps:
1. From the command line, type `./gradlew test`
2. The build script will retrieve an npm distribution, install rxjs 7.2.5 and @vertx/core 4.2.4 from the `package.json` file in this project, and setup a node_modules directory containing two identical es4x verticle implementations and a common js module that uses rxjs 
3. Gradle will then execute the junit 5 test cases in the `DeployTests` test class
4. There are 2 unit tests:
   1. `deploysOne` deploys just one of the verticles (mock-verticle-01) successfully to demonstrate that there is no problem with any of the modules used in the es4x verticles
   2. `deploysTwo` attempts to deploy both of the verticles (mock-verticle-01 and mock-verticle-02), with a delay of 1s between the deployment attempts, both of which attempt to load a common dependency module (mock-internal) that pulls in functions from the public rxjs and es4x vertx/core packages. This test case is currently always failing on the windows system where it is run, and it fails during the module load process for the second deployed verticle, before its `deployment` event is processed. The error is usually `org.graalvm.polyglot.PolyglotException: Multi threaded access requested by thread`, but there have been some cases where it has failed with `org.graalvm.polyglot.PolyglotException: java.lang.AssertionError`. This error seems to occur when the delay time between deployments is increased long enough (for example, 10s) so the first verticle is able to complete processing its `deploy` event before the second verticle's deployment starts. When this is the case, the second verticle will usually fail with the `AssertionError` exception during require processing in the `mock-internal` library.

## verticle implementation
The source code for verticle-01 and verticle-02 is identical except for the logger names used to log their activity. On deployment, each verticle sends an event bus message to some address and waits for a response before it will signal that its deployment is complete.

The junit test cases provide the address configuration to the verticles, and in `deploysOne`, the junit test case implements a consumer that will immediately respond as soon as the es4x verticle starts its deployment and sends its message. If the verticle loads and completes deployment, that means it was able to process all of its require statements and complete its messaging.

In the `deploysTwo` test case, the junit driver provides address configuration to the two verticles, expecting both of them to deploy successfully, which can only happen if both verticles successfully process all of their require statements and complete their messaging with each other during their `deploy` events.
