/* global Java */
/* global vertx */

const logger = Java.type('org.slf4j.LoggerFactory').getLogger('org.acme.es4x.test.verticle-01.someModule')
logger.info('loading mock-internal/observables')
const { sendRequest } = require('mock-internal/observables')
logger.info('loading rxjs')
const { of, defer } = require('rxjs')
logger.info('loading rxjs/operators')
const { tap, mergeMap, retryWhen, delayWhen, timer } = require('rxjs/operators')
logger.info('done loading modules')

let consumer
module.exports.listen = (myAddress, myName) => {
  if (consumer) {
    logger.warn('ignoring listen request: already listening')
    return
  }
  const hello = {helloFrom: myName}
  consumer = vertx.eventBus().consumer(myAddress, message => {
    logger.info(`got a hello message: ${message.body().encode()}`)
    logger.info(`sending reply: ${hello}`)
    message.reply(hello)
    logger.info('reply sent')
  })
  logger.info(`now listening for hello requests on ${myAddress}`)
}

module.exports.waitFor = (theirAddress, theirName, myName) => {
  logger.info(`waiting for (${theirName})`)
  return defer(() => sendRequest(theirAddress, { helloFrom: myName }))
  .pipe(
    tap({
      next: () => logger.info(`got a response from (${theirName}) at (${theirAddress})`),
      error: (error) => logger.warn(`hello request to (${theirName}) failed`, error)
    }),
    retryWhen(errors => errors.pipe(
      delayWhen(() => {
        logger.info('trying again in 3s')
        return timer(3000)
      })
    ))
  )
}

module.exports.stop = () => {
  logger.info('unregistering consumer')
  consumer && consumer.unregister()
  logger.info('consumer unregistered')
}