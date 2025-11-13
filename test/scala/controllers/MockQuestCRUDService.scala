package controllers

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.*
import cats.effect.IO
import cats.effect.Ref
import cats.implicits.*
import fs2.Stream
import infrastructure.cache.*
import models.auth.UserSession
import models.database.*
import models.kafka.Failure
import models.kafka.KafkaProducerResult
import models.kafka.SuccessfulWrite
import models.quests.*
import models.QuestStatus
import models.Rank
import services.QuestCRUDServiceAlgebra

class MockQuestCRUDService(userQuestData: Map[String, QuestPartial]) extends QuestCRUDServiceAlgebra[IO] {

  override def getByQuestId(businessId: String): IO[Option[QuestPartial]] =
    userQuestData.get(businessId) match {
      case Some(address) => IO.pure(Some(address))
      case None => IO.pure(None)
    }
  override def create(request: CreateQuestPartial, clientId: String): IO[ValidatedNel[Failure, KafkaProducerResult]] =
    IO.pure(Valid(SuccessfulWrite))

  override def update(businessId: String, request: UpdateQuestPartial): IO[ValidatedNel[DatabaseErrors, DatabaseSuccess]] =
    IO.pure(Valid(UpdateSuccess))

  override def delete(businessId: String): IO[ValidatedNel[DatabaseErrors, DatabaseSuccess]] =
    IO.pure(Valid(DeleteSuccess))
}
