/*

DFScala

Copyright (c) 2010-2012, The University of Manchester
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of The University of Manchester nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF MANCHESTER BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package eu.teraflux.uniman.dataflow

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArraySeq

object DFState extends Enumeration {
  type DFState = Value
  val Initalising, Waiting, Ready, Running, Finished = Value
}

import DFState._

class Token[-T](f:Function[T,Unit])
{
  def apply(t:T) = f(t)
}

object NullToken extends Token[Any](_ => ())
{
  override def apply(t:Any) = DFLogger.nullTokenPassed(DFManager.currentThread.get)
}

sealed trait OptionArg[+T] {
  def toOption: Option[T]
}

case class SomeArg[T](a: T) extends OptionArg[T] {
  def toOption: Option[T] = Some(a)
}

case object NoneArg extends OptionArg[Nothing] {
  def toOption: Option[Nothing] = None
}

abstract class DFThread(val manager: DFManager, val args: Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], val startThread:Boolean) extends Runnable{
  val logID:Int = DFLogger.getThreadID
  private val syncBits: BitMap = new BitMap(args)
  var state: DFState = Initalising
  private val inBarrierBits: BitMap = new BitMap(inBarriers.size)
  private val tokenList = new ListBuffer[(DFThread, Int)]
  private val threadList = new ListBuffer[DFThread]
  def nullToken:Token[Any] = NullToken
  def startthread = startThread
  def no_args = args

  for (b <- inBarriers)
    b.registerOutThread(this)
  for (b <- outBarriers)
    b.registerInThread(this)

  def addThread(thread: DFThread) {
    DFLogger.threadCreated(thread, this)
    threadList += thread
  }

  def activate() {
    state = Waiting
    checkReady()
  }

  // Passing tokens at end of thread
  def cleanup: Unit = {
    for (token <- tokenList) token._1.receiveToken(token._2)
    for (thread <- threadList) DFManager.registerDFThread(thread)
  }

  def signalFromBarrier(barr: DFBarrier): Unit = {
    var ind = inBarriers.indexWhere((d) => d == barr)
    inBarrierBits.set(ind + 1)
    checkReady
  }

  def runX()

  def checkReady() = {
    synchronized{
      if (syncBits.isSet && inBarrierBits.isSet && state == Waiting) 
        state = Ready
      manager.wake()
    }
  }

  def passToken(dft: DFThread, id: Int) {
     DFLogger.tokenPassed(this, dft, id)
    //tokens don't actually escape thread till cleanup at the end
    synchronized{
      tokenList += ((dft, id))
    }
  }

  def receiveToken(id: Int) = {
      require(!syncBits.isSet(id), "Received same input twice")
      syncBits.set(id)
      checkReady
  }

  def run = {
    try {
      DFManager.currentThread.set(this)
      DFLogger.threadStarted(this)
      runX
      DFLogger.threadFinished(this)
      cleanup
      for (b <- outBarriers)
        b.signal
      state = Finished
      manager.wake()
    } catch {
      case e: Throwable => { manager.sendException(e) }
    }
  }

  def isReady() = { state == Ready }
  def isRunning() = { state == Running }
  def isFinished() = { state == Finished }
  def isWaiting() = { state == Waiting }
  
  override def toString(): String = {
    val string = new StringBuilder()
    string.append("DFThread in state " + (if (state == DFState.Initalising) "Initalised\n"
    else if (state == DFState.Waiting) "Waiting\n"
    else if (state == DFState.Ready) "Ready\n"
    else if (state == DFState.Running) "Running\n"
    else "Finished\n"))
    string.append("Token State " + syncBits + " \n")
    string.append("Identity " + System.identityHashCode(this) + "\n")
    string.append("Name " + name + "\n")
    string.toString
  }
  
  var name : String = hashCode.toString
}

//Traits for passing basic threads
trait DFThread0[R] extends DFThread {  
  private val listenerList = new ListBuffer[Token[R]]
  protected var rval: R = _
  private var notified = false

  def notifyListeners = {
    synchronized {
    for (l <- listenerList) l(rval)
    notified = true
    }
  }
  
  def addListener(pass:Token[R]) = {
    synchronized {
      if (notified)
        pass(rval)
      else
        listenerList += (pass)
    }
  }
}

private[dataflow] abstract class DFThread0X[R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean) 
  extends DFThread(manager, args, inBarriers, outBarriers, startThread)
  with DFThread0[R]

private[dataflow] class DFThread0Runable[R](f: () => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFThread0X[R](manager, 0, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f()
    notifyListeners
  }
}

abstract class DFLThread[E, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean)
  extends DFThread(manager, args, inBarriers, outBarriers, startThread)
  with DFThread0[R] {
  protected def listArg:List[E]
}

trait DFFold[E,R] extends DFLThread[E,R] {
    private var _accumulator: Option[R] = None
  
  def accumulator:R = {
    _accumulator match {
      case None => throw new Exception("Tried to read an unset accumulator")
      case Some(arg) => arg
    }
  }
  def accumulator_= (value:R):Unit = {
    _accumulator match {
      case None => { _accumulator = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 2)
                       receiveToken(2)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,2)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }
  
  def accumulatorToken():Token[R] = new Token(accumulator_= _)
}

trait DFListFoldThread[E,R] extends DFListThread[E,R] with DFFold[E,R]

trait DFFoldLeft[E, R] extends DFFold[E,R]{
  protected val f:(R,E) => R
  def runX() = {
    rval = listArg.foldLeft(accumulator) (f)
    notifyListeners
  }
}

trait DFFoldRight[E, R] extends DFFold[E,R]{
  protected val f:(E,R) => R
  def runX() = {
    rval = listArg.foldRight(accumulator) (f)
    notifyListeners
  }
}

trait DFReducer[E] extends DFLThread[E,E] {
  protected val f:(E,E) => E
  def runX() = {
    val arg = listArg
    rval = listArg.reduce(f)
    notifyListeners
  }
}

trait DFCollector[E] extends DFLThread[E, List[E]]{
  def runX() = {
    rval = listArg 
    notifyListeners
  }
}

abstract class DFListThread[E,R](manager: DFManager, args:Int,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean) 
extends DFLThread[E,R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread1[E,R]
{
  private var _arg1: List[E] = List()
  private var received_inputs = 0
  protected def listArg:List[E] = _arg1
  
  def arg1_= (value:E):Unit = {
    synchronized{
	  _arg1 = value::_arg1
	  if(startThread)
	  {
            DFLogger.tokenPassed(manager, this, 1)
	    receiveToken(1)
	  }
	  else
	  DFManager.currentThread.get().passToken(this,1)  
	}
  }
  
  def arg1:E = throw new Exception("Attempted to read indvidual elements out of a collector thread")

  def apply(a: Option[E]) {
    a match {
      case Some(a) => arg1 = a
      case None => ()
    }
  }

  override def no_args = {
    no_inputs + args - 1;
  }

  override def receiveToken(id: Int) = {
    id match {
      case 1 => synchronized {
      assert(received_inputs != no_inputs, "Too many inputs passed: " + received_inputs + " != " + no_inputs /* + " " + (received_inputs != no_inputs).toString */)
      received_inputs += 1
      if(received_inputs == no_inputs)
        super.receiveToken(id)
    }
      case _ => super.receiveToken(id)
    }
  }
}

