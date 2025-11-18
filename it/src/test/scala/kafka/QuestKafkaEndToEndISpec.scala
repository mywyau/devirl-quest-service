package kafka

import cats.effect.*
import cats.syntax.all.*
import fs2.kafka.*
import io.circe.syntax.*
import java.time.Instant
import models.events.QuestCompletedEvent
import models.events.QuestCreatedEvent
import models.events.QuestUpdatedEvent
import models.kafka.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import scala.concurrent.duration.*
import services.kafka.producers.QuestEventProducerImpl
import shared.KafkaProducerResource
import weaver.*

class QuestKafkaEndToEndISpec(global: GlobalRead) extends IOSuite {
  type Res = KafkaProducerResource

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def sharedResource: Resource[IO, Res] =
    for {
      producer <- global.getOrFailR[KafkaProducerResource]()
    } yield producer

  private def resetKafkaTopic(topic: String): IO[Unit] =
    IO.blocking {
      import sys.process._
      s"docker exec kafka-container-redpanda-1 rpk topic create $topic --brokers localhost:9092".!
    }.void

  private def deleteTopic(topic: String): IO[Unit] =
    IO.blocking {
      import sys.process._
      s"docker exec kafka-container-redpanda-1 rpk topic delete $topic --brokers localhost:9092".!
    }.void

  test("QuestCreatedEvent - should be produced and consumed successfully") { (sharedResource, log) =>

    val kafkaProducer = sharedResource.producer

    val topic = s"quest.events.test1.v1"

    val event =
      QuestCreatedEvent(
        questId = "quest001",
        title = "End-to-End Test Quest",
        clientId = "client-e2e",
        createdAt = Instant.now()
      )

    val questProducer = new QuestEventProducerImpl[IO](topic, kafkaProducer)

    val consumerSettings =
      ConsumerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092")
        .withGroupId(s"group-${System.currentTimeMillis()}")
        .withAutoOffsetReset(AutoOffsetReset.Latest)

    val consumeOnce =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topic)
        .records
        .evalMap { committable =>
          IO.fromEither(io.circe.parser.decode[QuestCreatedEvent](committable.record.value))
            .flatTap(ev => logger.info(s"[Consumer] Received: ${ev.questId}"))
            .flatTap(_ => committable.offset.commit)
        }
        .take(1)
        .compile
        .lastOrError
        .timeout(10.seconds)

    for {
      _ <- resetKafkaTopic(topic)
      fiber <- consumeOnce.start
      _ <- IO.sleep(500.millis) // give consumer time to subscribe
      _ <- logger.info(s"[QuestKafkaEndToEndISpec][publishQuestCreated] Sending event to topic $topic")
      _ <- questProducer.publishQuestCreated(event)
      received <- fiber.joinWithNever
      _ <- deleteTopic(topic)
    } yield expect(event.questId == received.questId)
  }

  test("QuestCompletedEvent - should be produced and consumed successfully") { (sharedResource, log) =>

    val kafkaProducer = sharedResource.producer

    val topic = s"quest.events.test2.v1"

    val completedEvent =
      QuestCompletedEvent(
        questId = "quest002",
        title = "Quest Completed Event",
        clientId = "client001",
        createdAt = Instant.now()
      )

    val questProducer = new QuestEventProducerImpl[IO](topic, kafkaProducer)

    val consumerSettings =
      ConsumerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092")
        .withGroupId(s"group-${System.currentTimeMillis()}")
        .withAutoOffsetReset(AutoOffsetReset.Latest)

    val consumeOnce =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topic)
        .records
        .evalMap { committable =>
          IO.fromEither(io.circe.parser.decode[QuestCompletedEvent](committable.record.value))
            .flatTap(ev => logger.info(s"[QuestKafkaEndToEndISpec][Consumer] Received: ${ev.questId}"))
            .flatTap(_ => committable.offset.commit)
        }
        .take(1)
        .compile
        .lastOrError
        .timeout(10.seconds)

    for {
      _ <- resetKafkaTopic(topic)
      fiber <- consumeOnce.start
      _ <- IO.sleep(500.millis) // give consumer time to subscribe
      _ <- questProducer.publishQuestCompleted(completedEvent)
      _ <- logger.info(s"[QuestKafkaEndToEndISpec][publishQuestCompleted] Sending event to topic $topic")

      received <- fiber.joinWithNever
      _ <- deleteTopic(topic)
    } yield expect.all(
      completedEvent.questId == received.questId,
      completedEvent.title == received.title
    )
  }

  test("QuestUpdatedEvent - should be produced and consumed successfully") { (sharedResource, log) =>

    val kafkaProducer = sharedResource.producer

    val topic = s"quest.events.test3.v1"

    val updateEvent =
      QuestUpdatedEvent(
        questId = "quest003",
        title = "Quest Updated Event",
        clientId = "client001",
        createdAt = Instant.now()
      )

    val questProducer = new QuestEventProducerImpl[IO](topic, kafkaProducer)

    val consumerSettings =
      ConsumerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092")
        .withGroupId(s"group-${System.currentTimeMillis()}")
        .withAutoOffsetReset(AutoOffsetReset.Latest)

    val consumeOnce =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topic)
        .records
        .evalMap { committable =>
          IO.fromEither(io.circe.parser.decode[QuestUpdatedEvent](committable.record.value))
            .flatTap(ev => logger.info(s"[QuestKafkaEndToEndISpec][Consumer] Received: ${ev.questId}"))
            .flatTap(_ => committable.offset.commit)
        }
        .take(1)
        .compile
        .lastOrError
        .timeout(10.seconds)

    for {
      _ <- resetKafkaTopic(topic)
      fiber <- consumeOnce.start
      _ <- IO.sleep(500.millis) // give consumer time to subscribe
      _ <- questProducer.publishQuestUpdated(updateEvent)
      _ <- logger.info(s"[QuestKafkaEndToEndISpec][publishQuestUpdated] Sending event to topic $topic")
      received <- fiber.joinWithNever
      _ <- deleteTopic(topic)
    } yield expect.all(
      updateEvent.questId == received.questId,
      updateEvent.title == received.title
    )
  }
}
