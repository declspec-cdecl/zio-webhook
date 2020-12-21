package cdecl.declspec.workerfactory

import zio.Fiber

case class WorkerEntry(url: String, fiber: Fiber[Throwable, Unit])

