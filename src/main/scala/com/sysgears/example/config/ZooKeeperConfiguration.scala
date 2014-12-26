package com.sysgears.example.config

import java.nio.charset.Charset
import java.util.MissingResourceException
import java.util.concurrent.TimeUnit

import com.google.common.base.Charsets
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.{RetryPolicy, retry}
import org.apache.zookeeper.KeeperException

import scala.util.Try

/**
 * Provides abilities to use ZooKeeper ensemble as a remote configuration holder.
 */
trait ZooKeeperConfiguration extends BaseConfiguration {

  /**
   * Default ZooKeeper client's connection string.
   */
  protected val DefaultConnectionString: String = config.getString("zookeeper.connectionString")

  /**
   * Default ZooKeeper client's connection timeout.
   */
  protected val DefaultConnectionTimeout: Int = config.getInt("zookeeper.connectionTimeout")

  /**
   * Default ZooKeeper client's session timeout.
   */
  protected val DefaultSessionTimeout: Int = config.getInt("zookeeper.sessionTimeout")

  /**
   * Default number of connection retries to Zookeeper ensemble.
   */
  protected val RetryAttemptsCount: Int = config.getInt("zookeeper.retryAttempts")

  /**
   * Default interval between connection retries to Zookeeper ensemble.
   */
  protected val RetryInterval: Int = config.getInt("zookeeper.retryInterval")

  /**
   * Default retry policy.
   */
  protected val DefaultRetryPolicy: RetryPolicy = new ExponentialBackoffRetry(RetryInterval, RetryAttemptsCount)

  /**
   * Lookup client timeout.
   */
  private val LookupClientTimeout: Int = 15000

  /**
   * Creates ZooKeeper remote configuration client.
   *
   * Initializes connection to the ZooKeeper ensemble.
   *
   * @param service service name
   * @param environment system environment
   * @param connectionString connection string; default value is taken from local configuration
   * @param connectionTimeout connection timeout; default value is taken from local configuration
   * @param sessionTimeout session timeout; default value is taken from local configuration
   * @param retryPolicy connection retry policy; default policy retries specified number of times with increasing
   *                    sleep time between retries
   * @param authScheme authentication scheme; null by default
   * @param authData authentication data bytes; null by default
   * @return client instance
   */
  def initZooKeeperClient(service: String,
                          environment: String = "dev",
                          connectionString: String = DefaultConnectionString,
                          connectionTimeout: Int = DefaultConnectionTimeout,
                          sessionTimeout: Int = DefaultSessionTimeout,
                          retryPolicy: RetryPolicy = DefaultRetryPolicy,
                          authScheme: String = null,
                          authData: Array[Byte] = null): CuratorFramework = {
    val lookupClient = CuratorFrameworkFactory.builder()
      .connectString(connectionString)
      .retryPolicy(new retry.RetryOneTime(RetryInterval))
      .buildTemp(LookupClientTimeout, TimeUnit.MILLISECONDS)
    val serviceConfigPath = "/system/%s/%s".format(environment, service)
    try {
      lookupClient.inTransaction().check().forPath(serviceConfigPath).and().commit()
    } catch {
      case ke: KeeperException => {
        throw new MissingResourceException("Remote configuration for %s service in %s environment is unavailable: %s - %s."
          .format(service, environment, ke.code(), ke.getMessage), "ZNode", serviceConfigPath)
      }
    }

    val client = CuratorFrameworkFactory.builder()
      .connectString(connectionString)
      .connectionTimeoutMs(connectionTimeout)
      .sessionTimeoutMs(sessionTimeout)
      .retryPolicy(retryPolicy)
      .authorization(authScheme, authData)
      .namespace("system/" + environment)
      .build()

    try {
      client.start()
      client
    } catch {
      case t: Throwable =>
        throw new RuntimeException("Unable to start ZooKeeper remote configuration client: %s".
          format(t.getLocalizedMessage), t)
    }
  }

