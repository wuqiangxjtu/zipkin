package com.twitter.zipkin.storage.cassandra

import com.twitter.util.TimeConversions._
import com.twitter.util._
import com.twitter.zipkin.common.Dependencies
import com.twitter.zipkin.conversions.thrift._
import com.twitter.zipkin.storage.DependencyStore
import com.twitter.zipkin.thriftscala.{Dependencies => ThriftDependencies}
import org.twitter.zipkin.storage.cassandra.Repository

import scala.collection.JavaConverters._

/**
 * This implementation of DependencyStore assumes that the job aggregating dependencies
 * only writes once a day. As such calls to [[.storeDependencies]] which vary contained
 * by a day will overwrite eachother.
 */
class CassandraDependencyStore(repository: Repository) extends DependencyStore {

  private[this] val pool = FuturePool.unboundedPool
  private[this] val codec = new ScroogeThriftCodec[ThriftDependencies](ThriftDependencies)

  def close() = repository.close()

  override def getDependencies(startDate: Option[Time],
                               endDate: Option[Time]): Future[Dependencies] = pool {
    val startTime = startDate.getOrElse(Time.now - 1.day)
    val endTime = endDate.getOrElse(Time.now)
    val dependencyLinks = repository.getDependencies(startTime.inMillis, endTime.inMillis).asScala
      .map(codec.decode(_))
      .flatMap(thriftToDependencies(_).toDependencies.links)
    Dependencies(startTime, endTime, dependencyLinks)
  }

  override def storeDependencies(dependencies: Dependencies): Future[Unit] = pool {
    val thrift = codec.encode(dependenciesToThrift(dependencies).toThrift)
    repository.storeDependencies(dependencies.startTime.inMillis, thrift)
  }
}
