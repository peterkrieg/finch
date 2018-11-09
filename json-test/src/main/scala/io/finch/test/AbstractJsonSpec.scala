package io.finch.test

import java.nio.charset.{ Charset, StandardCharsets }

import cats.{ Comonad, Eq, MonadError }
import cats.instances.AllInstances
import io.circe.Decoder
import io.finch.{ Decode, Encode }
import io.finch.iteratee.Enumerate
import io.finch.test.data._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.prop.Checkers
import org.typelevel.discipline.Laws
import scala.util.Try

abstract class AbstractJsonSpec extends FlatSpec with Matchers with Checkers with AllInstances {

  implicit val comonadEither: Comonad[Try] = new Comonad[Try] {
    def extract[A](x: Try[A]): A = x.get //never do it in production, kids

    def coflatMap[A, B](fa: Try[A])(f: Try[A] => B): Try[B] = Try(f(fa))

    def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)
  }

  implicit def arbitraryCharset: Arbitrary[Charset] = Arbitrary(
    Gen.oneOf(
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16,
      Charset.forName("UTF-32")
    )
  )

  implicit def arbitraryException: Arbitrary[Exception] = Arbitrary(
    Arbitrary.arbitrary[String].map(s => new Exception(s))
  )

  implicit def eqException: Eq[Exception] = Eq.instance((a, b) => a.getMessage == b.getMessage)

  implicit def decodeException: Decoder[Exception] =
    Decoder.forProduct1[Exception, String]("message")(s => new Exception(s))

  private def loop(name: String, ruleSet: Laws#RuleSet, library: String): Unit =
    for ((id, prop) <- ruleSet.all.properties) it should (s"$library.$id.$name") in { check(prop) }

  def checkJson(library: String)(
    implicit
    e: Encode.Json[List[ExampleNestedCaseClass]],
    d: Decode.Json[List[ExampleNestedCaseClass]]
  ): Unit = {
    loop("List[ExampleNestedCaseClass]", JsonLaws.encoding[List[ExampleNestedCaseClass]].all, library)
    loop("List[ExampleNestedCaseClass]", JsonLaws.decoding[List[ExampleNestedCaseClass]].all, library)
  }

  def checkEnumerateJson[F[_]: Comonad](library: String)(
    implicit
    en: Enumerate.Json[F, ExampleNestedCaseClass],
    monadError: MonadError[F, Throwable]
  ): Unit = {
    loop("ExampleNestedCaseClass", JsonLaws.enumerating[F, ExampleNestedCaseClass].all, library)
  }
}
