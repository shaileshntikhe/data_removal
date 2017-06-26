import com.datastax.driver.core._
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * Created by GS-1159 on 08-06-2017.
  */

object DBUtils extends Logging with Config {

  import logger.info

  val nodes = config.getStringList("cassandra.nodes").asScala.map(_.trim)

  private val cluster = new Cluster.Builder().addContactPoints(nodes: _*).build()
  private val session = cluster.connect()

  val fetchAllCustomersPS = session.prepare(
    s"select id, vendorid, registeredon, loginname, customermeta, lastname, migrated, name, password, scope, status, verified from vcs.customer"
  )

  val fetchAllDevicesPS = session.prepare(
    s"select vendorid, customerid, deviceid, registrationdate, lastheartbeatat, configuration, issecure, metadata, tokenstr from dms.device"
  )

  val deleteCustomerPS = session.prepare(s"delete from vcs.customer where id = ? and vendorid = ?")

  val deleteDevicePS = session.prepare(s"delete from dms.device where vendorid = ? and customerid = ? and deviceid = ?")

  val deleteDeviceStatusPS = session.prepare(s"delete from dms.devicestatus where vendorid = ? and customerid = ? and deviceid = ?")


  def deleteInvalidCustomers(customers: Set[Customer], keepDate: DateTime) = {

    val resultSet = session.execute(fetchAllCustomersPS.bind)

    var processedRows: Int = 0
    var invalidRows: Int = 0

    for (row <- resultSet) {
      if (!resultSet.isFullyFetched && resultSet.getAvailableWithoutFetching == 100) {
        resultSet.fetchMoreResults()
        info(s"fetching more customers from DB, processed rows: $processedRows")
      }
      // consider only auguest data
      if (Option(row.getInt("vendorid")).exists(_ == 16001))
        deleteIfInvalidRow(row)
      processedRows += 1
    }

    info(s"total deleted customer row count: $invalidRows")

    def deleteIfInvalidRow(row: Row) = {
      if (isInvalidRow(row)) {
        info(s"deleting customer row: $row")
        invalidRows += 1
        deleteRow(row)
      }
      else {
        info(s"skipping customer row: $row")
      }
    }

    def deleteRow(row: Row) = {
      session.execute(deleteCustomerPS.bind(row.getString("id"), row.getInt("vendorid").asInstanceOf[Integer]))
    }

    def isInvalidRow(row: Row): Boolean = {
      // registration date is less than keepdate and customer doesn't exist in data provided by august
      getDate(row, "registeredon").exists(keepDate.isAfter) &&
        !customers.contains(Customer(row.getString("id").trim, row.getString("loginname").trim))
    }

  }

  def deleteInvalidDevices(devices: Set[Device], keepDate: DateTime) = {

    val resultSet = session.execute(fetchAllDevicesPS.bind)

    var processedRows: Int = 0
    var invalidRows: Int = 0

    for (row <- resultSet) {
      if (!resultSet.isFullyFetched && resultSet.getAvailableWithoutFetching == 100) {
        resultSet.fetchMoreResults()
        info(s"fetching more devices from DB, processed rows: $processedRows")
      }
      // consider only auguest data
      if (Option(row.getLong("vendorid")).exists(_ == 16001))
        deleteIfInvalidRow(row)
      processedRows += 1
    }

    info(s"total deleted device row count: $invalidRows")

    def deleteIfInvalidRow(row: Row) = {
      if (isInvalidRow(row)) {
        info(s"deleting device row: $row")
        invalidRows += 1
        deleteDeviceWithStatus(row)
      }
      else {
        info(s"skipping device row: $row")
      }
    }

    def deleteDeviceWithStatus(row: Row) = {
      session.execute(deleteDevicePS.bind(row.getLong("vendorid").asInstanceOf[Object], row.getString("customerid"), row.getString("deviceid")))
      session.execute(deleteDeviceStatusPS.bind(row.getLong("vendorid").asInstanceOf[Object], row.getString("customerid"), row.getString("deviceid")))
    }

    def isInvalidRow(row: Row): Boolean = {
      // registration date is less than keepdate and device doesn't exist in data provided by august
      getDate(row, "registrationdate").exists(keepDate.isAfter) &&
        !devices.contains(Device(row.getString("deviceid").trim, row.getString("customerid").trim))
    }

  }

  def getDate(row: Row, dateColumn: String): Option[Long] = {
    if (row.isNull(dateColumn))
      None
    else
      Some(row.getDate(dateColumn).getTime)
  }

  def closeCluster = cluster.close()

  def closeSession = session.close()

}