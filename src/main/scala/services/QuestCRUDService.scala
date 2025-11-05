package services

import cats.data.EitherT
import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import cats.Monad
import cats.NonEmptyParallel
import configuration.AppConfig
import fs2.Stream
import java.util.UUID
import models.*
import models.database.*
import models.events.QuestCreatedEvent
import models.kafka.*
import models.languages.Language
import models.quests.*
import models.skills.Questing
import models.work_time.HoursOfWork
import models.NotStarted
import models.QuestStatus
import org.typelevel.log4cats.Logger
import repositories.*
import services.kafka.producers.QuestEventProducerAlgebra

trait QuestCRUDServiceAlgebra[F[_]] {

  def getByQuestId(questId: String): F[Option[QuestPartial]]

  def create(request: CreateQuestPartial, clientId: String): F[ValidatedNel[Failure, KafkaProducerResult]]

  def update(questId: String, request: UpdateQuestPartial): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]]

  def delete(questId: String): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]]
}

class QuestCRUDServiceImpl[F[_] : Concurrent : NonEmptyParallel : Monad : Logger](
  appConfig: AppConfig,
  questRepo: QuestRepositoryAlgebra[F],
  questEventProducer: QuestEventProducerAlgebra[F]
) extends QuestCRUDServiceAlgebra[F] {

  override def getByQuestId(questId: String): F[Option[QuestPartial]] =
    questRepo.findByQuestId(questId).flatMap {
      case Some(quest) =>
        Logger[F].debug(s"[QuestCRUDService][getByQuestId] Found quest with ID: $questId") *> Concurrent[F].pure(Some(quest))
      case None =>
        Logger[F].debug(s"[QuestCRUDService][getByQuestId] No quest found with ID: $questId") *> Concurrent[F].pure(None)
    }

  override def create(request: CreateQuestPartial, clientId: String): F[ValidatedNel[Failure, KafkaProducerResult]] = {

    val newQuestId = s"quest-${UUID.randomUUID().toString}"
    val now = java.time.Instant.now()

    val createQuest = CreateQuest(
      clientId = clientId,
      questId = newQuestId,
      rank = request.rank,
      title = request.title,
      description = request.description,
      acceptanceCriteria = request.acceptanceCriteria,
      tags = request.tags,
      status = Some(NotEstimated)
    )

    for {
      _ <- Logger[F].info(s"[QuestCRUDService][create] Creating new quest $newQuestId")

      dbResult <- questRepo.create(createQuest)

      result <- dbResult match {
        case Valid(_) =>
          val event = QuestCreatedEvent(
            questId = newQuestId,
            title = request.title,
            clientId = clientId,
            createdAt = now
          )

          questEventProducer
            .publishQuestCreated(event)
            .map(Valid(_))
            .handleErrorWith(e =>
              Logger[F].warn(e)(s"[QuestCRUDService][create] Failed to publish event for $newQuestId") *>
                Concurrent[F].pure(Invalid(NonEmptyList.one(KafkaSendError(e.getMessage))))
            )

        case Invalid(dbErrors) =>
          Logger[F].error(s"[QuestCRUDService][create] DB error: ${dbErrors.toList.mkString(", ")}") *>
            Concurrent[F].pure(Invalid(dbErrors.map(e => DatabaseFailure(e.toString))))
      }
    } yield result
  }

  override def update(questId: String, request: UpdateQuestPartial): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]] =
    questRepo.update(questId, request).flatMap {
      case Valid(value) =>
        Logger[F].debug(s"[QuestCRUDService][update] Successfully updated quest with ID: $questId") *>
          Concurrent[F].pure(Valid(value))
      case Invalid(errors) =>
        Logger[F].error(s"[QuestCRUDService][update] Failed to update quest with ID: $questId. Errors: ${errors.toList.mkString(", ")}") *>
          Concurrent[F].pure(Invalid(errors))
    }

  override def delete(questId: String): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]] =
    questRepo.delete(questId).flatMap {
      case Valid(value) =>
        Logger[F].debug(s"[QuestCRUDService][delete] Successfully deleted quest with ID: $questId") *>
          Concurrent[F].pure(Valid(value))
      case Invalid(errors) =>
        Logger[F].error(s"[QuestCRUDService][delete] Failed to delete quest with ID: $questId. Errors: ${errors.toList.mkString(", ")}") *>
          Concurrent[F].pure(Invalid(errors))
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
