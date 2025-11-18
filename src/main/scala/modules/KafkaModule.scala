// modules/KafkaModule.scala
package modules

import cats.effect.*
import configuration.AppConfig
import fs2.kafka.*
import infrastructure.KafkaProducerProvider
import org.typelevel.log4cats.Logger
import services.kafka.producers.*

final case class KafkaProducers[F[_]](
  questEventProducer: QuestEventProducerAlgebra[F]
)

object KafkaModule {

  def make[F[_] : Async : Logger](appConfig: AppConfig): Resource[F, KafkaProducers[F]] =
    for {
      // ✅ Use your existing provider
      producer <- KafkaProducerProvider.make[F](
        bootstrap = appConfig.kafka.bootstrapServers,
        clientId = appConfig.kafka.clientId,
        acks = appConfig.kafka.acks,
        lingerMs = appConfig.kafka.lingerMs,
        retries = appConfig.kafka.retries
      )

      questEventProducer = new QuestEventProducerImpl[F](appConfig.kafka.topic.questEventsTopic, producer)

    } yield KafkaProducers(questEventProducer)
}
