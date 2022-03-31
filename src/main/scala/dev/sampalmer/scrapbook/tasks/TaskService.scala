package dev.sampalmer.scrapbook.tasks

import cats.effect.Sync
import cats.implicits._
import dev.sampalmer.scrapbook.tasks.TaskService.Task
import io.chrisdavenport.fuuid.FUUID

import java.util.UUID

trait TaskService[F[_]] {
  def add(userId: UUID, taskDescription: String): F[Task]
}

object TaskService {
  case class Task(id: FUUID, userId: UUID, description: String)

  def apply[F[_]: Sync](): TaskService[F] = new TaskService[F] {

    override def add(userId: UUID, taskDescription: String): F[Task] =
      FUUID.randomFUUID.map(uuid => Task(uuid, userId, taskDescription))
  }
}
