package darts.lib.event

trait Dispatcher[E] {

    /**
     * Notify registered subscribers, that an event
     * occurred. Detail information can be gathered from
     * the given `event` object.
     */
    
    def apply(event: E): Unit
}