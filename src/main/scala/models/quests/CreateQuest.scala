package models.quests

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.Decoder
import io.circe.Encoder
import java.time.LocalDateTime
import models.languages.Language
import models.QuestStatus
import models.Rank

case class CreateQuest(
  questId: String,
  clientId: String,
  rank: Rank,
  title: String,
  description: Option[String],
  acceptanceCriteria: String,
  // tags: Seq[Language]
)

object CreateQuest {
  implicit val encoder: Encoder[CreateQuest] = deriveEncoder[CreateQuest]
  implicit val decoder: Decoder[CreateQuest] = deriveDecoder[CreateQuest]
}
