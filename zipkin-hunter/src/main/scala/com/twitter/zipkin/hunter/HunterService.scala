package com.twitter.zipkin.hunter

import com.twitter.finagle.redis.Client
import com.twitter.zipkin.storage.redis.RedisIndex
import com.twitter.zipkin.storage.redis.RedisStorage
import com.twitter.util.Future
import com.twitter.zipkin.storage.IndexedTraceId
import com.twitter.util.Await.{ ready, result }
import com.twitter.zipkin.storage.IndexedTraceId
import com.twitter.zipkin.storage.redis.RedisSpanStore
import com.twitter.zipkin.storage.TraceIdDuration
import com.twitter.util.Await


/**
 * @author wuqiang
 */
class HunterService(val redisIndex: RedisIndex, val redisStorage: RedisStorage) {

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
    def getTracesDurationAverage(traceIds: Seq[IndexedTraceId]):Long = {
        val traceDurations = Await.result(getTracesDuration(traceIds))
        val sum = traceDurations.map { 
            x => x.duration 
        }.sum
        val size = traceDurations.size
        sum/size
    }
    
    

}