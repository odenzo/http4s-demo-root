import java.nio.charset.Charset
import scala.jdk.CollectionConverters.*
val x = Charset.availableCharsets().asScala

x.foreach(println)
