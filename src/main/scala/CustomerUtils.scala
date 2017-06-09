import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by GS-1159 on 08-06-2017.
  */
object CustomerUtils extends Logging {

  import logger.{debug, error}

  implicit val customerRead: Reads[Customer] = (
    (JsPath \ "intellivisionCreds" \ "customerID").read[String] and
      (JsPath \ "intellivisionCreds" \ "email").read[String]
    ) (Customer.apply _)

  def getCustomer(jsValue: JsValue): Option[Customer] = {
    debug(s"processing customer:\n$jsValue")
    jsValue.validate[Customer] match {
      case cust: JsSuccess[Customer] =>
        cust.map(c => Customer(c.id.trim, c.emailId.trim)).asOpt
      case err: JsError =>
        error(s"error occured: $err, while deserializing customer $jsValue")
        None
    }
  }

}

case class Customer(id: String, emailId: String)