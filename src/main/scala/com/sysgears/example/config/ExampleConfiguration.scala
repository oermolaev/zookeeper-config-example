package com.sysgears.example.config

import org.apache.curator.framework.CuratorFramework

/**
 * Example Service configuration.
 */
trait ExampleConfiguration extends ZooKeeperConfiguration {

  /**
   * Service name.
   */
  val Service: String = "example"

  /**
   * ZooKeeper Remote configuration client.
   */
  implicit val zkClient: CuratorFramework = initZooKeeperClient(service = Service, environment = Environment)
}
