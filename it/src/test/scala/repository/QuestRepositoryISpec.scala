package repository

import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.Resource
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import models.Completed
import models.Demonic
import models.InProgress
import models.database.*
import models.languages.*
import models.quests.CreateQuest
import repositories.QuestRepositoryImpl
import repository.RepositoryISpecBase
import repository.fragments.QuestRepoFragments.*
import shared.TransactorResource
import test_data.ITestConstants.*
import weaver.GlobalRead
import weaver.IOSuite
import weaver.ResourceTag

import java.time.LocalDateTime
import scala.collection.immutable.ArraySeq

class QuestRepositoryISpec(global: GlobalRead) extends IOSuite with RepositoryISpecBase {
  type Res = QuestRepositoryImpl[IO]

  private def initializeSchema(transactor: TransactorResource): Resource[IO, Unit] =
    Resource.eval(
      createQuestTable.update.run.transact(transactor.xa).void *>
        resetQuestTable.update.run.transact(transactor.xa).void *>
        insertQuestData.update.run.transact(transactor.xa).void
    )

  def sharedResource: Resource[IO, QuestRepositoryImpl[IO]] = {
    val setup = for {
      transactor <- global.getOrFailR[TransactorResource]()
      questRepo = new QuestRepositoryImpl[IO](transactor.xa)
      createSchemaIfNotPresent <- initializeSchema(transactor)
    } yield questRepo

    setup
  }

  test(".create() - should find and return the quest if quest_id exists for a previously created quest") { questRepo =>

    val createQuest =
      CreateQuest(
        questId = "QUEST016",
        clientId = "USER008",
        rank = Demonic,
        title = "a fake title",
        description = Some("a fake description"),
        acceptanceCriteria = "fake acceptance criteria"
      )

    for {
      createResult <- questRepo.create(createQuest)
    } yield expect(createResult == Valid(CreateSuccess))
  }
}
