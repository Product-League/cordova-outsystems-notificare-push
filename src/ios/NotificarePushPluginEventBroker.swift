import NotificareKit

class NotificarePushPluginEventBroker {

    typealias Consumer = (_ event: Event) -> Void

    private static var eventQueue = [Event]()
    private static var consumer: Consumer?

    static func startListening(_ consumer: @escaping Consumer) {
        self.consumer = consumer

        if !eventQueue.isEmpty {
            NotificareLogger.debug("Processing event queue with ${eventQueue.size} items.")
            eventQueue.forEach { consumer($0) }
            eventQueue.removeAll()
        }
    }

    static func dispatchEvent(name: String, payload: Any?) {
        let event = Event(name: name, payload: payload)

        guard let consumer = consumer else {
            eventQueue.append(event)
            return
        }

        consumer(event)
    }

    private init() {}

    struct Event {
        let name: String
        let payload: Any?
    }
}
