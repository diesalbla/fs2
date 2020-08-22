package fs2.io

import scala.jdk.CollectionConverters._

private[fs2] object CollectionCompat {
  implicit class JIteratorOps[A](private val self: java.util.Iterator[A]) extends AnyVal {
    def asScala: Iterator[A] = IteratorHasAsScala(self).asScala
  }
  implicit class JIterableOps[A](private val self: java.lang.Iterable[A]) extends AnyVal {
    def asScala: Iterable[A] = IterableHasAsScala(self).asScala
  }
  implicit class JSetOps[A](private val self: java.util.Set[A]) extends AnyVal {
    def asScala: Set[A] = SetHasAsScala(self).asScala.toSet
  }
  implicit class ListOps[A](private val self: List[A]) extends AnyVal {
    def asJava: java.util.List[A] = SeqHasAsJava(self).asJava
  }
  implicit class SetOps[A](private val self: Set[A]) extends AnyVal {
    def asJava: java.util.Set[A] = SetHasAsJava(self).asJava
  }
  implicit class EnumerationOps[A](private val self: java.util.Enumeration[A]) extends AnyVal {
    def asScala: Iterator[A] = EnumerationHasAsScala(self).asScala
  }
}
