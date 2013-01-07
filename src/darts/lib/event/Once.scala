package darts.lib.event

import scala.annotation.tailrec
 
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicReference
import java.lang.ref.WeakReference

/**
 * A "once" is a special kind of publisher, which can
 * be triggered at most once. 
 */

trait Once[E] {
    
    /**
     * Type of the event published by this instance
     */
    
    type Event = E
    
    /**
     * Type of listener function for this publisher
     */
    
    type Listener = Function1[E,Any]
    
    /**
     * This instance's value, if it has already been fired,
     * and `None` otherwise.
     */
    
    def value: Option[E]

    /**
     * Adds a listener to this publisher. Depending on the
     * value of `retention`, the listener will be tracked
     * using a strong or a weak reference. The function
     * returns a "subscription" object, which can later be
     * used to undo the subscription, if notifications
     * of events are no longer desired.
     * 
     * If this trigger has already been called, then the 
     * listener is not registered.
     * 
     * This method returns either a subscription handle, if
     * the callback has been registered, or the value, with
     * which this trigger has been fired. Note, that if this
     * method returns a subscription, then the listener is
     * guaranteed to be called if the trigger is fired, even
     * if that happens on a different thread running concurrently
     * with the thread registering the listener.
     * 
     * Also note, that a registered listener is called at
     * most once for a given trigger. After being called (or
     * cancelled) the listener becomes eligible for garbage
     * collection.
     * 
     * @param listener	listener function to register
     * @param retention	reference retention policy
     * 
     * @returns	a "handle" representing the subscription
     * 			made; the handle may be used later to unregister
     * 			the listener from this publisher
     */
    
    def subscribe(listener: Listener, retention: Retention): Either[E,Subscription]

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
    
    def subscribe(listener: Listener): Either[E,Subscription] = subscribe(listener, Retention.Strong)
}