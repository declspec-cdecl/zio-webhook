package cdecl.declspec.workerfactory

import cdecl.declspec.appconfig.KafkaCommonSettings

case class KafkaSettings(common: KafkaCommonSettings, groupName: String)

