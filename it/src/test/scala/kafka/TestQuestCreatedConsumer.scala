package kafka

package kafka

import cats.effect.*
import cats.syntax.all.*
import fs2.kafka.*
import io.circe.parser.decode
import models.events.QuestCreatedEvent
import org.typelevel.log4cats.Logger

object TestQuestCreatedConsumer {

  def stream[F[_]: Async: Logger](
      topic: String,
      bootstrapServers: String
  ): fs2.Stream[F, QuestCreatedEvent] = {

    val settings =
      ConsumerSettings[F, String, String]
        .withBootstrapServers(bootstrapServers)
        .withGroupId("quest-consumer-itest")
        .withAutoOffsetReset(AutoOffsetReset.Earliest)

    KafkaConsumer
      .stream(settings)
      .subscribeTo(topic)
      .records
      .evalMap { msg =>
        decode[QuestCreatedEvent](msg.record.value) match {
          case Right(event) => msg.offset.commit.as(Some(event))
          case Left(_)      => msg.offset.commit.as(None)
        }
      }
      .unNone
  }
}
