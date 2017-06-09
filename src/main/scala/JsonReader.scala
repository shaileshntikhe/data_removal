import java.io.{FileInputStream, InputStream}
import play.api.libs.json.{JsValue, Json}
import scala.util.Try

/**
  * Created by GS-1159 on 08-06-2017.
  */
object JsonReader {

  def parse(filePath: String): Try[JsValue] =
    parse(new FileInputStream(filePath))

  def parse(inputStream: InputStream): Try[JsValue] = Try(Json.parse(inputStream))

}