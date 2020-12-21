package cdecl.declspec.appconfig

import cats.data.{EitherNel, NonEmptyList}
import cats.implicits._
import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
final case class AppSettings(kafka: KafkaCommonSettings,
                       retrySeconds: Int,
                       timeoutMillis: Int,
                       dbConfig: DbConfig)

object AppSettings {

  val bsServerKey = "bootstrapserver"
  val topicKey = "topic"
  val dbUrlKey = "dburl"
  val dbUserKey = "dbuser"
  val dbPasswordKey = "dbpassword"
  val retrySecondsKey = "retryseconds"
  val timeoutMillisKey = "timeoutmillis"

  val keys = List(bsServerKey, topicKey, dbUrlKey, dbUserKey, dbPasswordKey, retrySecondsKey, timeoutMillisKey)

  private def getNonEmptyWarning(propName: String) = s"$propName must be nonempty"

  private def getPositiveIntWarning(propName: String) = s"$propName must be positive integer"

  private def checkNonEmpty(value: String, propKey: String) = {
    if (value.isEmpty)
      getNonEmptyWarning(propKey).asLeft[String]
    else
      value.asRight[String]
  }

  import scala.util.control.Exception._

  private def makeInt(s: String): Option[Int] = allCatch.opt(s.toInt)

  private def checkPosInt(value: String, propKey: String) =
    makeInt(value) match {
      case Some(intValue) if intValue >= 0 => intValue.asRight[String]
      case _ => getPositiveIntWarning(propKey).asLeft[Int]
    }

  private def parsePair(pair: (String, String)): Option[EitherNel[String, AppSettingProps]] = {
    val (name, value) = pair
    val parsedOpt = name match {
      case `bsServerKey` => Some(checkNonEmpty(value, bsServerKey).map(s => AppSettingProps(bootStrapServer = Some(s))))
      case `topicKey` => Some(checkNonEmpty(value, topicKey).map(s => AppSettingProps(topic = Some(s))))
      case `dbUrlKey` => Some(checkNonEmpty(value, dbUrlKey).map(s => AppSettingProps(dbUrl = Some(s))))
      case `dbUserKey` => Some(checkNonEmpty(value, dbUserKey).map(s => AppSettingProps(dbUser = Some(s))))
      case `dbPasswordKey` => Some(checkNonEmpty(value, dbPasswordKey).map(s => AppSettingProps(dbPassword = Some(s))))
      case `retrySecondsKey` => Some(checkPosInt(value, retrySecondsKey).map(d => AppSettingProps(retrySeconds = Some(d))))
      case `timeoutMillisKey` => Some(checkPosInt(value, timeoutMillisKey).map(d => AppSettingProps(timeoutMillis = Some(d))))
      case _ => None
    }
    parsedOpt.map(_.toEitherNel)
  }

  def fromEnvs(envs: Map[String, String]): Either[String, AppSettings] = {
    val pairs = envs.toList.map { case (name, value) => (name.toLowerCase, value) }
    val absentProps = keys.toSet.diff(pairs.map(_._1).toSet).toList
    (absentProps.map(n => (n, "")) ::: pairs)
      .map(parsePair)
      .collect { case Some(value) => value } match {
      case ::(head, next) =>
        NonEmptyList(head, next)
          .parSequence
          .map(_.reduce)
          .leftMap(_.toList.mkString(","))
          .flatMap(_.toAppSettings.toRight("some value absent"))
      case Nil => s"${keys.mkString(",")} are absent ".asLeft[AppSettings]
    }
  }
}
