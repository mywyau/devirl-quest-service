package services

import cats.Monad
import cats.NonEmptyParallel
import cats.data.EitherT
import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import configuration.AppConfig
import fs2.Stream
import models.*
import models.NotStarted
import models.QuestStatus
import models.database.*
import models.events.QuestCreatedEvent
import models.kafka.*
import models.quests.*
import org.typelevel.log4cats.Logger
import repositories.*
import services.kafka.producers.QuestEventProducerAlgebra

import java.util.UUID

trait QuestCRUDServiceAlgebra[F[_]] {

  def create(request: CreateQuestPartial, clientId: String): F[ValidatedNel[Failure, KafkaProducerResult]]
}

class QuestCRUDServiceImpl[F[_] : Concurrent : NonEmptyParallel : Monad : Logger](
  appConfig: AppConfig,
  questRepo: QuestRepositoryAlgebra[F],
  questEventProducer: QuestEventProducerAlgebra[F]
) extends QuestCRUDServiceAlgebra[F] {

  private def createQuestDomainModel(request: CreateQuestPartial, clientId: String, newQuestId: String): CreateQuest =
    CreateQuest(
      clientId = clientId,
      questId = newQuestId,
      rank = request.rank,
      title = request.title,
      description = request.description,
      acceptanceCriteria = request.acceptanceCriteria,
      // tags = request.tags
    )

  private def publishEvent(event: QuestCreatedEvent, newQuestId: String): F[ValidatedNel[Failure, KafkaProducerResult]] =
    questEventProducer
      .publishQuestCreated(event)
      .map(Valid(_))
      .handleErrorWith(e =>
        Logger[F].warn(e)(s"[QuestCRUDService][create] Failed to publish event for $newQuestId") *>
          Concurrent[F].pure(Invalid(NonEmptyList.one(KafkaSendError(e.getMessage))))
      )

  override def create(request: CreateQuestPartial, clientId: String): F[ValidatedNel[Failure, KafkaProducerResult]] = {

    val newQuestId = s"quest-${UUID.randomUUID().toString}"
    val now = java.time.Instant.now()
    val createQuest = createQuestDomainModel(request, clientId, newQuestId)

    for {
      _ <- Logger[F].info(s"[QuestCRUDService][create] Creating new quest $newQuestId")
      dbResult <- questRepo.create(createQuest)
      result <- dbResult match {
        case Valid(_) =>
          val event =
            QuestCreatedEvent(
              questId = newQuestId,
              title = request.title,
              clientId = clientId,
              createdAt = now
            )
          publishEvent(event, newQuestId)
        case Invalid(dbErrors) =>
          Logger[F].error(s"[QuestCRUDService][create] DB error: ${dbErrors.toList.mkString(", ")}") *>
            Concurrent[F].pure(Invalid(dbErrors.map(e => DatabaseFailure(e.toString))))
      }
    } yield result
  }
}

object QuestCRUDService {

  def apply[F[_] : Concurrent : NonEmptyParallel : Logger](
    appConfig: AppConfig,
    questRepo: QuestRepositoryAlgebra[F],
    questEventProducer: QuestEventProducerAlgebra[F]
  ): QuestCRUDServiceAlgebra[F] =
    new QuestCRUDServiceImpl[F](appConfig, questRepo, questEventProducer)
}
