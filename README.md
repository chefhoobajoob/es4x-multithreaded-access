# es4x-multithreaded-access
Reproducer for graal cross-context module access exceptions during verticle deployment. These test cases use the rxjs library to illustrate the problem, but any sufficiently large/modularized npm package (ramda, yup, validator, etc.) can create these same conflicts. The problem is not unique to rxjs.

## How to use
This reproducer deploys an es4x verticle on a jvm instantiated by gradle using the following steps:
1. From the command line, type `./gradlew test`
2. The build script will retrieve an npm distribution, install rxjs 7.2.5 and @vertx/core 4.2.4 from the `package.json` file in this project, and setup a node_modules directory containing a single es4x verticle implementation that includes an internal js module that uses rxjs
3. Gradle will then execute the junit 5 test cases in the `DeployTests` test class
### Test Cases
#### `deploysOne`
Deploys one instance of the verticle (test-verticle) successfully to demonstrate that there is no problem with any of the modules used in the es4x verticle with a single deployed instance. This test case always passes.
#### `deploysTwoOverlapping`
Attempts to deploy two separate instances of the test verticle, with a delay of 1s between the deployment attempts, likely to be processing module load requests simultaneously for the two instances. This test case is currently always failing on the windows system where it is run, and it fails during the module load process for the second deployed verticle, before its `deployment` event is processed. The error in this case is `org.graalvm.polyglot.PolyglotException: Multi threaded access requested by thread` with a stack trace like:
```java
org.graalvm.polyglot.PolyglotException: Multi threaded access requested by thread Thread[vert.x-eventloop-thread-2,5,main] but is not allowed for language(s) js.
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotEngineException.illegalState(PolyglotEngineException.java:129)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.throwDeniedThreadAccess(PolyglotContextImpl.java:1034)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.checkAllThreadAccesses(PolyglotContextImpl.java:893)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.enterThreadChanged(PolyglotContextImpl.java:723)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotEngineImpl.enterCached(PolyglotEngineImpl.java:1991)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.HostToGuestRootNode.execute(HostToGuestRootNode.java:110)
  at app/org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotMap.get(PolyglotMap.java:127)
  at app//io.reactiverse.es4x.Runtime$1.apply(Runtime.java:82)
  at app//io.reactiverse.es4x.Runtime$1.apply(Runtime.java:58)
  at <js>._load(jvm-npm.js:68)
  at <js>.Require(jvm-npm.js:98)
  at <js>.this.require(jvm-npm.js:46)
  at <js>.:anonymous(/<project-root>/node_modules/rxjs/dist/cjs/index.js:220)
  at <js>._load(jvm-npm.js:73)
  at <js>.Require(jvm-npm.js:98)
  at <js>.this.require(jvm-npm.js:46)
  at <js>.:anonymous(/<project-root>/node_modules/test-verticle/observables.js:11)
  at <js>._load(jvm-npm.js:73)
  at <js>.Require(jvm-npm.js:98)
  at <js>.this.require(jvm-npm.js:46)
  at <js>.:anonymous(/<project-root>/node_modules/test-verticle/someModule.js:9)
```
#### `deploysOneThenTwo`
Like `deploysTwoOverlapping`, but with a 10 second interval between deploy attempts, so the first verticle instance is able to complete processing its `deploy` event before the second instance's deployment start. This test always fails with the exception `org.graalvm.polyglot.PolyglotException: java.lang.AssertionError`. Typical stack trace (the specific rxjs module that will produce the error typically changes with each run):
```java
org.graalvm.polyglot.PolyglotException: java.lang.AssertionError
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostContext.toGuestValue(HostContext.java:274)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostContext$ToGuestValueNode.doCached(HostContext.java:305)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostContextFactory$ToGuestValueNodeGen.execute(HostContextFactory.java:43)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostExecuteNode.doInvoke(HostExecuteNode.java:872)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostExecuteNode.doFixed(HostExecuteNode.java:137)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostExecuteNodeGen.execute(HostExecuteNodeGen.java:61)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostObject.invokeMember(HostObject.java:451)
    at app/org.graalvm.truffle/com.oracle.truffle.host.HostObjectGen$InteropLibraryExports$Cached.invokeMember(HostObjectGen.java:2674)
    at app/org.graalvm.truffle/com.oracle.truffle.api.interop.InteropLibrary$Asserts.invokeMember(InteropLibrary.java:3411)
    at app//com.oracle.truffle.js.nodes.cast.OrdinaryToPrimitiveNode.doForeignHintString(OrdinaryToPrimitiveNode.java:172)
    at app//com.oracle.truffle.js.nodes.cast.OrdinaryToPrimitiveNode.doForeign(OrdinaryToPrimitiveNode.java:108)
    at app//com.oracle.truffle.js.nodes.cast.OrdinaryToPrimitiveNodeGen.execute(OrdinaryToPrimitiveNodeGen.java:57)
    at app//com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.ordinaryToPrimitive(JSToPrimitiveNode.java:302)
    at app//com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.doForeignObject(JSToPrimitiveNode.java:252)
    at app//com.oracle.truffle.js.nodes.cast.JSToPrimitiveNodeGen.execute(JSToPrimitiveNodeGen.java:111)
    at app//com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.doOther(JSToPropertyKeyNode.java:82)
    at app//com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNodeGen.execute(JSToPropertyKeyNodeGen.java:47)
    at app//com.oracle.truffle.js.nodes.cast.ToArrayIndexNode.doNonArrayIndex(ToArrayIndexNode.java:175)
    at app//com.oracle.truffle.js.nodes.cast.ToArrayIndexNodeGen.execute(ToArrayIndexNodeGen.java:135)
    at app//com.oracle.truffle.js.nodes.access.CachedGetPropertyNode.doGeneric(CachedGetPropertyNode.java:116)
    at app//com.oracle.truffle.js.nodes.access.CachedGetPropertyNodeGen.execute(CachedGetPropertyNodeGen.java:83)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode$JSObjectReadElementNonArrayTypeCacheNode.execute(ReadElementNode.java:812)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode$JSObjectReadElementTypeCacheNode.readNonArrayObjectIndex(ReadElementNode.java:675)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode$JSObjectReadElementTypeCacheNode.executeWithTargetAndIndexUnchecked(ReadElementNode.java:662)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode.executeTypeDispatch(ReadElementNode.java:335)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode.executeWithTargetAndIndex(ReadElementNode.java:303)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode.executeWithTarget(ReadElementNode.java:230)
    at app//com.oracle.truffle.js.nodes.access.ReadElementNode.execute(ReadElementNode.java:186)
    at app//com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNodeGen.executeBoolean_generic4(JSToBooleanUnaryNodeGen.java:170)
    at app//com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNodeGen.executeBoolean(JSToBooleanUnaryNodeGen.java:108)
    at app//com.oracle.truffle.js.nodes.control.IfNode.executeCondition(IfNode.java:236)
    at app//com.oracle.truffle.js.nodes.control.IfNode.executeVoid(IfNode.java:176)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(AbstractBlockNode.java:80)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(AbstractBlockNode.java:55)
    at platform/jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime.OptimizedBlockNode.executeVoid(OptimizedBlockNode.java:124)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(AbstractBlockNode.java:70)
    at app//com.oracle.truffle.js.nodes.control.VoidBlockNode.execute(VoidBlockNode.java:61)
    at app//com.oracle.truffle.js.nodes.control.ReturnTargetNode$FrameReturnTargetNode.execute(ReturnTargetNode.java:124)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(AbstractBlockNode.java:85)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(AbstractBlockNode.java:55)
    at platform/jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime.OptimizedBlockNode.executeGeneric(OptimizedBlockNode.java:81)
    at app//com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(AbstractBlockNode.java:75)
    at app//com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(FunctionBodyNode.java:73)
    at app//com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(FunctionRootNode.java:143)
    at app//com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(JavaScriptRealmBoundaryRootNode.java:92)
    at <js>.Require(Unknown)
    at <js>.this.require(jvm-npm.js:46)
    at <js>.:anonymous(/<project-root>/node_modules/rxjs/dist/cjs/internal/operators/pairwise.js:5)
    at <js>._load(jvm-npm.js:73)
    at <js>.Require(jvm-npm.js:98)
    at <js>.this.require(jvm-npm.js:46)
    at <js>.:anonymous(/<project-root>/node_modules/rxjs/dist/cjs/index.js:254)
    at <js>._load(jvm-npm.js:73)
    at <js>.Require(jvm-npm.js:98)
    at <js>.this.require(jvm-npm.js:46)
    at <js>.:anonymous(/<project-root>/node_modules/test-verticle/observables.js:11)
    at <js>._load(jvm-npm.js:73)
    at <js>.Require(jvm-npm.js:98)
    at <js>.this.require(jvm-npm.js:46)
    at <js>.:anonymous(/<project-root>/node_modules/test-verticle/someModule.js:9)
```

## verticle implementation
The source code for the test verticle sends an event bus message to some address and waits for a response before it will signal that its deployment is complete.

The junit test cases provide the address configuration to the test verticle, and in `deploysOne`, the junit test case implements a consumer that will immediately respond as soon as the test verticle starts its deployment and sends its message. If the verticle loads and completes deployment, that means it was able to process all of its require statements and complete its messaging.

In the `deploysTwo` test case, the junit driver provides address configuration to two deployments of the same verticle, expecting both of them to deploy successfully, which can only happen if both instances successfully process all of their require statements and complete their messaging with each other during their `deploy` events.
