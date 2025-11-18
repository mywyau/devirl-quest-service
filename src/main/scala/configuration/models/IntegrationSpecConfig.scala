package configuration.models

import cats.kernel.Eq
import configuration.models.ServerConfig
import pureconfig.ConfigReader
import pureconfig.generic.derivation.*

case class IntegrationSpecConfig(
  devIrlFrontendConfig: DevIrlFrontendConfig,
  kafkaConfig: KafkaConfig,
  postgresqlConfig: PostgresqlConfig,
  redisConfig: RedisConfig,
  serverConfig: ServerConfig
) derives ConfigReader
