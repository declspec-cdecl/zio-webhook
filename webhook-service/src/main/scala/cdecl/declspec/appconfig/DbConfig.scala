package cdecl.declspec.appconfig

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class DbConfig(url: String,
                    user: String,
                    password: String)
