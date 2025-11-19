package services.kafka.producers

import cats.effect.Sync
import cats.syntax.all.*
import fs2.kafka.*
import io.circe.syntax.*
import models.Envelope
import models.events.QuestCompletedEvent
import models.events.QuestCreatedEvent
import models.events.QuestUpdatedEvent
import models.kafka.*

trait QuestEventProducerAlgebra[F[_]] {

  def publishQuestCreated(event: QuestCreatedEvent): F[KafkaProducerResult]

  def publishQuestCompleted(event: QuestCompletedEvent): F[KafkaProducerResult]

  def publishQuestUpdated(event: QuestUpdatedEvent): F[KafkaProducerResult]

}

final class QuestEventProducerImpl[F[_] : Sync](
  topic: String,
  producer: KafkaProducer[F, String, String]
) extends QuestEventProducerAlgebra[F] {

  private def publishEvent(key: String, value: String): F[KafkaProducerResult] = {
    val record = ProducerRecord(topic, key, value)
    val records = ProducerRecords.one(record)

    producer.produce(records).flatten.attempt.map {
      case Right(_) => SuccessfulWrite
      case Left(e) => FailedWrite(e.getMessage)
    }
  }

  override def publishQuestCreated(event: QuestCreatedEvent): F[KafkaProducerResult] = {
    val envelope =
      Envelope(
        typeName = "quest.created",
        payload = event
      )
    publishEvent(event.questId, envelope.asJson.noSpaces)
  }

  override def publishQuestCompleted(event: QuestCompletedEvent): F[KafkaProducerResult] = {
    val envelope =
      Envelope(
        typeName = "quest.completed",
        payload = event
      )
    publishEvent(event.questId, envelope.asJson.noSpaces)
  }

  override def publishQuestUpdated(event: QuestUpdatedEvent): F[KafkaProducerResult] = {
    val envelope =
      Envelope(
        typeName = "quest.update",
        payload = event
      )
    publishEvent(event.questId, envelope.asJson.noSpaces)
  }

  // override def publishQuestCreated(event: QuestCreatedEvent): F[KafkaProducerResult] =
  //   publishEvent(event.questId, event.asJson.noSpaces)

  // override def publishQuestCompleted(event: QuestCompletedEvent): F[KafkaProducerResult] =
  //   publishEvent(event.questId, event.asJson.noSpaces)

  // override def publishQuestUpdated(event: QuestUpdatedEvent): F[KafkaProducerResult] =
  //   publishEvent(event.questId, event.asJson.noSpaces)

}
