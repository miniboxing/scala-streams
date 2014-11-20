package streams

import scala.reflect.ClassTag
import scala.Array
import scala.collection.mutable.ArrayBuffer

trait MbFunction1[-T1, +R] {
  def apply(t1: T1): R
}

trait MbFunction2[-T1, -T2, +R] {
  def apply(t1: T1, t2: T2): R
}

trait Numeric[T] {
  def plus(x: T, y: T): T
  def zero: T
}

object `package` {
  implicit object LongIsNumeric extends Numeric[Long] {
    def plus(t1: Long, t2: Long): Long = t1 + t2
    def zero: Long = 0l
  }
}

final class Stream[T: ClassTag](val streamf: MbFunction1[MbFunction1[T, Boolean], Unit]) {

  // most likely, defining these closures in a miniboxed environment will trigger
  // bug #114 (https://github.com/miniboxing/miniboxing-plugin/issues/114) which
  // will make performance be even worse than generic:

  def toArray(): Array[T] =
    (foldLeft(new ArrayBuffer[T])(new MbFunction2[ArrayBuffer[T], T, ArrayBuffer[T]] {
      def apply(a: ArrayBuffer[T], value: T) = a += value
    })).toArray

  def filter(p: MbFunction1[T, Boolean]): Stream[T] =
    new Stream(
      new MbFunction1[MbFunction1[T, Boolean], Unit] {
        def apply(iterf: MbFunction1[T, Boolean]) =
          streamf(new MbFunction1[T, Boolean] {
            def apply(value: T): Boolean = !p(value) || iterf(value)
          })
      })

  def map[R: ClassTag](f: MbFunction1[T, R]): Stream[R] =
    new Stream(
      new MbFunction1[MbFunction1[R, Boolean], Unit] {
        def apply(iterf: MbFunction1[R, Boolean]) =
          streamf(new MbFunction1[T, Boolean] {
            def apply(value: T) = iterf(f(value))
          })
      })

  def takeWhile(p: MbFunction1[T, Boolean]): Stream[T] =
    new Stream(
      new MbFunction1[MbFunction1[T, Boolean], Unit] {
        def apply(iterf: MbFunction1[T, Boolean]) =
          streamf(new MbFunction1[T, Boolean] {
            def apply(value: T): Boolean = if (p(value)) iterf(value) else false
          })
      })

  def skipWhile(p: MbFunction1[T, Boolean]): Stream[T] =
    new Stream(new MbFunction1[MbFunction1[T, Boolean], Unit] {
      def apply(iterf: MbFunction1[T, Boolean]) =
        streamf(new MbFunction1[T, Boolean] {
          def apply(value: T): Boolean = {
            var shortcut = true;
            if (!shortcut && p(value)) {
              true
            } else {
              shortcut = true
              iterf(value)
            }
          }
        })
    })

  def skip(n: Int): Stream[T] = {
    var count = 0
    new Stream(new MbFunction1[MbFunction1[T, Boolean], Unit] {
      def apply(iterf: MbFunction1[T, Boolean]) =
        streamf(new MbFunction1[T, Boolean] {
          def apply(value: T): Boolean = {
            count += 1
            if (count > n) {
              iterf(value)
            } else {
              true
            }
          }
        })
    })
  }

  def take(n: Int): Stream[T] = {
    var count = 0
    new Stream(new MbFunction1[MbFunction1[T, Boolean], Unit] {
      def apply(iterf: MbFunction1[T, Boolean]) =
        streamf(new MbFunction1[T, Boolean] {
          def apply(value: T): Boolean = {
            count += 1
            if (count <= n) {
              iterf(value)
            } else {
              false
            }
          }
        })
    })
  }

  def flatMap[R: ClassTag](f: MbFunction1[T, Stream[R]]): Stream[R] =
    new Stream(new MbFunction1[MbFunction1[R, Boolean], Unit] {
      def apply(iterf: MbFunction1[R, Boolean]) =
        streamf(new MbFunction1[T, Boolean] {
          def apply(value: T): Boolean = {
            val innerf = f(value).streamf
            innerf(iterf)
            true
          }
        })
    })

  def foldLeft[A](a: A)(op: MbFunction2[A, T, A]): A = {
    var acc = a
    streamf(new MbFunction1[T, Boolean] {
      def apply(value: T) = {
        acc = op(acc, value)
        true
      }
    })
    acc
  }

  def fold(z: T)(op: MbFunction2[T, T, T]): T = foldLeft(z)(op)

  def size(): Long = foldLeft(0)(new MbFunction2[Int, T, Int] {
    def apply(a: Int, t: T): Int = a + 1
  })

  def sum[N >: T](implicit num: Numeric[N]): N =
    foldLeft(num.zero)(new MbFunction2[N, T, N] {
      def apply(t1: N, t2: T): N = num.plus(t1, t2)
    })
}

object Stream {
  @inline def apply[T: ClassTag](xs: Array[T]) = {
    val gen = new MbFunction1[MbFunction1[T, Boolean], Unit] {
      def apply(iterf: MbFunction1[T, Boolean]): Unit = {
        var counter = 0
        var cont = true
        val size = xs.length
        while (counter < size && cont) {
          cont = iterf(xs(counter))
          counter += 1
        }
      }
    }
    new Stream(gen)
  }
}
