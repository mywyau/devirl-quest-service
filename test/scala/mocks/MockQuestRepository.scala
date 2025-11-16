package mocks

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.IO
import fs2.Stream
import java.time.Instant
import models.database.*
import models.quests.*
import models.QuestStatus
import models.Rank
import repositories.QuestRepositoryAlgebra

case class MockQuestRepository(
  countActiveQuests: Int = 5,
  existingQuest: Map[String, QuestPartial] = Map.empty
) extends QuestRepositoryAlgebra[IO] {

  def showAllUsers: IO[Map[String, QuestPartial]] = IO.pure(existingQuest)
  override def create(request: CreateQuest): IO[ValidatedNel[DatabaseErrors, DatabaseSuccess]] = IO.pure(Valid(CreateSuccess))
}
