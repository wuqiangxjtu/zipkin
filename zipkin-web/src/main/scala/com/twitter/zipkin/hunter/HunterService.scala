package com.twitter.zipkin.hunter

import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.zipkin.storage.IndexedTraceId
import com.twitter.zipkin.storage.TraceIdDuration
import com.twitter.zipkin.storage.redis.RedisIndex
import com.twitter.zipkin.storage.redis.RedisSpanStore
import com.twitter.zipkin.storage.redis.RedisStorage
import com.twitter.zipkin.thriftscala.ZipkinQuery.GetServiceNames
import scala.collection.mutable
import com.twitter.conversions.time.intToTimeableNumber
import com.twitter.finagle.redis.Client
import com.twitter.util.Duration

/**
 * @author wuqiang
 */
class HunterService(val address: String) {

    val _client = Client(address)

    val redisIndex = new RedisIndex {
        val database = _client
        val ttl = Some(7.days)
    }

    var redisStorage = new RedisStorage {
        val database = _client
        val ttl = Some(7.days)
    }
    val spanStore = new RedisSpanStore(redisIndex, redisStorage)

    /**
     * 获取所有的service name
     */
    def getServiceNames(): Set[String] = {
        Await.result(redisIndex.getServiceNames)
    }

    /**
     * 通过service name查找到该service下所有的traceId
     */
    def getTraceIdsByName(serviceName: String, endTs: Long, limit: Int): Future[Seq[IndexedTraceId]] = {
        redisIndex.getTraceIdsByName(serviceName, None, endTs, limit)
    }

    /**
     * 找出traceIds对应的时长，traceIds通常属于一个service，这样就可以找到一个service下所有trace的时长
     */
    def getTracesDuration(traceIds: Future[Seq[IndexedTraceId]]): Future[Seq[TraceIdDuration]] = {
        spanStore.getTracesDuration(
            Await.result(traceIds).map {
                x => x.traceId
            })
    }
    
    /**
     * 找出一批trace对应的最大的Span的数量
     */
    /*
    def getTracesMaxSpan(traceIds: Future[Seq[IndexedTraceId]]): Future[Long] = {
       
    }
    *
    */

    /**
     * 求一批trace的平均时长
     */
    def getTracesDurationAverage(traceIds: Future[Seq[IndexedTraceId]]): Long = {
        val traceDurations = getTracesDuration(traceIds)

        val drua = Await.result(traceDurations)
        val sum = drua.map {
            x => x.duration
        }.sum
        val size = drua.size
        size match {
            case 0 => 0
            case _ => sum / size
        }
    }

    case class MaxAndMinDuration(max: Long, min: Long)

    def getServiceMaxAndMinDuration(traceIds: Future[Seq[IndexedTraceId]]): MaxAndMinDuration = {
        val sortedDuration = Await.result(getTracesDuration(traceIds)).sortWith { (id1, id2) => id1.duration > id2.duration }
        sortedDuration.size match {
            case 0 => MaxAndMinDuration(0, 0)
            case _ => MaxAndMinDuration(sortedDuration.head.duration, sortedDuration.last.duration)
        }
        
    }

    case class StatVo(name:String, max:Long, min:Long, avg:Long)
    def getServicesTimeStats(name: String, endTs: Long, limit: Int): Future[StatVo] = {
        Future {
            val traceIds = getTraceIdsByName(name, endTs, limit)

            val maxmin = getServiceMaxAndMinDuration(traceIds)
            val max = maxmin.max / 1000
            val min = maxmin.min / 1000
            val average = getTracesDurationAverage(traceIds) / 1000

            StatVo(name,max,min,average)    
        }
    }

}