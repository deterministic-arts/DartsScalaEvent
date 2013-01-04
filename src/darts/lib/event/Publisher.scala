package darts.lib.event

/**
 * Event publisher. A publisher is basically some entity,
 * you can subscribe listeners to in order to be notified
 * of events.
 */

trait Publisher[E] {

    /**
     * Type of the event published by this instance
     */
    
    type Event = E
    
    /**
     * Type of listener function for this publisher
     */
    
    type Listener = Function1[E,Any]

    /**
     * Adds a listener to this publisher. Depending on the
     * value of `retention`, the listener will be tracked
     * using a strong or a weak reference. The function
     * returns a "subscription" object, which can later be
     * used to undo the subscription, if notifications
     * of events are no longer desired.
     * 
     * @param listener	listener function to register
     * @param retention	reference retention policy
     * 
     * @returns	a "handle" representing the subscription
     * 			made; the handle may be used later to unregister
     * 			the listener from this publisher
     */
    
    def subscribe(listener: Listener, retention: Retention): Subscription

    /**
     * Adds a listener to this publisher. The listener will
     * always be tracked using a strong reference. The 
     * function returns a "subscription" object, which can 
     * later be used to undo the subscription, if notifications
     * of events are no longer desired.
     * 
     * @param listener	listener function to register
     * 
     * @returns	a "handle" representing the subscription
     * 			made; the handle may be used later to unregister
     * 			the listener from this publisher
     */
    
    def subscribe(listener: Listener): Subscription = subscribe(listener, Retention.Strong)
}

