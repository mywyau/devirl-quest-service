package kafka

import cats.effect.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import configuration.models.*
import configuration.BaseAppConfig
import controllers.test_routes.TestRoutes.*
import dev.profunktor.redis4cats.Redis
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fs2.kafka.Acks
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerSettings
import infrastructure.cache.*
import infrastructure.cache.SessionCache
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
import shared.KafkaProducerResource
import shared.SessionCacheResource
import shared.TransactorResource
import weaver.GlobalResource
import weaver.GlobalWrite

object KafkaSharedResource extends GlobalResource with BaseAppConfig {

  def kafkaProducerResource(): Resource[IO, KafkaProducer[IO, String, String]] =
    KafkaProducer.resource(
      ProducerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092") // adjust if your Kafka is elsewhere - todo move to config
        .withAcks(Acks.All)
    )

  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      appConfig <- appConfigResource
      kafkaProducer <- kafkaProducerResource()
      _ <- global.putR(KafkaProducerResource(kafkaProducer))
    } yield ()
}
