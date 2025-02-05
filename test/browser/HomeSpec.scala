package browser

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class HomeSpec extends Specification with Tables with BaseSpec {
  
  "Home" should {
    "Redirect to /login when disconnected" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = applicationWithBrowser) {
      val loginURL = controllers.routes.HomeController.index().absoluteURL(false, s"localhost:$port")

      browser.goTo(loginURL)


      browser.url must endWith(controllers.routes.LoginController.login().url.substring(1))
      browser.pageSource must contain("Vous devez vous identifier pour accèder à cette page.")
    }

    "Status up" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = applicationWithBrowser) {
      val loginURL = controllers.routes.HomeController.status().absoluteURL(false, s"localhost:$port")

      browser.goTo(loginURL)

      browser.pageSource must contain("OK")
    }
  }
}