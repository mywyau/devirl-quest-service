package kafka

import cats.effect.*
import cats.syntax.all.*
import fs2.kafka.*
import io.circe.syntax.*
import models.events.QuestCreatedEvent
import models.kafka.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.kafka.producers.QuestEventProducerImpl
import shared.KafkaProducerResource
import weaver.*

import java.time.Instant
import scala.concurrent.duration.*

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

  test("QuestCreatedEvent should be produced and consumed successfully") { (sharedResource, log) =>

    val kafkaProducer = sharedResource.producer

    val topic = s"quest.created.v1.test.${System.currentTimeMillis()}"
    val event =
      QuestCreatedEvent(
        questId = "quest-e2e-001",
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
        .timeout(5.seconds)

    for {
      _ <- resetKafkaTopic(topic)
      _ <- logger.info(s"[Producer] Sending event to topic $topic")
      fiber <- consumeOnce.start
      _ <- IO.sleep(500.millis) // give consumer time to subscribe
      _ <- questProducer.publishQuestCreated(event)
      received <- fiber.joinWithNever
      _ <- deleteTopic(topic)
    } yield expect(event.questId == received.questId)
  }
}