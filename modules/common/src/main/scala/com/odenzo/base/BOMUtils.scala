package com.odenzo.base

import java.nio.charset.spi.CharsetProvider
import java.nio.charset.{CharacterCodingException, Charset}
import scodec.bits.*

/** XML DOM Utilities including attempt at encoding sniffing. Falls back to UTF-8 */
object BOMUtils {
  import java.nio.charset.StandardCharsets

  /** This is a little odd, because UTF-16 IS WITH A BOM and 16BE and LE charsets for without a BOM. So, if we keep BOM (e.g. peeking the
    * byes, go UTF_16, is we consume DOM then the BE/LE
    */
  val bomMappings = Map(
    hex"EFBBBF"      -> StandardCharsets.UTF_8,  // Direct works, but we need to drop the BOM for UTF-8 or? JUST DON"T DEAL WITH IT
    hex"FEFF"        -> StandardCharsets.UTF_16, // UTF016 will get BE, UTF-16BE requires no BOM
    hex"FFFE"        -> StandardCharsets.UTF_16, // LE but LE has no BOM, UTF-16 does
    hex"00 00 FE FF" -> Charset.forName("UTF-32"),
    hex"FF FE 00 00" -> Charset.forName("UTF-32")
  )

  val xmlIn: ByteVector = ByteVector.encodeAscii("<?xml").getOrElse(throw Throwable("Init Error on closeElem"))
  val closeElem: Byte   = ByteVector.encodeAscii(">").map(_.head).getOrElse(throw Throwable("Init Error on closeElem"))

  val encodingInXmlPI = """<\?xml .* encoding=['|"](.*?)['|"]""".r.unanchored
  val encodingAttr    = """encoding=['|"](.*)['|"]""".r

  // ByteVector should be at least 32 bytes for safety.
  def detectBom(xml: ByteVector): Option[Charset] =
    bomMappings.collectFirst { case (prefix, charset) if xml.startsWith(prefix) => charset }

  /** Extracts and <?xml prolog encoding attribute anchored at the start of this String.
    * @return
    *   Maybe CharSet if encoding is found, throws if system doesn't support the given encoding
    */
  def detectXmlPiEncoding(s: String) = {
    scribe.info(s"Detecting XMLPI Encoding in [$s]")
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
      scribe.info(s"XMLPI -- Started with xmlIn")
      bv.takeWhile(b => b != closeElem).decodeUtf8 match {
        case Left(err)  =>
          scribe.warn(s"Problem decodeing XML Prolog as UTF-8: ${err.toString}")
          None
        case Right(str) => detectXmlPiEncoding(str)
      }
    else None
  }

}
