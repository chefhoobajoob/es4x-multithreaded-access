/* global Java */
/* global vertx */

const logger = Java.type('org.slf4j.LoggerFactory').getLogger('org.acme.es4x.test.verticle-01')
logger.info('loading someModule')
const someModule = require('./someModule')
logger.info('done loading modules')

process.on('deploy', deploy => {
  const setup = JSON.parse(vertx.getOrCreateContext().config().encode())
  logger.info(`>>> starting with config: ${JSON.stringify(setup)}`)
  someModule.listen(setup.myAddress, setup.myName)
  someModule.waitFor(setup.theirAddress, setup.theirName, setup.myName)
  .subscribe({
    next: (hello) => {
      logger.info(`>>> got a hello message: ${JSON.stringify(hello)}`)
      logger.info('>>> fully deployed')
      deploy.complete()
    },
    error: (error) => {
      logger.error('>>> something bad happened', error)
      logger.error('>>> failing deployment')
      deploy.fail(error.getMessage())
    }
  })
})

process.on('undeploy', undeploy => {
  logger.info('>>> stopping')
  someModule.stop()
  logger.info('>>> stopped')
  undeploy.complete()
})