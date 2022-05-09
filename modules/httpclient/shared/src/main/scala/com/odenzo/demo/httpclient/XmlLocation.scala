package com.odenzo.demo.httpclient

import io.circe.*
import io.circe.generic.*

/** A ScalaJS Browser represenation of Path, we can't use real Java Path */
case class XmlLocation(path: String)

given Codec[XmlLocation] = Codec.from(Decoder.decodeString.map(XmlLocation.apply), Encoder.encodeString.contramap(_.path))
