import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsArray
import scala.util.{Failure, Success}

/**
  * Created by GS-1159 on 08-06-2017.
  */
object Runner extends App with Logging with Config {

  import logger.{error, info}

  info(s"app started")

  val jsonFileName = config.getString("august.data.file")

  val jsonData = JsonReader.parse(jsonFileName)

  val jsonSeq = jsonData match {
    case Success(json) =>
      json.asInstanceOf[JsArray].value
    case Failure(th) =>
      error(s"error occured while deserializing json file, exception: $th")
      th.printStackTrace
      List.empty
  }

  //if there is error while parsing json, do not proceed
  assert(jsonSeq.nonEmpty, "error occured while deserializing data")

  val customerJson = jsonSeq.map(CustomerUtils.getCustomer)
  val deviceJson = jsonSeq.map(DeviceUtils.getDevice)

  //if there is at least one customer whose data can't be deserialized, do not proceed
  assert(customerJson.forall(_.isDefined), "some customer data can't be deserialized")

  //if there is at least one device whose data can't be deserialized
  assert(deviceJson.forall(_.isDefined), "some device data can't be deserialized")

  val customers = customerJson.map(_.get).toSet
  val devices = deviceJson.map(_.get).toSet

  // august has taken dump Nov 29th, at 17:56 PST, converting this time to UTC i.e. 2017 Nov 30, 01:00:00 AM,
  val keepDate = new DateTime(2016, 12, 1, 0, 0, 0, DateTimeZone.UTC)
  info(s"keepDate is: $keepDate")

  DBUtils.deleteInvalidCustomers(customers, keepDate)
  DBUtils.deleteInvalidDevices(devices, keepDate)

  info(s"clean up completed, closing cassandra session")

  DBUtils.closeSession
  DBUtils.closeCluster

  info(s"app completed")

}