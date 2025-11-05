package controllers.test_routes

import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect.*
import cats.implicits.*
import configuration.AppConfig
import configuration.BaseAppConfig
import controllers.mocks.*
import controllers.BaseController
import controllers.QuestController
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import fs2.kafka.KafkaProducer
import infrastructure.cache.*
import java.net.URI
import java.time.Duration
import java.time.Instant
import models.auth.UserSession
import models.cache.*
import models.events.QuestCreatedEvent
import models.kafka.*
import org.http4s.server.Router
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import repositories.*
import services.*
import services.kafka.producers.QuestEventProducerAlgebra
import services.kafka.producers.QuestEventProducerImpl

object TestRoutes extends BaseAppConfig {

  implicit val testLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def baseRoutes(): HttpRoutes[IO] = {
    val baseController = BaseController[IO]()
    baseController.routes
  }

  val sessionToken = "test-session-token"

  def fakeUserSession(userId: String) =
    UserSession(
      userId = userId,
      cookieValue = sessionToken,
      email = s"$userId@example.com",
      userType = "Dev"
    )

  def questRoutes(appConfig: AppConfig, transactor: Transactor[IO], producer: KafkaProducer[IO, String, String]): Resource[IO, HttpRoutes[IO]] = {

    val topic = "quest.created.v1.test"

    for {
      authSessionRef <- Resource.eval(
        Ref.of[IO, Map[String, UserSession]](
          Map(
            s"auth:session:USER001" -> fakeUserSession("USER001"),
            s"auth:session:USER002" -> fakeUserSession("USER002"),
            s"auth:session:USER003" -> fakeUserSession("USER003"),
            s"auth:session:USER004" -> fakeUserSession("USER004"),
            s"auth:session:USER005" -> fakeUserSession("USER005"),
            s"auth:session:USER006" -> fakeUserSession("USER006"),
            s"auth:session:USER007" -> fakeUserSession("USER007")
          )
        )
      )
      mockSessionCache = new MockSessionCache(authSessionRef)
      questRepository = QuestRepository(transactor)
          
      questEventProducer = new QuestEventProducerImpl(topic, producer)
      questCRUDService = QuestCRUDService(appConfig, questRepository, questEventProducer)
      questController = QuestController[IO](mockSessionCache, questCRUDService)
    } yield questController.routes
  }

  def createTestRouter(appConfig: AppConfig, transactor: Transactor[IO], producer: KafkaProducer[IO, String, String]): Resource[IO, HttpRoutes[IO]] = {

    val redisHost = sys.env.getOrElse("REDIS_HOST", appConfig.redisConfig.host)
    val redisPort = sys.env.get("REDIS_PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(appConfig.redisConfig.port)
    
    for {
      questRoutes: HttpRoutes[IO] <- questRoutes(appConfig, transactor, producer)
    } yield Router(
      "/devirl-quest-service" -> (
        baseRoutes() <+>
          questRoutes
      )
    )
  }
}
