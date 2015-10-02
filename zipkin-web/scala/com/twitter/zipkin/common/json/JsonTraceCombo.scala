/*
 * Copyright 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.zipkin.common.json

import com.twitter.zipkin.query.TraceCombo

case class JsonTraceCombo(trace: JsonTrace, traceSummary: Option[JsonTraceSummary], traceTimeline: Option[JsonTraceTimeline],
                          spanDepths: Option[Map[Long, Int]])
  extends WrappedJson

object JsonTraceCombo {
  def wrap(t: TraceCombo) = {
    JsonTraceCombo(JsonTrace.wrap(t.trace), t.traceSummary map { JsonTraceSummary.wrap(_) }, t.traceTimeline map { JsonTraceTimeline.wrap(_) }, t.spanDepths)
  }
}
