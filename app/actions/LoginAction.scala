package actions

import javax.inject.{Inject, Singleton}
import controllers.routes
import models._
import play.api.mvc._
import play.api.mvc.Results.{Redirect, _}
import services.{EventService, TokenService, UserService}
import extentions.UUIDHelper
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class RequestWithUserData[A](val currentUser: User, val currentArea: Area, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class LoginAction @Inject()(val parser: BodyParsers.Default,
                            userService: UserService,
                            eventService: EventService,
                            tokenService: TokenService,
                            configuration: play.api.Configuration)(implicit ec: ExecutionContext) extends ActionBuilder[RequestWithUserData, AnyContent] with ActionRefiner[Request, RequestWithUserData] {
  def executionContext = ec

  private lazy val areasWithLoginByKey = configuration.underlying.getString("app.areasWithLoginByKey").split(",").flatMap(UUIDHelper.fromString)
  private lazy val tokenExpirationInMinutes = configuration.underlying.getInt("app.tokenExpirationInMinutes")

  private def queryToString(qs: Map[String, Seq[String]]) = {
    val queryString = qs.map { case (key, value) => key + "=" + value.sorted.mkString("|,|") }.mkString("&")
    if (queryString.nonEmpty) "?" + queryString else ""
  }

  override def refine[A](request: Request[A]) =
    Future.successful {
      implicit val req = request
      val path = request.path + queryToString(request.queryString - "key" - "token")
      val url = "http" + (if (request.secure) "s" else "") + "://" + request.host + path
      (userBySession,userByKey,tokenById) match {
        case (Some(userSession), Some(userKey), None) if userSession.id == userKey.id =>
          Left(Redirect(Call(request.method, url)))
        case (_, Some(user), None) =>
          val area = areaFromContext(user)
          implicit val requestWithUserData = new RequestWithUserData(user, area, request)
          if(areasWithLoginByKey.contains(area.id) && !user.admin) {
            // areasWithLoginByKey is an insecure setting for demo usage and transition only
            eventService.info("LOGIN_BY_KEY", s"Connexion par clé réussi (Transition ne doit pas être maintenu en prod)")
            Left(Redirect(Call(request.method, url)).withSession(request.session - "userId" + ("userId" -> user.id.toString)))
          } else {
            eventService.info("TRY_LOGIN_BY_KEY", "Clé dans l'url, redirige vers la page de connexion")
            Left(Redirect(routes.LoginController.login()).flashing("email" -> user.email, "path" -> path))
          }
        case (_, None, Some(token)) =>
          manageToken(token)
        case (Some(user), None, None) if user.disabled == false =>
          manageUserLogged(user)
        case (Some(user), None, None) if user.disabled == true =>
          val area = areaFromContext(user)
          implicit val requestWithUserData = new RequestWithUserData(user, area, request)
          eventService.info("USER_ACCESS_DISABLED", s"Utilisateur désactivé essaye d'accèder à la page ${request.path}}")
          userNotLogged("Votre compte a été désactivé. Contactez votre référent ou l'équipe d'Administration+ sur contact@aplus.beta.gouv.fr en cas de problème.")
        case _ =>
          val message = request.getQueryString("token") match {
            case Some(token) =>
              eventService.info(User.systemUser, Area.notApplicable, request.remoteAddress, "UNKNOWN_TOKEN", s"Token $token est inconnue", None, None)
              "Le lien que vous avez utilisé n'est plus valide, il a déjà été utilisé. Si cette erreur se répète, contactez l'équipe Administration+"
            case None =>
              Logger.warn(s"Accès à la ${request.path} non autorisé")
              "Vous devez vous identifier pour accèder à cette page."
          }
          userNotLogged(message)
      }
    }

  private def manageUserLogged[A](user: User)(implicit request: Request[A]) = {
    val area = areaFromContext(user)
    implicit val requestWithUserData = new RequestWithUserData(user, area, request)
    if(user.cguAcceptationDate.nonEmpty || request.path.contains("cgu")) {
      Right(requestWithUserData)
    } else {
      eventService.info("REDIRECTED_TO_CGU","Redirection vers les CGUs")
      Left(Redirect(routes.UserController.showCGU()).flashing("redirect" -> request.path))
    }
  }

  private def areaFromContext[A](user: User)(implicit request: Request[A]) = request.session.get("areaId").flatMap(UUIDHelper.fromString).orElse(user.areas.headOption).flatMap(id => Area.all.find(_.id == id)).getOrElse(Area.all.head)

  private def manageToken[A](token: LoginToken)(implicit request: Request[A]) = {
    val userOption = userService.byId(token.userId)
    userOption match {
      case None =>
        Logger.error(s"Try to login by token ${token.token} for an unknown user : ${token.userId}")
        userNotLogged("Une erreur s'est produite, votre utilisateur n'existe plus")
      case Some(user) =>
        //hack: we need RequestWithUserData to call the logger
        val area = areaFromContext(user)
        implicit val requestWithUserData = new RequestWithUserData(user, area, request)

        if(token.ipAddress != request.remoteAddress) {
          eventService.warn("AUTH_WITH_DIFFERENT_IP", s"Utilisateur ${token.userId} à une adresse ip différente pour l'essai de connexion")
        }
        if(token.isActive){
          val url = request.path + queryToString(request.queryString - "key" - "token")
          eventService.info("AUTH_BY_KEY", s"Identification par token")
          Left(Redirect(Call(request.method, url)).withSession(request.session - "userId" + ("userId" -> user.id.toString)))
        } else {
          eventService.warn("EXPIRED_TOKEN", s"Token expiré pour ${token.userId}")
          userNotLogged(s"Le lien que vous avez utilisez a expiré (il expire après $tokenExpirationInMinutes minutes), saisissez votre email pour vous reconnecter")
        }
    }
  }

  private def userNotLogged[A](message: String)(implicit request: Request[A]) = Left(Redirect(routes.LoginController.login())
    .withSession(request.session - "userId").flashing("error" -> message))

  private def tokenById[A](implicit request: Request[A]) = request.getQueryString("token").flatMap(tokenService.byToken)
  private def userByKey[A](implicit request: Request[A]) = request.getQueryString("key").flatMap(userService.byKey)
  private def userBySession[A](implicit request: Request[A]) = request.session.get("userId").flatMap(UUIDHelper.fromString).flatMap(id => userService.byIdCheckDisabled(id,true))
}

