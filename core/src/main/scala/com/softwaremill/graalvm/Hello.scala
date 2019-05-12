package com.softwaremill.graalvm

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

object Hello extends App {
  val task = Task(println(s"Hello, world from thread: ${Thread.currentThread().getName}!"))
  (for {
    fiber1 <- task.start
    _ <- task
    _ <- fiber1.join
  } yield ()).runSyncUnsafe()
}
