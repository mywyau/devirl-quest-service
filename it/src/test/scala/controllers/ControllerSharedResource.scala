package controllers

import infrastructure.cache.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import cats.effect.*
import configuration.models.*
import configuration.BaseAppConfig
import TestRoutes.*
import dev.profunktor.redis4cats.Redis
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import repository.DatabaseResource.postgresqlConfigResource
import scala.concurrent.ExecutionContext
import shared.HttpClientResource
import shared.SessionCacheResource
import shared.TransactorResource
import weaver.GlobalResource
import weaver.GlobalWrite
import infrastructure.cache.SessionCache
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerSettings
import fs2.kafka.Acks

object ControllerSharedResource extends GlobalResource with BaseAppConfig {

  def executionContextResource: Resource[IO, ExecutionContext] =
    ExecutionContexts.fixedThreadPool(4)

  def transactorResource(postgresqlConfig: PostgresqlConfig, ce: ExecutionContext): Resource[IO, HikariTransactor[IO]] =
    HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = s"jdbc:postgresql://${postgresqlConfig.host}:${postgresqlConfig.port}/${postgresqlConfig.dbName}",
      user = postgresqlConfig.username,
      pass = postgresqlConfig.password,
      connectEC = ce
    )

  def clientResource: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  def serverResource(
    host: Host,
    port: Port,
    router: Resource[IO, HttpRoutes[IO]]
  ): Resource[IO, Server] =
    router.flatMap { router =>
      EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(router.orNotFound)
        .build
    }

  def kafkaProducerResource(): Resource[IO, KafkaProducer[IO, String, String]] =
    KafkaProducer.resource(
      ProducerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092") // adjust if your Kafka is elsewhere - todo move to config
        .withAcks(Acks.All)
    )
  
  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      appConfig <- appConfigResource
      host <- hostResource(appConfig)
      port <- portResource(appConfig)
      postgresqlConfig <- postgresqlConfigResource(appConfig)
      postgresqlHost <- Resource.eval {
        IO.pure(sys.env.getOrElse("DB_HOST", postgresqlConfig.host))
      }
      postgresqlPort <- Resource.eval {
        IO.pure(sys.env.get("DB_PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(postgresqlConfig.port))
      }
      appRedisConfig <- redisConfigResource(appConfig)
      redisHost <- Resource.eval {
        IO.pure(sys.env.getOrElse("REDIS_HOST", appRedisConfig.host))
      }
      redisPort <- Resource.eval {
        IO.pure(sys.env.get("REDIS_PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(appRedisConfig.port))
      }
      ce <- executionContextResource
      xa <- transactorResource(postgresqlConfig.copy(host = postgresqlHost, port = postgresqlPort), ce)
      sessionCache <- SessionCache.make[IO](appConfig)
      client <- clientResource
      kafkaProducer <- kafkaProducerResource()
      _ <- serverResource(host, port, createTestRouter(appConfig, xa, kafkaProducer))
      _ <- global.putR(TransactorResource(xa))
      _ <- global.putR(HttpClientResource(client))
      _ <- global.putR(SessionCacheResource(sessionCache))
    } yield ()
}
