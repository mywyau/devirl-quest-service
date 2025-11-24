package configuration.constants

import configuration.AppConfig
import configuration.models.*

object ProdAppConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = false,
      useCors = false,
      useHttpsLocalstack = true,
      useProdStripe = false
    )

  val devIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 8080,
      baseUrl = "https://devirl.com"
    )

  val appServerConfig =
    ServerConfig(
      host = "0.0.0.0",
      port = 8080
    )

  val containerPostgreSqlConfig =
    PostgresqlConfig(
      dbName = "dev_quest_db",
      dockerHost = "dev-quest-container",
      host = "localhost",
      port = 5431,
      username = "dev_quest_user",
      password = "turnip",
      maxPoolSize = 42
    )

  val redisConfig =
    RedisConfig(
      dockerHost = "redis-container",
      host = "localhost",
      port = 6379
    )

  val prodAppConfig =
    ProdAppConfig(
      devIrlFrontendConfig = devIrlFrontendConfig,
      kafkaConfig = kafkaConfig,
      serverConfig = appServerConfig,
      postgresqlConfig = containerPostgreSqlConfig,
      redisConfig = redisConfig
    )

  val kafkaConfig =
    KafkaConfig(
      bootstrapServers = "localhost:9092",
      clientId = "devirl-quest-service",
      acks = "all",
      lingerMs = 5,
      retries = 10,
      topic = KafkaTopicConfig("quest.events.v1")
    )

  val prodAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      kafka = kafkaConfig,
      devIrlFrontendConfig = devIrlFrontendConfig,
      serverConfig = appServerConfig,
      postgresqlConfig = containerPostgreSqlConfig,
      redisConfig = redisConfig
    )
}
