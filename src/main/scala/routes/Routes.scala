package routes

import infrastructure.cache.RedisCacheImpl
import infrastructure.cache.SessionCache
import infrastructure.cache.SessionCacheImpl
import cats.effect.*
import cats.NonEmptyParallel
import configuration.AppConfig
import controllers.*
import doobie.hikari.HikariTransactor
import java.net.URI
import org.http4s.client.Client
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import repositories.*
import services.*
import services.kafka.producers.QuestEventProducerAlgebra // <-- add this import


object Routes {

  def baseRoutes[F[_] : Concurrent : Logger](): HttpRoutes[F] = {

    val baseController = BaseController()

    baseController.routes
  }

  def questsRoutes[F[_] : Concurrent : Temporal : NonEmptyParallel : Async : Logger](
    transactor: HikariTransactor[F],
    appConfig: AppConfig,
    questEventProducer: QuestEventProducerAlgebra[F]
  ): HttpRoutes[F] = {

    val sessionCache = new SessionCacheImpl(appConfig)
    val questRepository = QuestRepository(transactor)

    val questCRUDService =
      QuestCRUDService(
        appConfig,
        questRepository,
        questEventProducer
      )

    val questController = QuestController(sessionCache, questCRUDService)

    questController.routes
  }
}
