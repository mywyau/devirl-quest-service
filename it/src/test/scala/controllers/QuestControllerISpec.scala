package controllers.quest

import cats.effect.*
import cats.effect.IO
import cats.implicits.*
import controllers.ControllerISpecBase
import controllers.fragments.QuestControllerFragments.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.kafka.AutoOffsetReset
import fs2.kafka.ConsumerSettings
import fs2.kafka.KafkaConsumer
import fs2.text.lines
import fs2.text.utf8Decode
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*
import models.Completed
import models.Demonic
import models.InProgress
import models.NotStarted
import models.auth.UserSession
import models.database.*
import models.events.QuestCreatedEvent
import models.kafka.SuccessfulWrite
import models.languages.*
import models.quests.*
import models.responses.*
import org.http4s.*
import org.http4s.MediaType
import org.http4s.Method.*
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.Status
import org.http4s.Uri
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shared.HttpClientResource
import shared.TransactorResource
import test_data.ITestConstants.*
import weaver.*

import java.time.Instant
import java.time.LocalDateTime
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.*

class QuestControllerISpec(global: GlobalRead) extends IOSuite with ControllerISpecBase {
  type Res = (TransactorResource, HttpClientResource)

  def sharedResource: Resource[IO, Res] =
    for {
      transactor <- global.getOrFailR[TransactorResource]()
      _ <- Resource.eval(
        createQuestTable.update.run.transact(transactor.xa).void *>
          resetQuestTable.update.run.transact(transactor.xa).void *>
          insertQuestData.update.run.transact(transactor.xa).void *>
          insertQuestDataNoDevId.update.run.transact(transactor.xa).void
      )
      client <- global.getOrFailR[HttpClientResource]()
    } yield (transactor, client)

  val sessionToken = "test-session-token"

  def fakeUserSession(clientId: String) =
    UserSession(
      userId = "USER001",
      cookieValue = sessionToken,
      email = "fakeEmail@gmail.com",
      userType = "Dev"
    )

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

  test(
    "GET - /devirl-quest-service/quest/USER001/QUEST001 - should find the quest data for given quest id, returning OK and the correct quest json body"
  ) { (sharedResource, log) =>

    val transactor = sharedResource._1.xa
    val client = sharedResource._2.client

    val sessionToken = "test-session-token"

    val request =
      Request[IO](GET, uri"http://127.0.0.1:9999/devirl-quest-service/quest/health")

    client.run(request).use { response =>
      response.as[GetResponse].map { body =>
        expect.all(
          response.status == Status.Ok,
          body == GetResponse("/devirl-quest-service/health", "I am alive")
        )
      }
    }
  }

  test(
    "POST - /devirl-quest-service/quest/create/USER006 - should generate the quest data in db table and publish an event to kafka - returning a Created response"
  ) { (sharedResource, log) =>

    val transactor = sharedResource._1.xa
    val client = sharedResource._2.client

    val topic = "quest.created.v1.test"

    val sessionToken = "test-session-token"

    def testCreateQuest(): CreateQuestPartial =
      CreateQuestPartial(
        rank = Demonic,
        title = "Implement User Authentication - also test event being created",
        description = Some("Set up Auth0 integration and secure routes using JWT tokens."),
        acceptanceCriteria = "Some acceptance criteria",
        tags = Seq(Python, Scala, TypeScript)
      )

    val createRequest: Json = testCreateQuest().asJson

    val request =
      Request[IO](POST, uri"http://127.0.0.1:9999/devirl-quest-service/quest/create/USER006")
        .withEntity(createRequest)
        .addCookie("auth_session", sessionToken)

    val expectedBody = CreatedResponse(SuccessfulWrite.toString, "quest details created successfully")

    for {
      _ <- resetKafkaTopic(topic)
      _ <- logger.info(s"[QuestControllerISpec] Sending event to topic $topic")
      testResult <- client.run(request).use { response =>
        for {
          raw <- response.bodyText.compile.string
          _ <- logger.info(s"[QuestControllerISpec] RAW RESPONSE BODY \n$raw")
          _ <- logger.info(s"[QuestControllerISpec] STATUS ${response.status}")
          _ <- logger.info("[QuestControllerISpec] COOKIES: " + request.cookies.toString)
          decoded <- IO.fromEither(io.circe.parser.decode[CreatedResponse](raw))
        } yield expect.all(
          response.status == Status.Created,
          decoded == expectedBody
        )
      }
      _ <- deleteTopic(topic)
    } yield testResult
  }
}
