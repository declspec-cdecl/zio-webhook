package cdecl.declspec.subscriptionhandler

import cdecl.declspec.appconfig.AppSettings
import cdecl.declspec.models.subscriptions.Subscription
import cdecl.declspec.workerfactory.{HookWorkerSettings, KafkaSettings, WebhookSettings}

object WorkerSettingsFactory {

  def getWorkerSettings(settings: AppSettings, subscription: Subscription): HookWorkerSettings = {
    val hookSettings = WebhookSettings(subscription.url, settings.timeoutMillis, settings.retrySeconds)
    val kafkaSettings = KafkaSettings(settings.kafka, s"group${subscription.id}")
    HookWorkerSettings(hookSettings, kafkaSettings)
  }
}
