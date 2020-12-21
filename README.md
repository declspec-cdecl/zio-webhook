#Webhook service

Webhook сервис, обеспечивающий возможность их создания, изменения, удаления.
Указанные выше операции осуществляются с помощью json rest интерфейса. В качестве 
источчника событий выступает топик в Apache Kafka.

##Детали реализации

###Состав проекта

Проект содержит следующие артефакты:
* __webhook-service__ - сам сервис вебхуков
* __simple-http-event-consumer__ - json rest сервис, реализующий единственный post endpoint. Предназначен для 
тестирования отправки данных сервисом __webhook-service__
* __simple-kafka-sender__ - сервис реализующий отправку сообщений с заданным интервалом в указанный топик. Сообщения 
представляют собой целый числа, монотонно возрастающие с шагом в 1. Предназначен для тестирования сервиса 
__webhook-service__
* __docker-compose.yaml__ - файл конфигурации набора контейннеров для интеграционного тестирования

###Используемый стек

При реализации были использованы следующие основные библиотеки:
* zio
* zio-kafka
* tofu
* circe
* sttp
* tapir
* http4s
* doobie
* flyway
* zio-test
* cats

###Архитектура сервиса __webhook-service__

Сервис __webhook-service__ реализован в виде json rest сервиса. Он предоставляет возможности по управлению вебхуками. 
Внутренне помимо обработчика http запросов ```HttpService``` сервис содержит отдельный обработчик команд 
```SubscriptionHandler``` по управлению вебхуками. Команды представлены трейтом ```SubscriptionCommand```, который 
содержит варианты для создания вебхука ```Subscribe```, удаления вебхука ```UnSubscribe``` и обновления ```Update```. 
Обработчик комманд ```SubscriptionHandler``` связан c обрабочиком http ```HttpService``` через компонент 
```SubscriptionQueue```. В основе ```SubscriptionQueue``` лежит очередь queue из zio, позволяющая организовать 
взаимодействие между   ```HttpService``` и ```SubscriptionHandler```. ```SubscriptionHandler``` с помощью модуля 
```WorkerFactory``` создает обработчик ```KafkaWorker```, который с использованием zio-kafka создает ```ZStream``` 
сообщений из заданного топика. Эти сообщения отправляются по указанному url с помощью модуля ```HookSender```. 
```KafkaWorker``` обеспечивает семантику чтения at least once. ```HttpService``` также предоставляет OpenAPI документацию 
по url __/docs__.

##Тестирование

###docker-compose.yaml

docker-compose.yaml позволяет осуществить интеграционное тестирование сервиса __webhook-service__. В рамки создаваемого 
им набора входят следующие сервисы:
* db - инстанс Postgres
* zookeeper - инстанс ZooKeeper
* kafka - инстанс Apache Kafka
* sender - инстанс сервиса __simple-kafka-sender__ для записи в kafka потока событий
* consumer1 и consumer2 - инстансы __simple-http-event-consumer__ для получения http колбэков
* webhook - инстанс __webhook-service__

###unit тесты
Для тестирования модуля  ```HttpService``` реализован набор тестов ```HttpServiceLogicSpec```.
Для тестирования модуля  ```AppConfig``` реализован набор тестов ```AppSettingsParsingSpec```. 
Для тестирования функционала обработки сообщений реализован набор тестов ```KafkaWorkerSpec```
 
