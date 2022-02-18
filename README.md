# es4x-multithreaded-access
Reproducer for graal multithreaded access exceptions during verticle deployment

## How to use
This reproducer deploys an es4x verticle on a jvm instantiated by gradle using the following steps:
1. From the command line, type `./gradlew test`
2. The build script will retrieve an npm distribution, install rxjs 7.2.5 and @vertx/core 4.2.4 from the `package.json` file in this project, and setup a node_modules directory containing a single es4x verticle implementation that includes an internal js module that uses rxjs
3. Gradle will then execute the junit 5 test cases in the `DeployTests` test class
4. There are 2 unit tests:
   1. `deploysOne` deploys one instance of the verticle (test-verticle) successfully to demonstrate that there is no problem with any of the modules used in the es4x verticle with a single deployed instance
   2. `deploysTwo` attempts to deploy two separate instances of the test verticle, with a delay of 1s between the deployment attempts. This test case is currently always failing on the windows system where it is run, and it fails during the module load process for the second deployed verticle, before its `deployment` event is processed. The error is usually `org.graalvm.polyglot.PolyglotException: Multi threaded access requested by thread`, but there have been some cases where it has failed with `org.graalvm.polyglot.PolyglotException: java.lang.AssertionError`. This error seems to occur when the delay time between deployments is increased long enough (for example, 10s) so the first verticle instanc is able to complete processing its `deploy` event before the second instance's deployment starts. When this is the case, the second instance will usually fail with the `AssertionError` exception during require processing in its internal `observables` module.

## verticle implementation
The source code for the test verticle sends an event bus message to some address and waits for a response before it will signal that its deployment is complete.

The junit test cases provide the address configuration to the test verticle, and in `deploysOne`, the junit test case implements a consumer that will immediately respond as soon as the test verticle starts its deployment and sends its message. If the verticle loads and completes deployment, that means it was able to process all of its require statements and complete its messaging.

In the `deploysTwo` test case, the junit driver provides address configuration to two deployments of the same verticle, expecting both of them to deploy successfully, which can only happen if both instances successfully process all of their require statements and complete their messaging with each other during their `deploy` events.
