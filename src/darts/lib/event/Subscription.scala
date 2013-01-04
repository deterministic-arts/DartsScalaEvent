package darts.lib.event

trait Subscription {
    
    /**
     * Cancel this subscription. The listener is no
     * longer invoked by events. Calling this method is
     * generally safe, even from within an event listener
     * (and in particular, from within the listener being
     * represented by this subcription handle).
     * 
     * Note, however, that 
     */
    
    def cancel(): Boolean
}
