package controllers

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.kernel.Async
import cats.implicits.*
import infrastructure.cache.*
import io.circe.syntax.*
import io.circe.syntax.EncoderOps
import models.responses.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.typelevel.log4cats.Logger
import scala.concurrent.duration.*
import services.SessionService
import services.SessionServiceAlgebra

trait AuthControllerAlgebra[F[_]] {
  def routes: HttpRoutes[F]
}

class AuthControllerImpl[F[_] : Async : Logger](
  sessionService: SessionServiceAlgebra[F]
) extends Http4sDsl[F]
    with AuthControllerAlgebra[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "auth" / "session" / userId =>
      Logger[F].debug(s"[AuthControllerImpl] GET - Validating session for userId: $userId") *>
        sessionService.getSession(userId).flatMap {
          case Some(token) =>
            Logger[F].debug(s"[AuthControllerImpl] Session found for $userId: $token") *>
              Ok(GetResponse("200", s"Session token: $token").asJson)
          case None =>
            Logger[F].debug(s"[AuthControllerImpl] No session found for $userId") *>
              NotFound(ErrorResponse("NOT_FOUND", s"No session for userId $userId").asJson)
        }

    case req @ POST -> Root / "auth" / "session" / userId =>
      Logger[F].debug(s"Incoming cookies: ${req.cookies.map(c => s"${c.name}=${c.content}").mkString(", ")}") *>
        Logger[F].debug(s"POST - Creating session for userId: $userId") *>
        Async[F].delay(req.cookies.find(_.name == "auth_session")).flatMap {
          case Some(cookie) =>
            sessionService.storeOnlyCookie(userId, cookie.content) *>
              Created(CreatedResponse(userId, "Session stored from cookie").asJson)
                .map(_.withContentType(`Content-Type`(MediaType.application.json)))
          case None =>
            Logger[F].debug(s"No auth_session cookie for $userId") *>
              BadRequest(ErrorResponse("NO_COOKIE", "auth_session cookie not found").asJson)
                .map(_.withContentType(`Content-Type`(MediaType.application.json)))
        }
  }
}

object AuthController {
  def apply[F[_] : Async : Logger](sessionService: SessionServiceAlgebra[F]): AuthControllerAlgebra[F] =
    new AuthControllerImpl[F](sessionService)
}
