package darts.lib.event

sealed abstract class Retention

object Retention {
    
    final case object Strong extends Retention 
    final case object Weak extends Retention
}
