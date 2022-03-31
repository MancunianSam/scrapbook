package dev.sampalmer.scrapbook.auth

import cats.data.OptionT
import cats.effect.Sync
import tsec.authentication.{AuthenticatedCookie, BackingStore}
import tsec.mac.jca.HMACSHA256

import java.util.UUID
import scala.collection.mutable

object CookieStore {
  def apply[F[_] : Sync](): BackingStore[
    F,
    UUID,
    AuthenticatedCookie[HMACSHA256, UUID]] =
    new BackingStore[F, UUID, AuthenticatedCookie[HMACSHA256, UUID]] {
      private val storageMap = mutable.HashMap.empty[UUID, AuthenticatedCookie[HMACSHA256, UUID]]

      override def put(elem: AuthenticatedCookie[HMACSHA256, UUID]): F[AuthenticatedCookie[HMACSHA256, UUID]] = {
        val map = storageMap.put(elem.id, elem)
        if (map.isEmpty)
          Sync[F].pure(elem)
        else
          Sync[F].raiseError(new IllegalArgumentException)
      }

      override def update(v: AuthenticatedCookie[HMACSHA256, UUID]): F[AuthenticatedCookie[HMACSHA256, UUID]] = {
        storageMap.update(v.id, v)
        Sync[F].pure(v)
      }

      override def delete(id: UUID): F[Unit] =
        storageMap.remove(id) match {
          case Some(_) => Sync[F].unit
          case None => Sync[F].raiseError(new Exception(""))
        }

      override def get(id: UUID): OptionT[F, AuthenticatedCookie[HMACSHA256, UUID]] =
        OptionT.fromOption(storageMap.get(id))
    }

  def empty[F[_] : Sync]: BackingStore[F, UUID, AuthenticatedCookie[HMACSHA256, UUID]] = CookieStore()
}
