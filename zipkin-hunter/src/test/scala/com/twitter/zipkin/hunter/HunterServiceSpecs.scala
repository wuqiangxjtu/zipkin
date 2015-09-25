package com.twitter.zipkin.hunter

import com.twitter.zipkin.storage.redis.HunterSpecification
import com.twitter.zipkin.storage.redis.RedisIndex
import com.twitter.conversions.time.intToTimeableNumber
import com.twitter.zipkin.storage.redis.RedisStorage
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.twitter.util.Await
import scala.collection.generic.Sorted

/**
 * @author wuqiang
 */
@RunWith(classOf[JUnitRunner])
class HunterServiceSpecs extends HunterSpecification {

    val redisIndex = new RedisIndex {
        val database = _client
        val ttl = Some(7.days)
    }

    var redisStorage = new RedisStorage {
        val database = _client
        val ttl = Some(7.days)
    }

    val service = new HunterService(redisIndex, redisStorage)

    test("getServiceNames") {
        service.getServiceNames().map {
            x => println(x)
        }
    }

    test("getTraceIdsByName") {
        val endTs = 1443063998890000L
        val indexTraceIds = Await.result(service.getTraceIdsByName("/lyreport/effect/report!contract.action", endTs, 10))
        println(indexTraceIds.size)
        indexTraceIds.map { x => println(x.traceId + "," + x.timestamp) }
    }

    test("getTracesDuration") {
        val endTs = 1443063998890000L
        val indexTraceIds = Await.result(service.getTraceIdsByName("/lyreport/effect/report!contract.action", endTs, 10))
        //        indexTraceIds.filter { x => x.traceId == -7963571317287753521L }.map { y => println(y.traceId) }
        Await.result(service.getTracesDuration(indexTraceIds)).map { x => println(x.duration) }
    }

    test("getTracesDurationMax") {
        val endTs = 1443063998890000L
        val indexTraceIds = Await.result(service.getTraceIdsByName("/lyreport/effect/report!contract.action", endTs, 10))
        val max = Await.result(service.getTracesDuration(indexTraceIds)).sortWith((x,y)=>x.duration < y.duration).last
        println(max.duration)
    }
    
    test("getTracesDurationAverage") {
         val endTs = 1443063998890000L
         val indexTraceIds = Await.result(service.getTraceIdsByName("/lyreport/effect/report!contract.action", endTs, 10))
         println(service.getTracesDurationAverage(indexTraceIds))
         
    }
}