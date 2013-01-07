package darts.lib.event

import scala.annotation.tailrec
 
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicReference
import java.lang.ref.WeakReference

trait Once[E] {
    
    /**
     * Type of the event published by this instance
     */
    
    type Event = E
    
    /**
     * Type of listener function for this publisher
     */
    
    type Listener = Function1[E,Any]
    
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

trait Trigger[E] extends Once[E] with Dispatcher[E]

object Trigger {

	def apply[E](): Trigger[E] = new BasicTrigger[E]
}

object BasicTrigger {
    

}

class BasicTrigger[E] 
extends Trigger[E] {
    
    private val cell = new AtomicReference[State](State.Initial)
    private def getState = cell.get
    private def setState(s1: State, s2: State): Boolean = cell.compareAndSet(s1, s2)
    
    private def unregister(e: Entry) {
        @tailrec def loop(state: State): Unit = state match {
            case State.Initial => ()
            case State.Signalled(_) => ()
            case State.Pending(list) => {
            	val newList = list.filter(p => p != e && p.isAlive)
    			if (!setState(state, State.Pending(newList))) loop(getState)
            }
        }
        loop(getState)
    }
   
    private def register(e: Entry): Either[E,Entry] = {
        @tailrec def loop(state: State): Either[E,Entry] = state match {
            case State.Signalled(value) => Left(value)
            case State.Pending(list) => if (setState(state, State.Pending(e :: list))) Right(e) else loop(getState)
            case State.Initial => if (setState(state, State.Pending(List(e)))) Right(e) else loop(getState)
        }
        loop(getState)
    }
    
    protected def handleError(error: Throwable, event: E, subscription: Subscription): Unit =
        throw error
    
    def value: Option[E] = getState match {
        case State.Signalled(value) => Some(value)
        case _ => None
    }
        
    def apply(event: E) {
        @tailrec def loop(list: List[Entry]) {
            if (!list.isEmpty) {
                try list.head.invoke(event)
                catch {
                    case thr => handleError(thr, event, list.head)
                }
                loop(list.tail)
            }
        }
        @tailrec def setsignalled(state: State): Unit = state match {
            case State.Initial => if (!setState(state, State.Signalled(event))) setsignalled(getState)
            case State.Pending(list) => if (setState(state, State.Signalled(event))) loop(list) else setsignalled(getState)
            case State.Signalled(_) => throw new IllegalStateException
        }
        setsignalled(getState)
    }
    
    def subscribe(listener: E=>Any, retention: Retention): Either[E,Subscription] = {
        val reference = retention match {
            case Retention.Strong => new StrongRef(listener)
            case Retention.Weak => new WeakRef(listener)
        }
        val entry = new Entry(reference)
        register(entry)
    }
 
    private sealed trait State
    private object State {
       
        final case class Signalled(val state: E) extends State
        final case object Initial extends State
        final case class Pending(val listeners: List[Entry]) extends State
    }
    
    private final class Entry(reference: ListenerRef[E]) 
    extends Cell[ListenerRef[E]](reference) 
    with Subscription {
    
        private def exchg: ListenerRef[E] = {
            @tailrec def loop(old: ListenerRef[E]): ListenerRef[E] = 
                if (old == NullRef) old
                else if (cas(old, NullRef)) old
                else loop(get)
            loop(get)
        }
        
    	private[BasicTrigger] def isAlive: Boolean = get.isAlive
		private[BasicTrigger] def invoke(event: E): Unit = exchg.invoke(event)
        
        def cancel(): Boolean = {
            if (exchg == NullRef) false else { unregister(this); true }
        }
    	
    	override def toString: String = 
    	    "Subscription(" + get + ")"
    }
}