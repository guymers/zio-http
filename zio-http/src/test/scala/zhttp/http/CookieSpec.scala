package zhttp.http

import zhttp.internal.HttpGen
import zio.test._

object CookieSpec extends DefaultRunnableSpec {
  def spec = suite("Cookies") {
    suite("response cookies") {
      testM("encode/decode signed/unsigned cookies with secret") {
        check(HttpGen.cookies) { cookie =>
          val expected = cookie.encode
          val actual   = Cookie.decodeResponseCookie(cookie.encode, cookie.secret).get.encode

          assertTrue(actual == expected)
        }
      }
    } +
      suite("request cookies") {
        testM("encode/decode multiple cookies with ZIO Test Gen") {
          check(for {
            name         <- Gen.anyString
            content      <- Gen.anyString
            cookieList   <- Gen.listOf(Gen.const(Cookie(name, content)))
            cookieString <- Gen.const(cookieList.map(x => s"${x.name}=${x.content}").mkString(";"))
          } yield (cookieList, cookieString)) { case (cookies, message) =>
            val actual = Cookie.decodeRequestCookie(message)
            assertTrue(actual == cookies)
          }
        }
      }
  }
}
