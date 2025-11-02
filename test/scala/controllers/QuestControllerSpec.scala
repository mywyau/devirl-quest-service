// package controllers

// import cats.effect.IO
// import cats.effect.Ref
// import controllers.ControllerSpecBase
// import controllers.QuestController
// import controllers.QuestControllerConstants.*
// import infrastructure.cache.SessionCacheAlgebra
// import mocks.MockSessionCache
// import models.auth.UserSession
// import org.http4s.*
// import org.http4s.implicits.*
// import org.http4s.Method.*
// import org.http4s.Status.Ok
// import services.QuestCRUDServiceAlgebra
// import weaver.SimpleIOSuite

// object QuestControllerSpec extends SimpleIOSuite with ControllerSpecBase {

//   def createQuestController(
//     sessionCache: SessionCacheAlgebra[IO],
//     questCrudService: QuestCRUDServiceAlgebra[IO]
//   ): HttpRoutes[IO] =
//     QuestController[IO](sessionCache, questCrudService).routes

//   test("GET - /quest/USER001/QUEST001 should return 200 when quest is retrieved successfully") {

//     val sessionToken = "test-session-token"
//     val fakeUserSession = UserSession(
//       userId = "USER001",
//       cookieValue = sessionToken,
//       email = "fakeEmail@gmail.com",
//       userType = "Dev"
//     )
//     val mockQuestCRUDService = new MockQuestCRUDService(Map("QUEST001" -> sampleQuest1))
//     val request = Request[IO](Method.GET, uri"/quest/USER001/QUEST001")

//     for {
//       // create an in-memory session cache and pre-populate it
//       sessionCache <- MockSessionCache.make[IO]
//       _ <- sessionCache.storeSession("USER001", Some(fakeUserSession))

//       controller = createQuestController(sessionCache, mockQuestCRUDService)
//       response <- controller.orNotFound.run(
//         request.addCookie("auth_session", sessionToken)
//       )
//     } yield expect(response.status == Ok)
//   }
// }
