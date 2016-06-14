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

package swave.core

import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal
import swave.core.impl.{ Port, TypeLogic, RunContext }

/**
 * A [[Piping]] represents a system or network of connected pipes in which all inlet and outlets
 * have been properly connected and which is therefore ready to be started.
 */
final class Piping[A] private[core] (port: Port, val result: A) {

  def pipeElem: PipeElem.Basic = port.pipeElem

  def mapResult[B](f: A ⇒ B): Piping[B] = new Piping(port, f(result))

  def seal()(implicit env: StreamEnv): Try[SealedPiping[A]] =
    Try {
      val ctx = new RunContext(port)
      ctx.seal()
      new SealedPiping(ctx, result)
    }

  def run()(implicit env: StreamEnv, ev: TypeLogic.TryFlatten[A]): ev.Out =
    seal() match {
      case Success(x) ⇒ x.run()
      case Failure(e) ⇒ ev.failure(e)
    }
}

final class SealedPiping[A] private[core] (ctx: RunContext, val result: A) {

  def pipeElem: PipeElem.Basic = ctx.port.pipeElem

  def mapResult[B](f: A ⇒ B): SealedPiping[B] = new SealedPiping(ctx, f(result))

  def run()(implicit ev: TypeLogic.TryFlatten[A]): ev.Out =
    try {
      ctx.start()
      ev.success(result)
    } catch {
      case NonFatal(e) ⇒ ev.failure(e)
    }
}