class DFCollectorThread[E] (no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFListThread[E,List[E]](manager: DFManager, 1,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean)
  with DFCollector[E]

class DFReducerThread[E] (protected val f:(E,E) => E,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFListThread[E,E](manager: DFManager, 1,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean) 
  with DFReducer[E]

class DFFoldLeftThread[E,R](protected val f:(R,E) => R,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFListThread[E,R](manager,  2, no_inputs, inBarriers, outBarriers, startThread)
  with DFListFoldThread[E,R]
  with DFFoldLeft[E,R]

class DFFoldRightThread[E,R](protected val f:(E,R) => R,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFListThread[E,R](manager,  2, no_inputs, inBarriers, outBarriers, startThread)
  with DFListFoldThread[E,R]
  with DFFoldRight[E,R]

abstract class DFOrderedListThread[E, R](manager: DFManager, args:Int,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean) 
  extends DFLThread[E, R](manager, args, inBarriers, outBarriers, startThread)
  {
  private val _arg1:ArraySeq[E] = new ArraySeq[E](no_inputs)
  private val arrived:ArraySeq[Boolean] = new ArraySeq[Boolean](no_inputs)
  
  private var recieved_inputs = 0
  def listArg:List[E] = _arg1.toList
  
  def update(pos:Int, value:E):Unit = {
	synchronized { 
	  assert(!arrived(pos), "Value has alread been set")
	  arrived(pos) = true
	  _arg1(pos) = value
	  if(startThread)
	  {
	    DFLogger.tokenPassed(manager, this, 1)
	    receiveToken(1)
	  }
	  else
	    DFManager.currentThread.get().passToken(this,1)
	}
  }
  
  def token(pos:Int):Token[E] = new Token(update(pos, _))
  
  override def receiveToken(id: Int) = {
    id match {
      case 1 => synchronized { 
      recieved_inputs += 1
      if(recieved_inputs == no_inputs)
        super.receiveToken(id)
    }
      case _ => super.receiveToken(id)
    }
  }
}

abstract class DFOrderedListFoldThread[E, R] (manager: DFManager, args:Int,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean)
  extends DFOrderedListThread[E, R] (manager, args,  no_inputs, inBarriers, outBarriers, startThread)
  with DFFold[E,R]

class DFOrderedFoldLeftThread[E,R](protected val f:(R,E) => R,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFOrderedListFoldThread[E,R](manager,  2, no_inputs, inBarriers, outBarriers, startThread) 
  with DFFoldLeft[E,R]

class DFOrderedFoldRightThread[E,R](protected val f:(E,R) => R,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFOrderedListFoldThread[E,R](manager,  2, no_inputs, inBarriers, outBarriers, startThread)
  with DFFoldRight[E,R]

class DFOrderedCollectorThread[E] (no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFOrderedListThread[E, List[E]](manager: DFManager, 1,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean)
  with DFCollector[E]

class DFOrderedReducerThread[E] (protected val f:(E,E) => E,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager) 
  extends DFOrderedListThread[E, E](manager: DFManager, 1,  no_inputs:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean) 
  with DFReducer[E]

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread1[T1, R] extends DFThread0[R]{
  def arg1:T1
  def arg1_= (value:T1):Unit
  def token1:Token[T1] = new Token(arg1_= _)
  def apply(a1: Option[T1]): Unit
  def apply(a1: OptionArg[T1]): Unit = apply(a1.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread1X[T1, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread0X[R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread1[T1, R]{

  private var _arg1: Option[T1] = None

  def arg1:T1 = {
    _arg1 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg1_= (value:T1):Unit = {
    _arg1 match {
      case None => { _arg1 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 1)
                       receiveToken(1)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,1)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1]) {
    a1 match {
      case Some(a) => arg1 = a
      case None => ()
    }
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread1Runable[T1, R](f: (T1) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread1X[T1, R](manager, 1, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread2[T1, T2, R] extends DFThread1[T1, R]{
  def arg2:T2
  def arg2_= (value:T2):Unit
  def token2:Token[T2] = new Token(arg2_= _)
  def apply(a1: Option[T1], a2: Option[T2]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2]): Unit = apply(a1.toOption, a2.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread2X[T1, T2, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread1X[T1, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread2[T1, T2, R]{

  private var _arg2: Option[T2] = None

  def arg2:T2 = {
    _arg2 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg2_= (value:T2):Unit = {
    _arg2 match {
      case None => { _arg2 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 2)
                       receiveToken(2)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,2)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2]) {
    a2 match {
      case Some(a) => arg2 = a
      case None => ()
    }
    apply(a1)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread2Runable[T1, T2, R](f: (T1, T2) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread2X[T1, T2, R](manager, 2, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread3[T1, T2, T3, R] extends DFThread2[T1, T2, R]{
  def arg3:T3
  def arg3_= (value:T3):Unit
  def token3:Token[T3] = new Token(arg3_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3]): Unit = apply(a1.toOption, a2.toOption, a3.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread3X[T1, T2, T3, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread2X[T1, T2, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread3[T1, T2, T3, R]{

  private var _arg3: Option[T3] = None

  def arg3:T3 = {
    _arg3 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg3_= (value:T3):Unit = {
    _arg3 match {
      case None => { _arg3 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 3)
                       receiveToken(3)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,3)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3]) {
    a3 match {
      case Some(a) => arg3 = a
      case None => ()
    }
    apply(a1, a2)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread3Runable[T1, T2, T3, R](f: (T1, T2, T3) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread3X[T1, T2, T3, R](manager, 3, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread4[T1, T2, T3, T4, R] extends DFThread3[T1, T2, T3, R]{
  def arg4:T4
  def arg4_= (value:T4):Unit
  def token4:Token[T4] = new Token(arg4_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread4X[T1, T2, T3, T4, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread3X[T1, T2, T3, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread4[T1, T2, T3, T4, R]{

  private var _arg4: Option[T4] = None

  def arg4:T4 = {
    _arg4 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg4_= (value:T4):Unit = {
    _arg4 match {
      case None => { _arg4 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 4)
                       receiveToken(4)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,4)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4]) {
    a4 match {
      case Some(a) => arg4 = a
      case None => ()
    }
    apply(a1, a2, a3)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread4Runable[T1, T2, T3, T4, R](f: (T1, T2, T3, T4) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread4X[T1, T2, T3, T4, R](manager, 4, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread5[T1, T2, T3, T4, T5, R] extends DFThread4[T1, T2, T3, T4, R]{
  def arg5:T5
  def arg5_= (value:T5):Unit
  def token5:Token[T5] = new Token(arg5_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread5X[T1, T2, T3, T4, T5, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread4X[T1, T2, T3, T4, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread5[T1, T2, T3, T4, T5, R]{

  private var _arg5: Option[T5] = None

  def arg5:T5 = {
    _arg5 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg5_= (value:T5):Unit = {
    _arg5 match {
      case None => { _arg5 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 5)
                       receiveToken(5)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,5)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5]) {
    a5 match {
      case Some(a) => arg5 = a
      case None => ()
    }
    apply(a1, a2, a3, a4)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread5Runable[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread5X[T1, T2, T3, T4, T5, R](manager, 5, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread6[T1, T2, T3, T4, T5, T6, R] extends DFThread5[T1, T2, T3, T4, T5, R]{
  def arg6:T6
  def arg6_= (value:T6):Unit
  def token6:Token[T6] = new Token(arg6_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread6X[T1, T2, T3, T4, T5, T6, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread5X[T1, T2, T3, T4, T5, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread6[T1, T2, T3, T4, T5, T6, R]{

  private var _arg6: Option[T6] = None

  def arg6:T6 = {
    _arg6 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg6_= (value:T6):Unit = {
    _arg6 match {
      case None => { _arg6 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 6)
                       receiveToken(6)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,6)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6]) {
    a6 match {
      case Some(a) => arg6 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread6Runable[T1, T2, T3, T4, T5, T6, R](f: (T1, T2, T3, T4, T5, T6) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread6X[T1, T2, T3, T4, T5, T6, R](manager, 6, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread7[T1, T2, T3, T4, T5, T6, T7, R] extends DFThread6[T1, T2, T3, T4, T5, T6, R]{
  def arg7:T7
  def arg7_= (value:T7):Unit
  def token7:Token[T7] = new Token(arg7_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread7X[T1, T2, T3, T4, T5, T6, T7, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread6X[T1, T2, T3, T4, T5, T6, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread7[T1, T2, T3, T4, T5, T6, T7, R]{

  private var _arg7: Option[T7] = None

  def arg7:T7 = {
    _arg7 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg7_= (value:T7):Unit = {
    _arg7 match {
      case None => { _arg7 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 7)
                       receiveToken(7)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,7)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7]) {
    a7 match {
      case Some(a) => arg7 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread7Runable[T1, T2, T3, T4, T5, T6, T7, R](f: (T1, T2, T3, T4, T5, T6, T7) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread7X[T1, T2, T3, T4, T5, T6, T7, R](manager, 7, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R] extends DFThread7[T1, T2, T3, T4, T5, T6, T7, R]{
  def arg8:T8
  def arg8_= (value:T8):Unit
  def token8:Token[T8] = new Token(arg8_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread8X[T1, T2, T3, T4, T5, T6, T7, T8, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread7X[T1, T2, T3, T4, T5, T6, T7, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R]{

  private var _arg8: Option[T8] = None

  def arg8:T8 = {
    _arg8 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg8_= (value:T8):Unit = {
    _arg8 match {
      case None => { _arg8 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 8)
                       receiveToken(8)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,8)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8]) {
    a8 match {
      case Some(a) => arg8 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread8Runable[T1, T2, T3, T4, T5, T6, T7, T8, R](f: (T1, T2, T3, T4, T5, T6, T7, T8) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread8X[T1, T2, T3, T4, T5, T6, T7, T8, R](manager, 8, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] extends DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R]{
  def arg9:T9
  def arg9_= (value:T9):Unit
  def token9:Token[T9] = new Token(arg9_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread9X[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread8X[T1, T2, T3, T4, T5, T6, T7, T8, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]{

  private var _arg9: Option[T9] = None

  def arg9:T9 = {
    _arg9 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg9_= (value:T9):Unit = {
    _arg9 match {
      case None => { _arg9 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 9)
                       receiveToken(9)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,9)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9]) {
    a9 match {
      case Some(a) => arg9 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread9Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread9X[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](manager, 9, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] extends DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]{
  def arg10:T10
  def arg10_= (value:T10):Unit
  def token10:Token[T10] = new Token(arg10_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread10X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread9X[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]{

  private var _arg10: Option[T10] = None

  def arg10:T10 = {
    _arg10 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg10_= (value:T10):Unit = {
    _arg10 match {
      case None => { _arg10 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 10)
                       receiveToken(10)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,10)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10]) {
    a10 match {
      case Some(a) => arg10 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread10Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread10X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](manager, 10, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] extends DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]{
  def arg11:T11
  def arg11_= (value:T11):Unit
  def token11:Token[T11] = new Token(arg11_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread11X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread10X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]{

  private var _arg11: Option[T11] = None

  def arg11:T11 = {
    _arg11 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg11_= (value:T11):Unit = {
    _arg11 match {
      case None => { _arg11 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 11)
                       receiveToken(11)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,11)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11]) {
    a11 match {
      case Some(a) => arg11 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread11Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread11X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](manager, 11, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] extends DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]{
  def arg12:T12
  def arg12_= (value:T12):Unit
  def token12:Token[T12] = new Token(arg12_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread12X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread11X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]{

  private var _arg12: Option[T12] = None

  def arg12:T12 = {
    _arg12 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg12_= (value:T12):Unit = {
    _arg12 match {
      case None => { _arg12 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 12)
                       receiveToken(12)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,12)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12]) {
    a12 match {
      case Some(a) => arg12 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread12Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread12X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](manager, 12, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] extends DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]{
  def arg13:T13
  def arg13_= (value:T13):Unit
  def token13:Token[T13] = new Token(arg13_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread13X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread12X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]{

  private var _arg13: Option[T13] = None

  def arg13:T13 = {
    _arg13 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg13_= (value:T13):Unit = {
    _arg13 match {
      case None => { _arg13 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 13)
                       receiveToken(13)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,13)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13]) {
    a13 match {
      case Some(a) => arg13 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread13Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread13X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](manager, 13, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] extends DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]{
  def arg14:T14
  def arg14_= (value:T14):Unit
  def token14:Token[T14] = new Token(arg14_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread14X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread13X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]{

  private var _arg14: Option[T14] = None

  def arg14:T14 = {
    _arg14 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg14_= (value:T14):Unit = {
    _arg14 match {
      case None => { _arg14 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 14)
                       receiveToken(14)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,14)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14]) {
    a14 match {
      case Some(a) => arg14 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread14Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread14X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](manager, 14, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] extends DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]{
  def arg15:T15
  def arg15_= (value:T15):Unit
  def token15:Token[T15] = new Token(arg15_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread15X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread14X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]{

  private var _arg15: Option[T15] = None

  def arg15:T15 = {
    _arg15 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg15_= (value:T15):Unit = {
    _arg15 match {
      case None => { _arg15 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 15)
                       receiveToken(15)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,15)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15]) {
    a15 match {
      case Some(a) => arg15 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread15Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread15X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](manager, 15, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] extends DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]{
  def arg16:T16
  def arg16_= (value:T16):Unit
  def token16:Token[T16] = new Token(arg16_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread16X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread15X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]{

  private var _arg16: Option[T16] = None

  def arg16:T16 = {
    _arg16 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg16_= (value:T16):Unit = {
    _arg16 match {
      case None => { _arg16 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 16)
                       receiveToken(16)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,16)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16]) {
    a16 match {
      case Some(a) => arg16 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread16Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread16X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](manager, 16, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] extends DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]{
  def arg17:T17
  def arg17_= (value:T17):Unit
  def token17:Token[T17] = new Token(arg17_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread17X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread16X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]{

  private var _arg17: Option[T17] = None

  def arg17:T17 = {
    _arg17 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg17_= (value:T17):Unit = {
    _arg17 match {
      case None => { _arg17 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 17)
                       receiveToken(17)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,17)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17]) {
    a17 match {
      case Some(a) => arg17 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread17Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread17X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](manager, 17, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] extends DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]{
  def arg18:T18
  def arg18_= (value:T18):Unit
  def token18:Token[T18] = new Token(arg18_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread18X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread17X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]{

  private var _arg18: Option[T18] = None

  def arg18:T18 = {
    _arg18 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg18_= (value:T18):Unit = {
    _arg18 match {
      case None => { _arg18 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 18)
                       receiveToken(18)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,18)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18]) {
    a18 match {
      case Some(a) => arg18 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread18Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread18X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](manager, 18, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] extends DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]{
  def arg19:T19
  def arg19_= (value:T19):Unit
  def token19:Token[T19] = new Token(arg19_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18], a19: OptionArg[T19]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption, a19.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread19X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread18X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]{

  private var _arg19: Option[T19] = None

  def arg19:T19 = {
    _arg19 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg19_= (value:T19):Unit = {
    _arg19 match {
      case None => { _arg19 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 19)
                       receiveToken(19)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,19)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19]) {
    a19 match {
      case Some(a) => arg19 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread19Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread19X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](manager, 19, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19)
    notifyListeners
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
trait DFThread20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] extends DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]{
  def arg20:T20
  def arg20_= (value:T20):Unit
  def token20:Token[T20] = new Token(arg20_= _)
  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19], a20: Option[T20]): Unit
  def apply(a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18], a19: OptionArg[T19], a20: OptionArg[T20]): Unit = apply(a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption, a19.toOption, a20.toOption)
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] abstract class DFThread20X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)
  extends DFThread19X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](manager, args, inBarriers, outBarriers, startThread)
  with DFThread20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]{

  private var _arg20: Option[T20] = None

  def arg20:T20 = {
    _arg20 match {
      case None => throw new Exception("Tried to read an unset argument")
      case Some(arg) => arg
    }
  }

  def arg20_= (value:T20):Unit = {
    _arg20 match {
      case None => { _arg20 = Some(value)
                     if(startThread)
                     {
                       DFLogger.tokenPassed(manager, this, 20)
                       receiveToken(20)
                     }
                     else
                       DFManager.currentThread.get().passToken(this,20)
                   }
      case Some(arg) => throw new Exception("Tried to write an already written argument")
    }
  }

  def apply(a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19], a20: Option[T20]) {
    a20 match {
      case Some(a) => arg20 = a
      case None => ()
    }
    apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)
  }
}

// Automatically generated - edit tools/thread-code.py, rather than this code
private[dataflow] class DFThread20Runable[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)
  extends DFThread20X[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](manager, 20, inBarriers, outBarriers, startThread) {
  def runX() = {
    rval = f(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20)
    notifyListeners
  }
}
