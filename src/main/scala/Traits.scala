import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

/**
  * Created by GS-1159 on 08-06-2017.
  */
trait Logging {
  val logger = LoggerFactory.getLogger(this.getClass)
}

trait Config {
  val config = ConfigFactory.load
}