package controllers

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.implicits.*
import fs2.Stream
import infrastructure.cache.*
import infrastructure.cache.SessionCacheAlgebra
import io.circe.Json
import io.circe.syntax.EncoderOps
import models.*
import models.database.UpdateSuccess
import models.quests.*
import models.responses.*
import org.http4s.*
import org.http4s.Challenge
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.syntax.all.http4sHeaderSyntax
import org.typelevel.log4cats.Logger
import services.QuestCRUDServiceAlgebra

import scala.concurrent.duration.*

trait QuestControllerAlgebra[F[_]] {
  def routes: HttpRoutes[F]
}

class QuestControllerImpl[F[_] : Async : Concurrent : Logger](
  sessionCache: SessionCacheAlgebra[F],
  questCRUDService: QuestCRUDServiceAlgebra[F]
) extends Http4sDsl[F]
    with QuestControllerAlgebra[F] {

  implicit val createDecoder: EntityDecoder[F, CreateQuestPartial] = jsonOf[F, CreateQuestPartial]

  implicit val questStatusQueryParamDecoder: QueryParamDecoder[QuestStatus] =
    QueryParamDecoder[String].emap { str =>
      Either
        .catchNonFatal(QuestStatus.fromString(str))
        .leftMap(t => ParseFailure("Invalid status", t.getMessage))
    }

  object StatusParam extends OptionalQueryParamDecoderMatcher[QuestStatus]("status")
  object PageParam extends OptionalQueryParamDecoderMatcher[Int]("page")
  object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  private def extractSessionToken(req: Request[F]): Option[String] =
    req.cookies
      .find(_.name == "auth_session")
      .map(_.content)

  private def withValidSession(userId: String, token: String)(onValid: F[Response[F]]): F[Response[F]] =
    sessionCache.getSession(userId).flatMap {
      case Some(userSessionJson) if userSessionJson.cookieValue == token =>
        Logger[F].debug("[QuestControllerImpl][withValidSession] Found valid session for userId:") *>
          onValid
      case Some(_) =>
        Logger[F].debug("[QuestControllerImpl][withValidSession] User session does not match requested user session token value from redis.")
        Forbidden("User session does not match requested user session token value from redis.")
      case None =>
        Logger[F].debug("[QuestControllerImpl][withValidSession] Invalid or expired session")
        Forbidden("Invalid or expired session")
    }

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ GET -> Root / "quest" / "health" =>
      Logger[F].debug(s"[BaseControllerImpl] GET - Health check for backend QuestController service") *>
        Ok(GetResponse("/devirl-quest-service/health", "I am alive").asJson)

    case req @ POST -> Root / "quest" / "create" / userIdFromRoute =>
      extractSessionToken(req) match {
        case Some(cookieToken) =>
          withValidSession(userIdFromRoute, cookieToken) {
            Logger[F].debug(s"[QuestControllerImpl] POST - Creating quest") *>
              req.decode[CreateQuestPartial] { request =>
                questCRUDService.create(request, userIdFromRoute).flatMap {
                  case Valid(response) =>
                    Logger[F].debug(s"[QuestControllerImpl] POST - Successfully created a quest") *>
                      Created(CreatedResponse(response.toString, "quest details created successfully").asJson)
                  case Invalid(_) =>
                    InternalServerError(ErrorResponse(code = "Code", message = "An error occurred").asJson)
                }
              }
          }
        case None =>
          Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "api")), "Missing Cookie")
      }
  }
}

object QuestController {
  def apply[F[_] : Async : Concurrent](
    sessionCache: SessionCacheAlgebra[F],
    questCRUDService: QuestCRUDServiceAlgebra[F]
  )(implicit logger: Logger[F]): QuestControllerAlgebra[F] =
    new QuestControllerImpl[F](sessionCache, questCRUDService)
}