  /**
   * Retrieves raw value from remote configuration.
   *
   * @param path path to configuration entry; for example, <i>section.subsection.entry</i>
   * @param client ZooKeeper remote config client
   * @return configuration setting value
   */
  def getSetting(path: String)(implicit client: CuratorFramework): Array[Byte] = {
    client.getData.forPath("/%s".format(path.trim.replaceAll("\\.", "/")))
  }

  /**
   * Retrieves optional raw value from remote configuration.
   *
   * @param path path to configuration entry; for example, <i>section.subsection.entry</i>
   * @param client remote configuration client
   * @return optional configuration setting value
   */
  def getOptionalSetting(path: String)(implicit client: CuratorFramework): Option[Array[Byte]] = {
    Try {
      getSetting(path)
    }.toOption
  }

  /**
   * Helps to convert data from ZooKeeper into base data types.
   *
   * @param zData raw data from ZooKeeper
   * @param charset charset to use for data conversion
   */
  implicit class ZDataConverter(val zData: Array[Byte])(implicit val charset: Charset = Charsets.UTF_8) {

    /**
     * Converts data from ZooKeeper to string, if possible.
     *
     * @return string value
     */
    def asString: String = new String(zData, charset)

    /**
     * Converts data from ZooKeeper to boolean, if possible.
     *
     * @return boolean value
     * @throws IllegalArgumentException if can not cast value to boolean
     */
    def asBoolean: Boolean = new String(zData, charset).toBoolean

    /**
     * Converts data from ZooKeeper to byte, if possible.
     *
     * @return byte value
     * @throws NumberFormatException if value is not a valid byte
     */
    def asByte: Byte = new String(zData, charset).toByte

    /**
     * Converts data from ZooKeeper to int, if possible.
     *
     * @return int value
     * @throws NumberFormatException if value is not a valid integer
     */
    def asInt: Int = new String(zData, charset).toInt

    /**
     * Converts data from ZooKeeper to long, if possible.
     *
     * @return long value
     * @throws NumberFormatException if value is not a valid long
     */
    def asLong: Long = new String(zData, charset).toLong

    /**
     * Converts data from ZooKeeper to double, if possible.
     *
     * @return double value
     * @throws NumberFormatException if value is not a valid double
     */
    def asDouble: Double = new String(zData, charset).toDouble
  }

  /**
   * Helps to convert optional data from ZooKeeper into base data types and wrapped in Option.
   *
   * @param zDataOption data from ZooKeeper, wrapped in Option
   * @param charset charset to use for data conversion
   */
  implicit class ZDataOptionConverter(val zDataOption: Option[Array[Byte]])(implicit val charset: Charset = Charsets.UTF_8) {

    /**
     * Converts data from ZooKeeper to optional string, if possible.
     *
     * @return optional string value
     */
    def asOptionalString: Option[String] = zDataOption.map(_.asString)

    /**
     * Converts data from ZooKeeper to optional boolean, if possible.
     *
     * @return optional boolean value
     */
    def asOptionalBoolean: Option[Boolean] = zDataOption.flatMap(v => Try {
      v.asBoolean
    }.toOption)

    /**
     * Converts data from ZooKeeper to optional byte, if possible.
     *
     * @return optional byte value
     */
    def asOptionalByte: Option[Byte] = zDataOption.flatMap(v => Try {
      v.asByte
    }.toOption)

    /**
     * Converts data from ZooKeeper to optional integer, if possible.
     *
     * @return optional int value
     */
    def asOptionalInt: Option[Int] = zDataOption.flatMap(v => Try {
      v.asInt
    }.toOption)

    /**
     * Converts data from ZooKeeper to optional long, if possible.
     *
     * @return optional long value
     */
    def asOptionalLong: Option[Long] = zDataOption.flatMap(v => Try {
      v.asLong
    }.toOption)

    /**
     * Converts data from ZooKeeper to optional double, if possible.
     *
     * @return optional double value
     */
    def asOptionalDouble: Option[Double] = zDataOption.flatMap(v => Try {
      v.asDouble
    }.toOption)
  }

}