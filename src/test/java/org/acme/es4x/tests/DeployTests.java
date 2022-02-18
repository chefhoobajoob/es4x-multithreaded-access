package org.acme.es4x.tests;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeployTests {
    Logger _logger = LoggerFactory.getLogger( "org.acme.es4x.tests.main" );

    @Test
    @Order(1)
    public void deploysOne( Vertx theVertx, VertxTestContext theContext ) {
        _logger = LoggerFactory.getLogger( "org.acme.es4x.tests.main.deploysOne" );
        String v1Name = "js:node_modules/test-verticle/main.js";
        JsonObject v1Config = new JsonObject()
            .put("myAddress", "org.acme.es4x.verticle.01")
            .put("myName", "Kendrick")
            .put("theirAddress", _logger.getName())
            .put("theirName", "BbyMutha");
        DeploymentOptions v1Options = new DeploymentOptions().setConfig( v1Config );

        JsonObject hello = new JsonObject().put("helloFrom", v1Config.getString("theirName"));
        MessageConsumer<JsonObject> consumer = theVertx.eventBus().consumer( _logger.getName(), message -> {
            _logger.info(">>> got a hello message: {}", message.body().encode());
            _logger.info(">>> sending a response: {}", hello.encode());
            message.reply(hello);
        });

        Checkpoint v1Deployed = theContext.checkpoint();
        Checkpoint v1Undeployed = theContext.checkpoint();
        _logger.info(">>> launching verticle 1");
        theVertx.deployVerticle( v1Name, v1Options, deploy -> {
            if ( deploy.failed() ) {
                _logger.error("couldn't deploy verticle 1", deploy.cause());
                theContext.failNow( deploy.cause() );
                consumer.unregister();
                return;
            }
            v1Deployed.flag();
            theVertx.undeploy( deploy.result(), (ignored) -> {
                consumer.unregister();
                v1Undeployed.flag();
            } );
        });
    }

    @Test
    @Order(2)
    @Timeout(value=3, timeUnit = TimeUnit.MINUTES)
    public void deploysTwoOverlapping( Vertx theVertx, VertxTestContext theContext ) {
        _logger = LoggerFactory.getLogger( "org.acme.es4x.tests.main.deploysTwoOverlapping" );
        deployTwo( theVertx, theContext, 1000L );
    }

    @Test
    @Order(3)
    @Timeout(value=3, timeUnit = TimeUnit.MINUTES)
    public void deploysOneThenTwo( Vertx theVertx, VertxTestContext theContext ) {
        _logger = LoggerFactory.getLogger( "org.acme.es4x.tests.main.deploysOneThenTwo" );
        deployTwo( theVertx, theContext, 10*1000L );
    }

    private void deployTwo( Vertx theVertx, VertxTestContext theContext, Long theWaitTime ) {
        String v1Name = "js:node_modules/test-verticle/main.js";
        JsonObject v1Config = new JsonObject()
            .put("myAddress", "org.acme.es4x.verticle.01")
            .put("myName", "Kendrick")
            .put("theirAddress", "org.acme.es4x.verticle.02")
            .put("theirName", "BbyMutha");
        DeploymentOptions v1Options = new DeploymentOptions().setConfig( v1Config );

        String v2Name = "js:node_modules/test-verticle/main.js";
        JsonObject v2Config = new JsonObject()
            .put("myAddress", v1Config.getString("theirAddress"))
            .put("myName", v1Config.getString("theirName"))
            .put("theirAddress", v1Config.getString("myAddress"))
            .put("theirName", v1Config.getString("myName"));
        DeploymentOptions v2Options = new DeploymentOptions().setConfig( v2Config );

        Checkpoint v1Deployed = theContext.checkpoint();
        Checkpoint v1Undeployed = theContext.checkpoint();
        _logger.info(">>> launching verticle 1");
        theVertx.deployVerticle( v1Name, v1Options, deploy -> {
            if ( deploy.failed() ) {
                _logger.error("couldn't deploy verticle 1", deploy.cause());
                theContext.failNow( deploy.cause() );
                return;
            }
            v1Deployed.flag();
            theVertx.undeploy( deploy.result(), (ignored) -> v1Undeployed.flag() );
        });

        Checkpoint v2Deployed = theContext.checkpoint();
        Checkpoint v2Undeployed = theContext.checkpoint();
        theVertx.setTimer( theWaitTime, tid -> {
            _logger.info(">>> launching verticle 2");
            theVertx.deployVerticle( v2Name, v2Options, deploy -> {
                if ( deploy.failed() ) {
                    _logger.error("couldn't deploy verticle 2", deploy.cause());
                    theContext.failNow( deploy.cause() );
                    return;
                }
                v2Deployed.flag();
                theVertx.undeploy( deploy.result(), (ignored) -> v2Undeployed.flag() );
            });
        } );
    }
}
