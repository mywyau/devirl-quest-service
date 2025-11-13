package repositories

import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.Monad
import configuration.AppConfig
import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import fs2.Stream
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import models.database.*
import models.quests.*
import models.Open
import models.QuestStatus
import models.Rank
import org.typelevel.log4cats.Logger

trait QuestRepositoryAlgebra[F[_]] {

  def create(request: CreateQuest): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]]
}

class QuestRepositoryImpl[F[_] : Concurrent : Monad : Logger](transactor: Transactor[F]) extends QuestRepositoryAlgebra[F] with DoobieMetas {

  override def create(request: CreateQuest): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]] = ???
}

object QuestRepository {
  
  def apply[F[_] : Concurrent : Monad : Logger](transactor: Transactor[F]): QuestRepositoryAlgebra[F] =
    new QuestRepositoryImpl[F](transactor)
}
