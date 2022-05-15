package com.odenzo.demo.httpclient

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import org.http4s.client.Client
import com.odenzo.demo.httpclient.XmlUtils.given
import io.circe.*
import io.circe.syntax.*

import javax.security.sasl.SaslClientFactory

given throwCodec: Codec[Throwable] = Codec.from(Decoder.decodeString.map(s => Throwable(s)), Encoder.encodeString.contramap(_.toString))
trait TestResult

case class TestSuccessResult(uri: XmlLocation, mode: TestXmlType, expectedPass: Boolean, notes: String) extends TestResult
    derives Codec.AsObject {}

case class TestErrorResult(uri: XmlLocation, mode: TestXmlType, expectedPass: Boolean, notes: String, error: Option[Throwable] = None)
    extends TestResult derives Codec.AsObject

/** A systematic error in running the test framework (as opposed to dealing with a specific file. */
case class TestFailure(message: String, error: Option[Throwable]) extends TestResult derives Codec.AsObject

object TestResult {
  private val errCodec: Codec.AsObject[TestErrorResult]        = summon[Codec.AsObject[TestErrorResult]]
  private val succcessCodec: Codec.AsObject[TestSuccessResult] = summon[Codec.AsObject[TestSuccessResult]]
  private val failureCodec                                     = summon[Codec.AsObject[TestFailure]]

  private val discError: String   = TestErrorResult.getClass.getSimpleName
  private val discSuccess: String = TestSuccessResult.getClass.getSimpleName
  private val discFailure: String = TestFailure.getClass.getSimpleName

  given encoderTestResult: Encoder.AsObject[TestResult] = Encoder.AsObject.instance[TestResult] {
    case e: TestErrorResult   => errCodec.encodeObject(e).add("_discriminator", Json.fromString(discError))
    case s: TestSuccessResult => succcessCodec.encodeObject(s).add("_discriminator", Json.fromString(discSuccess))
    case f: TestFailure       => failureCodec.encodeObject(f).add("_discriminator", Json.fromString(discFailure))
  }

  given decoderTestResult: Decoder[TestResult] = Decoder.instance[TestResult] { hcursor =>
    hcursor.get[String]("_discriminator").flatMap { disc =>
      if disc == discError then hcursor.as[TestErrorResult]
      else if disc == discSuccess then hcursor.as[TestSuccessResult]
      else if disc == discFailure then hcursor.as[TestFailure]
      else Left(DecodingFailure(s"[$disc not a valid discriminator for TestResult]", hcursor.history))
    }
  }

  def failures(k: List[TestResult]): List[TestResult] = k.filter {
    case _: TestFailure => true
    case _              => false
  }

  def incorrectResult(k: List[TestResult]): List[TestResult] = k.filter {
    case t: TestSuccessResult if !t.expectedPass => true
    case t: TestErrorResult if t.expectedPass    => true
    case _                                       => false
  }

  def correctResult(k: List[TestResult]): List[TestResult] = k.filter {
    case t: TestSuccessResult if t.expectedPass => true
    case t: TestErrorResult if !t.expectedPass  => true
    case _                                      => false
  }

  def formatTestResultsSummary(k: List[TestResult]) = {
    s"""
       |Summary:
       |  Failures: ${failures(k).length}
       |  Correct: ${correctResult(k).length}
       |  Incorrect: ${incorrectResult(k).length}
       |
       |""".stripMargin
  }
}
