package controllers.test_routes

import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect.*
import cats.implicits.*
import configuration.AppConfig
import configuration.BaseAppConfig
import controllers.mocks.*
import controllers.BaseController
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
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

object TestRoutes extends BaseAppConfig {

  implicit val testLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def baseRoutes(): HttpRoutes[IO] = {
    val baseController = BaseController[IO]()
    baseController.routes
  }

  def createTestRouter(transactor: Transactor[IO], appConfig: AppConfig): Resource[IO, HttpRoutes[IO]] = {

    val redisHost = sys.env.getOrElse("REDIS_HOST", appConfig.redisConfig.host)
    val redisPort = sys.env.get("REDIS_PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(appConfig.redisConfig.port)

    // Use only mock producer — no real Kafka connection
    val questEventProducer: QuestEventProducerAlgebra[IO] = new MockQuestEventProducer[IO]()

    Resource.pure(
      Router(
        "/devirl-quest-service" -> baseRoutes()
      )
    )
  }
}
