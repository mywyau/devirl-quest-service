package configuration.models

import cats.kernel.Eq
import configuration.models.ServerConfig
import pureconfig.generic.derivation.*
import pureconfig.ConfigReader

case class LocalAppConfig(
  devIrlFrontendConfig: DevIrlFrontendConfig,
  kafkaConfig: KafkaConfig,
  postgresqlConfig: PostgresqlConfig,
  redisConfig: RedisConfig,
  serverConfig: ServerConfig
) derives ConfigReader
