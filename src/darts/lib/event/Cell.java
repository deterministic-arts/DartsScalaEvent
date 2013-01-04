package darts.lib.event;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
class Cell<T> {

	private static final AtomicReferenceFieldUpdater<Cell,Object> UPDATER = AtomicReferenceFieldUpdater.newUpdater(Cell.class, Object.class, "slot");
	
	private volatile T slot;
	
	Cell(T value) {
		slot = value;
	}
	
	protected T get() {
		return (T)UPDATER.get(this);
	}
	
	protected boolean cas(T old, T rep) {
		return UPDATER.compareAndSet(this, old, rep);
	}
}
