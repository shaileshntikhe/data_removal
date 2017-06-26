import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by GS-1159 on 08-06-2017.
  */
object DeviceUtils extends Logging {

  import logger.{debug, error}

  implicit val customerRead: Reads[Device] = (
    (JsPath \ "intellivisionCreds" \ "deviceID").read[String] and
      (JsPath \ "intellivisionCreds" \ "customerID").read[String]
    ) (Device.apply _)

  def getDevice(jsValue: JsValue): Option[Device] = {
    debug(s"processing device:\n$jsValue")
    jsValue.validate[Device] match {
      case dev: JsSuccess[Device] =>
        dev.map(d => Device(d.id.trim, d.customerId.trim)).asOpt
      case err: JsError =>
        error(s"error occured: $err, while deserializing device $jsValue")
        None
    }
  }

}

case class Device(id: String, customerId: String)