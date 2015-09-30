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
class HunterService(address: String) {

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
        redisIndex.getServiceNames()
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
    def getTracesDuration(traceIds: Seq[IndexedTraceId]): Future[Seq[TraceIdDuration]] = {
        spanStore.getTracesDuration(
            traceIds.map {
                x => x.traceId
            })
    }

    /**
     * 求一批trace的平均时长
     */
    def getTracesDurationAverage(traceIds: Seq[IndexedTraceId]): Long = {
        val traceDurations = Await.result(getTracesDuration(traceIds))
        val sum = traceDurations.map {
            x => x.duration
        }.sum
        val size = traceDurations.size
        sum / size
    }

    def getServiceMaxDuration(traceIds: Seq[IndexedTraceId]): Long = {
        Await.result(getTracesDuration(traceIds)).last.duration
    }

    def getServiceMinDuration(traceIds: Seq[IndexedTraceId]): Long = {
        Await.result(getTracesDuration(traceIds)).head.duration
    }

    def getServicesTimeStats1(endTs: Long, limit: Int): Map[String, Object] = {
        //        var data = Map[String, Map[String, Object]]()
        var data = Map[String, Object]()
        val services = getServiceNames
        services.map { serviceName =>
            //println(serviceName)
            val traceIds = Await.result(getTraceIdsByName(serviceName, endTs, limit))
            var max = 0L
            var min = 0L
            var average = 0L
            if (traceIds.size > 0) {
                max = getServiceMaxDuration(traceIds)
                min = getServiceMinDuration(traceIds)
                average = getTracesDurationAverage(traceIds)
            }
            val st = Map(
                ("max" -> max),
                ("min" -> min),
                ("avg" -> average))
            data = Map("name" -> max.toString())

        }
        println(data)
        data
    }

    def getServicesTimeStats(name: String, endTs: Long, limit: Int): Map[String, Object] = {
        val traceIds = Await.result(getTraceIdsByName(name, endTs, limit))
        var max = 0L
        var min = 0L
        var average = 0L
        if (traceIds.size > 0) {
            max = getServiceMaxDuration(traceIds)/1000
            min = getServiceMinDuration(traceIds)/1000
            average = getTracesDurationAverage(traceIds)/1000
        }
        Map(("name"-> name),
            ("max" -> max.toString()),
            ("min" -> min.toString()),
            ("avg" -> average.toString()))
 
    }

}