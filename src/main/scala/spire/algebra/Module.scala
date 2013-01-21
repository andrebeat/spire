package spire.algebra

import spire.macrosk.Ops
import spire.NoImplicit

import scala.{ specialized => spec }
import scala.collection.SeqLike
import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

/**
 * A module generalizes a vector space by requiring its scalar need only form
 * a ring, rather than a field. In Spire, a `Module` is a left R-Module.
 */
trait Module[V, @spec(Int,Long,Float,Double) R] extends AdditiveAbGroup[V] {
  implicit def scalar: Ring[R] // TODO: Can this be Rng[R] instead?

  def timesl(r: R, v: V): V
}

/**
 * An R-module whose scalar multiplication comes from the right.
 */
trait RightModule[V, @spec(Int,Long,Float,Double) R] extends AdditiveAbGroup[V] {
  implicit def scalar: Ring[R]

  def timesr(v: V, r: R): V
}

object Module extends Module2 {
  @inline final def apply[V, @spec(Int,Long,Float,Double) R](implicit V: Module[V, R]) = V
}

final class ModuleOps[V, F](rhs: V)(implicit ev: Module[V, F]) {
  def *: (lhs:F): V = macro Ops.rbinop[F, V]
}

final class RightModuleOps[V, F](lhs: V)(implicit ev: RightModule[V, F]) {
  def :* (rhs:F): V = macro Ops.binop[F, V]
}

trait Module0 {

  // Note that, unfortunately, I must put a NoImplicit implicit here. I think
  // this is because Module[CC[A], A] is more specific than Module[V, A], so
  // this takes precendence over the VectorSpaceIsModule implicit.

  implicit def seq[A, CC[A] <: SeqLike[A, CC[A]]](implicit
      ring0: Ring[A], cbf0: CanBuildFrom[CC[A], A, CC[A]],
      ev: NoImplicit[VectorSpace[CC[A], A]]) = new SeqModule[A, CC[A]] {
    val scalar = ring0
    val cbf = cbf0
  }

  implicit def MapModule[K, V](implicit V0: Ring[V],
      ev: NoImplicit[VectorSpace[Map[K, V], V]]) = new MapRng[K, V] {
    val scalar = Ring[V]
  }
}

trait Module1 extends Module0 {
  implicit def IdentityModule[@spec(Int,Long,Float,Double) V](implicit ring: Ring[V]) = {
    new IdentityModule[V] {
      val scalar = ring
    }
  }

  implicit def ArrayModule[@spec(Int,Long,Float,Double) A](implicit
      ev: NoImplicit[VectorSpace[Array[A], A]], classTag0: ClassTag[A],
      scalar0: Ring[A]): Module[Array[A], A] = new ArrayModule[A] {
    val scalar = scalar0
    val classTag = classTag0
  }
}

trait Module2 extends Module1 {
  implicit def VectorSpaceIsModule[V,@spec(Int,Long,Float,Double) R](implicit
    vs: VectorSpace[V, R]): Module[V, R] = vs
}

trait IdentityModule[@spec(Int,Long,Float,Double) V] extends Module[V, V] {
  def zero = scalar.zero
  def negate(v: V) = scalar.negate(v)
  def plus(v: V, w: V): V = scalar.plus(v, w)
  override def minus(v: V, w: V): V = scalar.minus(v, w)

  def timesl(r: V, v: V): V = scalar.times(r, v)
}

trait ArrayModule[@spec(Int,Long,Float,Double) A] extends Module[Array[A], A] {
  implicit def classTag: ClassTag[A]

  def zero: Array[A] = new Array[A](0)

  def negate(x: Array[A]): Array[A] = {
    val y = new Array[A](x.length)
    var i = 0
    while (i < x.length) {
      y(i) = scalar.negate(x(i))
      i += 1
    }
    y
  }

  def plus(x: Array[A], y: Array[A]): Array[A] = {
    var prefix = math.min(x.length, y.length)
    var z = new Array[A](math.max(x.length, y.length))
    var i = 0
    while (i < prefix) {
      z(i) = scalar.plus(x(i), y(i))
      i += 1
    }
    while (i < x.length) { z(i) = x(i); i += 1 }
    while (i < y.length) { z(i) = y(i); i += 1 }
    z
  }

  override def minus(x: Array[A], y: Array[A]): Array[A] = {
    var prefix = math.min(x.length, y.length)
    var z = new Array[A](math.max(x.length, y.length))
    var i = 0
    while (i < prefix) {
      z(i) = scalar.minus(x(i), y(i))
      i += 1
    }
    while (i < x.length) { z(i) = x(i); i += 1 }
    while (i < y.length) { z(i) = scalar.negate(y(i)); i += 1 }
    z
  }

  def timesl(r: A, x: Array[A]): Array[A] = {
    val y = new Array[A](x.length)
    var i = 0
    while (i < y.length) {
      y(i) = scalar.times(r, x(i))
      i += 1
    }
    y
  }
}
