/* global Java */
/* global vertx */
/* global config */

const setup = JSON.parse(config.encode())
const logger = Java.type('org.slf4j.LoggerFactory').getLogger(`${setup.myAddress}.verticle`)
logger.info('loading someModule')
const someModule = require('./someModule')
logger.info('done loading modules')

process.on('deploy', deploy => {
  logger.info(`>>> deploying with config: ${JSON.stringify(setup)}`)
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