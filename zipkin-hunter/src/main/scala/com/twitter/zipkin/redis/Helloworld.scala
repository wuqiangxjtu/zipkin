package com.twitter.zipkin.redis

import com.twitter.finagle.redis.Client
import com.twitter.zipkin.storage.redis.RedisIndex
import com.twitter.conversions.time.intToTimeableNumber
import com.twitter.util.Await.{ ready, result }
import com.twitter.zipkin.storage.redis.RedisStorage
import com.twitter.zipkin.storage.redis.RedisSpanStore
import com.twitter.zipkin.hunter.HunterService

/**
 * @author wuqiang
 */
object Helloworld {
    def main(args: Array[String]) {
        val client = Client("172.16.190.141:6379")
        val redisIndex = new RedisIndex {
            val database = client
            val ttl = Some(7.days)
        }
        
        val _client = Client("172.16.190.141:6379")

        var redisStorage = new RedisStorage {
            val database = _client
            val ttl = Some(7.days)
        }
        
        val service = new HunterService(redisIndex, redisStorage)
        service.getServiceNames().map { x => println(x) }

//        var spanStore = new RedisSpanStore(redisIndex, redisStorage)
//
//        result(spanStore.getTracesDuration(
//            result(redisIndex.getTraceIdsByName("/lyreport/login.action", None, 1443002838273000L, 3)).map {
//                x => x.traceId
//            })).map { y => println(y.duration) }

//        result(redisIndex.getTraceIdsByName("/lyreport/login.action", None, 1443002838273000L, 3)).map { x =>
//            println(x.traceId)
//            println(x.timestamp)
//        }
    }
}