package darts.lib.event

import scala.annotation.tailrec
 
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicReference
import java.lang.ref.WeakReference

trait Signal[E] extends Publisher[E] with Dispatcher[E]

object Signal {

	def apply[E](): Signal[E] = new BasicSignal[E]
}


private sealed trait ListenerRef[-E] {
    def isAlive: Boolean
    def invoke(event: E): Unit
}

private final object NullRef 
extends ListenerRef[Any] {
    def isAlive: Boolean = false
    def invoke(event: Any): Unit = ()
    override def toString: String = "ListenerRef(<cancelled>)"
}

private final class StrongRef[E] (val listener: E=>Any) 
extends ListenerRef[E] {
    def isAlive: Boolean = true 
    def invoke(event: E): Unit = listener(event)
    override def toString: String = "ListenerRef(" + listener + ")"
}

private final class WeakRef[E] (listener: E=>Any) 
extends WeakReference[E=>Any] (listener) with ListenerRef[E] {
    def isAlive: Boolean = null != get 
    def invoke(event: E): Unit = get match {
        case null => ()
        case func => func(event)
    }
    override def toString: String = get match {
        case null => "ListenerRef(<dead>)"
        case func => "ListenerRef(" + func + ")"
    }
}


class BasicSignal[E] 
extends Signal[E] {
    
    private val listeners = new AtomicReference[List[Entry]](List())
    
    private def unregister(e: Entry) {
        @tailrec def loop(list: List[Entry]) {
            val newList = list.filter(p => p != e && p.isAlive)
            if (!listeners.compareAndSet(list, newList)) loop(listeners.get)
        }
        loop(listeners.get)
    }
   
    private def register(e: Entry) {
        @tailrec def loop(list: List[Entry]) {
            val newList = e :: list
            if (!listeners.compareAndSet(list, newList)) loop(listeners.get)
        }
        loop(listeners.get)
    }
    
    protected def handleError(error: Throwable, event: E, subscription: Subscription): Unit =
        throw error
    
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
        loop(listeners.get)
    }
    
    def subscribe(listener: E=>Any, retention: Retention): Subscription = {
        val reference = retention match {
            case Retention.Strong => new StrongRef(listener)
            case Retention.Weak => new WeakRef(listener)
        }
        val entry = new Entry(reference)
        register(entry)
        entry
    }
    
    private final class Entry(reference: ListenerRef[E]) 
    extends Cell[ListenerRef[E]](reference) 
    with Subscription {
        
        private def doCancel: Boolean = {
    		@tailrec def loop(old: ListenerRef[E]): Boolean = 
				if (old == NullRef) false
				else if (cas(old, NullRef)) true
				else loop(get)
			loop(get)
    	}
    
    	private[BasicSignal] def isAlive: Boolean = get.isAlive
		private[BasicSignal] def invoke(event: E): Unit = get.invoke(event)
        
        def cancel(): Boolean = {
            if (!doCancel) false else { unregister(this); true }
        }
    	
    	override def toString: String = 
    	    "Subscription(" + get + ")"
    }
}