package configuration.constants

import configuration.models.*
import configuration.AppConfig

object LocalAppConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = false,
      useCors = false,
      useHttpsLocalstack = true,
      useProdStripe = false
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

  val devIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 3000,
      baseUrl = "http://localhost:3000"
    )

  val appServerConfig =
    ServerConfig(
      host = "0.0.0.0",
      port = 8080
    )

  val postgreSqlConfig =
    PostgresqlConfig(
      dbName = "dev_quests_db",
      dockerHost = "dev-quests-container",
      host = "localhost",
      port = 5431,
      username = "dev_quests_user",
      password = "turnip",
      maxPoolSize = 42
    )

  val redisConfig =
    RedisConfig(
      dockerHost = "redis-container",
      host = "localhost",
      port = 6379
    )

  val localAppConfig =
    LocalAppConfig(
      devIrlFrontendConfig = devIrlFrontendConfig,
      kafkaConfig = kafkaConfig,
      serverConfig = appServerConfig,
      postgresqlConfig = postgreSqlConfig,
      redisConfig = redisConfig
    )

  val localAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      kafka = kafkaConfig,
      devIrlFrontendConfig = devIrlFrontendConfig,
      serverConfig = appServerConfig,
      postgresqlConfig = postgreSqlConfig,
      redisConfig = redisConfig
    )
}
