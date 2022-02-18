/* global Java */
/* global vertx */

const logger = Java.type('org.slf4j.LoggerFactory').getLogger('org.acme.internal.observables')
logger.debug('loading @vertx/core/options')
const { DeliveryOptions } = require('@vertx/core/options')
logger.debug('loading rxjs')
const { Observable, of, throwError } = require('rxjs')
logger.debug('loading rxjs/operators')
const { tap, map, mergeMap, reduce } = require('rxjs/operators')
logger.debug('done loading modules')

const eventBus = vertx.eventBus()

module.exports.toRx = (vertxApiFunction, ...functionArguments) =>
  new Observable((subscriber) => {
    vertxApiFunction(...functionArguments, (response) => {
      if (response.failed()) {
        subscriber.error(response.cause())
        return
      }
      subscriber.next(response.result())
      subscriber.complete()
    })
  })

module.exports.sendRequest = (address, payload, options = new DeliveryOptions()) => {
  logger.debug(
    `Sending message to event bus address [${address}]` +
    ` with options ${JSON.stringify(options.toJson())}` +
    ` and payload ${JSON.stringify(payload)}`
  )
  return module.exports.toRx(eventBus.request, address, payload, options)
  .pipe(
    tap({
      next: (message) => logger.debug(`Received a response from [${address}]: ${message.body().encode()}`),
      error: (error) => logger.debug(`Event bus address [${address}] encountered a problem: ${error.getMessage()}`)
    }),
    map(message => JSON.parse(message.body().encode()))
  )
}
