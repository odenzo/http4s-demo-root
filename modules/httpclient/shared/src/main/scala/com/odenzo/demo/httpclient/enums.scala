package com.odenzo.demo.httpclient

import scala.util.Try

import org.latestbit.circe.adt.codec._

/** */
enum TestXmlType derives JsonTaggedAdt.PureEncoder, JsonTaggedAdt.PureDecoder:
  case TXT, BINARY, SXML_TEXT, SXML_BINARY, TXT_SNIFFED
