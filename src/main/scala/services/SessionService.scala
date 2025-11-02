package services

import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import cats.Monad
import cats.NonEmptyParallel
import fs2.Stream
import infrastructure.cache.SessionCacheAlgebra
import java.util.UUID
import models.auth.UserSession
import models.cache.*
import models.users.*
import models.Dev
import models.UnknownUserType
import org.typelevel.log4cats.Logger

trait SessionServiceAlgebra[F[_]] {

  def getSessionCookieOnly(userId: String): F[Option[String]]

  def getSession(userId: String): F[Option[UserSession]]

  def storeOnlyCookie(userId: String, token: String): F[Unit]
}

class SessionServiceImpl[F[_] : Concurrent : Monad : Logger](
  sessionCache: SessionCacheAlgebra[F]
) extends SessionServiceAlgebra[F] {

  override def getSessionCookieOnly(userId: String): F[Option[String]] =
    sessionCache.getSessionCookieOnly(userId)

  override def getSession(userId: String): F[Option[UserSession]] =
    sessionCache.getSession(userId)

  override def storeOnlyCookie(userId: String, cookieToken: String): F[Unit] =
    sessionCache.storeOnlyCookie(userId, cookieToken)
}

object SessionService {

  def apply[F[_] : Concurrent : Logger](
    sessionCache: SessionCacheAlgebra[F]
  ): SessionServiceAlgebra[F] =
    new SessionServiceImpl[F](sessionCache)
}
