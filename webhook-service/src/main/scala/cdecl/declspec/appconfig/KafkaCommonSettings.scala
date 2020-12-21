package cdecl.declspec.appconfig

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class KafkaCommonSettings (bootStrapServers: List[String], topic: String)
