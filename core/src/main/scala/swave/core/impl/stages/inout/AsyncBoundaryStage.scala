/*
 * Copyright © 2016 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package swave.core.impl.stages.inout

import swave.core.PipeElem
import swave.core.impl.stages.Stage
import swave.core.impl.{ Inport, Outport }
import swave.core.macros.StageImpl

// format: OFF
@StageImpl
private[core] final class AsyncBoundaryStage(dispatcherId: String) extends InOutStage with PipeElem.InOut.AsyncBoundary {

  def pipeElemType: String = "asyncBoundary"
  def pipeElemParams: List[Any] = dispatcherId :: Nil

  connectInOutAndSealWith { (ctx, in, out) ⇒
    val ins = in.asInstanceOf[Stage]
    val outs = out.asInstanceOf[Stage]
    ctx.registerForRunnerAssignment(ins, dispatcherId)
    ctx.registerForRunnerAssignment(outs, "") // fallback assignment of default StreamRunner
    running(ins, outs)
  }

  def running(inStage: Stage, outStage: Stage) = state(
    intercept = false,

    request = (n, from) ⇒ {
      inStage.runner.enqueueRequest(inStage, n.toLong, from)
      stay()
    },

    cancel = from => {
      inStage.runner.enqueueCancel(inStage, from)
      stop()
    },

    onNext = (elem, from) ⇒ {
      outStage.runner.enqueueOnNext(outStage, elem, from)
      stay()
    },

    onComplete = from => {
      outStage.runner.enqueueOnComplete(outStage, from)
      stop()
    },

    onError = (error, from) => {
      outStage.runner.enqueueOnError(outStage, error, from)
      stop()
    })
}

