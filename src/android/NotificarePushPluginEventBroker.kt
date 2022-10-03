package re.notifica.push.cordova

import re.notifica.internal.NotificareLogger

internal object NotificarePushPluginEventBroker {

    private val eventQueue = mutableListOf<Event>()
    private var consumer: Consumer? = null

    fun dispatchEvent(name: String, payload: Any?) {
        val event = Event(name, payload)

        val consumer = consumer ?: run {
            eventQueue.add(event)
            return
        }

        consumer.onEvent(event)
    }

    fun setup(consumer: Consumer) {
        this.consumer = consumer

        if (eventQueue.isNotEmpty()) {
            NotificareLogger.debug("Processing event queue with ${eventQueue.size} items.")
            eventQueue.forEach { consumer.onEvent(it) }
            eventQueue.clear()
        }
    }


    data class Event(
        val name: String,
        val payload: Any?,
    )

    interface Consumer {
        fun onEvent(event: Event)
    }
}
