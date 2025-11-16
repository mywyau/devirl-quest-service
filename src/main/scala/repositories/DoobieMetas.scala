package repositories

import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta
import java.sql.Timestamp
import java.time.LocalDateTime
import models.QuestStatus
import models.Rank

trait DoobieMetas {

  implicit val questStatusMeta: Meta[QuestStatus] = Meta[String].timap(QuestStatus.fromString)(_.toString)

  implicit val rank: Meta[Rank] = Meta[String].timap(Rank.fromString)(_.toString)

  implicit val localDateTimeMeta: Meta[LocalDateTime] = Meta[Timestamp].imap(_.toLocalDateTime)(Timestamp.valueOf)

  implicit val metaStringList: Meta[Seq[String]] = Meta[Array[String]].imap(_.toSeq)(_.toArray)
}
