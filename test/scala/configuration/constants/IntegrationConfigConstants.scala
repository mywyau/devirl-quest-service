package configuration.constants

import configuration.models.*
import configuration.AppConfig

object IntegrationConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = false,
      useCors = false,
      useHttpsLocalstack = true,
      useProdStripe = false
    )

  val itDevIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 3000,
      baseUrl = "http://localhost:3000"
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

  val itSpecServerConfig =
    ServerConfig(
      host = "127.0.0.1",
      port = 9999
    )

  val itPostgresqlConfig =
    PostgresqlConfig(
      dbName = "dev_quests_test_db",
      dockerHost = "dev-quests-db-it",
      host = "localhost",
      port = 5432,
      username = "dev_quests_test_user",
      password = "turnip",
      maxPoolSize = 42
    )

  val itRedisConfig =
    RedisConfig(
      dockerHost = "redis-test-container",
      host = "localhost",
      port = 6380
    )

  val integrationSpecConfig =
    IntegrationSpecConfig(
      devIrlFrontendConfig = itDevIrlFrontendConfig,
      serverConfig = itSpecServerConfig,
      kafkaConfig = kafkaConfig,
      postgresqlConfig = itPostgresqlConfig,
      redisConfig = itRedisConfig
    )

  val integrationAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      kafka = kafkaConfig,
      devIrlFrontendConfig = itDevIrlFrontendConfig,
      serverConfig = itSpecServerConfig,
      postgresqlConfig = itPostgresqlConfig,
      redisConfig = itRedisConfig
    )
}
