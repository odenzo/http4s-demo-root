package com.odenzo.base

import java.nio.charset.spi.CharsetProvider
import java.nio.charset.{CharacterCodingException, Charset}
import scodec.bits.*

/** XML DOM Utilities including attempt at encoding sniffing. Falls back to UTF-8 */
object BOMUtils {

  val bomMappings = Map(
    hex"EFBBBF"      -> Charset.forName("UTF-8"),
    hex"FEFF"        -> Charset.forName("UTF-16BE"),
    hex"FFFE"        -> Charset.forName("UTF-16LE"),
    hex"00 00 FE FF" -> Charset.forName("UTF-32BE"),
    hex"FF FE 00 00" -> Charset.forName("UTF-32LE")
  )

  val xmlIn: ByteVector = ByteVector.encodeAscii("<?xml").getOrElse(throw Throwable("Init Error on closeElem"))
  val closeElem: Byte   = ByteVector.encodeAscii(">").map(_.head).getOrElse(throw Throwable("Init Error on closeElem"))

  val encodingInXmlPI = """^<?xml.*encoding=['|"](.*)['|"]""".r
  val encodingAttr    = """encoding=['|"](.*)['|"]""".r

  // ByteVector should be at least 32 bytes for safety.
  def detectBom(xml: ByteVector): Option[Charset] =
    bomMappings.collectFirst { case (prefix, charset) if xml.startsWith(prefix) => charset }

  /** Extracts and <?xml prolog encoding attribute anchored at the start of this String.
    * @return
    *   Maybe CharSet if encoding is found, throws if system doesn't support the given encoding
    */
  def detectXmlPiEncoding(s: String) = {
    s match {
      case encodingInXmlPI(encoding) => Some(Charset.forName(encoding))
      case _                         => None
    }

  }

  /** This assumes you have checked for DOM and none so sniff not by regex but by ASCII encoding of encoding=" */
  def detectXmlPiEncoding(bv: ByteVector): Option[Charset] = {
    // Kinda wacky but safety. Prolog with NO BOM is found, then the prolog exists, w or w/o encoding. But, don't convert prolog to text
    // without searching for the end of the prolog <  in case stuff after prolog is not deodable in UTF-8. Then decode that and regex it.
    if bv.startsWith(xmlIn) then
      bv.takeWhile(b => b != closeElem).decodeUtf8 match {
        case Left(err)  =>
          scribe.warn(s"Problem decodeing XML Prolog as UTF-8: ${err.toString}")
          None
        case Right(str) => detectXmlPiEncoding(str)
      }
    else None
  }

}
