package cdecl.declspec

import cats.effect.Blocker
import cdecl.declspec.appconfig.AppConfig
import cdecl.declspec.appconfig.appSettings
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import zio.{Has, RLayer, Task, URIO, ZIO, ZLayer}
import zio.blocking.{Blocking, blocking}

package object db {

  type DB = Has[DB.Service]

  object DB {

    case class Service(transactor: Transactor[Task])

    private def initialize(transactor: HikariTransactor[Task], b: Blocking) = {
      transactor.configure(ds => blocking {
        ZIO {
          val flyWay = Flyway.configure().dataSource(ds).load()
          flyWay.migrate()
          ()
        }
      }.provide(b))
    }

    val live: RLayer[AppConfig with Blocking, DB] = {
      import zio.interop.catz._
      ZLayer.fromManaged(
        for {
          liveEC <- ZIO.descriptor.map(_.executor.asEC).toManaged_
          blockEC <- blocking(ZIO.descriptor.map(_.executor.asEC)).toManaged_
          conf <- appSettings.map(_.dbConfig).toManaged_
          trans <- HikariTransactor
            .newHikariTransactor[Task](
              "org.postgresql.Driver",
              conf.url,
              conf.user,
              conf.password,
              liveEC,
              Blocker.liftExecutionContext(blockEC)
            )
            .toManagedZIO
          blocker <- ZIO.environment[Blocking].toManaged_
          _ <- initialize(trans, blocker).toManaged_
        } yield Service(trans)
      )
    }
  }

  val getDb: URIO[DB, DB.Service] = URIO.access(_.get)
}
