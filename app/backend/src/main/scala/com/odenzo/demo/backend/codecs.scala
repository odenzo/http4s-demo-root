package com.odenzo.demo.httpclient

// FPath is JVM Only :-(
import fs2.io.file.Path as FPath
import io.circe.*

import scala.util.Try

given Codec[FPath] = Codec.from(Decoder.decodeString.emapTry(s => Try(FPath(s))), Encoder.encodeString.contramap(fpath => fpath.toString))
