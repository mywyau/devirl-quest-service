package shared

import cats.effect.IO
import fs2.kafka.KafkaProducer

final case class KafkaProducerResource(producer: KafkaProducer[IO, String, String])
