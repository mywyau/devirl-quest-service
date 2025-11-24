package configuration

import cats.kernel.Eq
import configuration.models.*
import pureconfig.ConfigReader
import pureconfig.generic.derivation.*

case class AppConfig(
  devIrlFrontendConfig: DevIrlFrontendConfig,
  featureSwitches: FeatureSwitches,
  kafka: KafkaConfig,
  postgresqlConfig: PostgresqlConfig,
  serverConfig: ServerConfig,
  redisConfig: RedisConfig
) derives ConfigReader
