package service

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global
import scala.service.PhishingDetector

class PhishingDetectorSpec extends AnyFunSuite with Matchers {

  test("extractUrls should extract all HTTP(S) URLs from text") {
    val text = "Visit https://a.com or http://b.com/test and ignore ftp://c.com"
    val urls = PhishingDetector.extractUrls(text)
    urls should contain theSameElementsAs List("https://a.com", "http://b.com/test")
  }

  test("extractUrls should return empty list from text without url") {
    val text = "Text without url"
    val urls = PhishingDetector.extractUrls(text)
    urls should contain theSameElementsAs Nil
  }

  test("isHighConfidence should return true for high confidence levels") {
    val levels = List("HIGH", "higher", "VERY_HIGH", "extremely_high")
    levels.foreach { level =>
      assert(PhishingDetector.isHighConfidence(level) === true)
    }
  }

  test("isHighConfidence should return false for low confidence levels") {
    val levels = List("LOW", "medium", "unknown", "")
    levels.foreach { level =>
      assert(PhishingDetector.isHighConfidence(level) === false)
    }
  }

  test("isPhishing (test mode) should return true if URL contains 'phish'") {
    val url = "http://example.com/phishing"
    val result = PhishingDetector.isPhishing(url, null).unsafeRunSync()
    result shouldBe true
  }

  test("isPhishing (test mode) should return false if URL does not contain 'phish'") {
    val url = "http://example.com/safe"
    val result = PhishingDetector.isPhishing(url, null).unsafeRunSync()
    result shouldBe false
  }
}
