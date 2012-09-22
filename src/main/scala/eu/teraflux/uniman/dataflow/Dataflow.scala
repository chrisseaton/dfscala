/*

Copyright (c) 2012, The University of Manchester
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

import eu.teraflux.uniman.dataflow._

object Dataflow {
  val Blank = NoneArg
  implicit def WrapSomeArg[T](a : T): OptionArg[T] = SomeArg(a)

  def thread[R](f: () => R): DFThread0[R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, R](f: (T1) => R): DFThread1[T1, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, R](f: (T1) => R, a1: Option[T1]): DFThread1[T1, R] = {
    val t = thread(f)
    t(a1)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, R](f: (T1) => R, a1: OptionArg[T1]): DFThread1[T1, R] = thread(f, a1.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, R](f: (T1, T2) => R): DFThread2[T1, T2, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, R](f: (T1, T2) => R, a1: Option[T1], a2: Option[T2]): DFThread2[T1, T2, R] = {
    val t = thread(f)
    t(a1, a2)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, R](f: (T1, T2) => R, a1: OptionArg[T1], a2: OptionArg[T2]): DFThread2[T1, T2, R] = thread(f, a1.toOption, a2.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, R](f: (T1, T2, T3) => R): DFThread3[T1, T2, T3, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, R](f: (T1, T2, T3) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3]): DFThread3[T1, T2, T3, R] = {
    val t = thread(f)
    t(a1, a2, a3)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, R](f: (T1, T2, T3) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3]): DFThread3[T1, T2, T3, R] = thread(f, a1.toOption, a2.toOption, a3.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, R](f: (T1, T2, T3, T4) => R): DFThread4[T1, T2, T3, T4, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, R](f: (T1, T2, T3, T4) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4]): DFThread4[T1, T2, T3, T4, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, R](f: (T1, T2, T3, T4) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4]): DFThread4[T1, T2, T3, T4, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => R): DFThread5[T1, T2, T3, T4, T5, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5]): DFThread5[T1, T2, T3, T4, T5, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5]): DFThread5[T1, T2, T3, T4, T5, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, R](f: (T1, T2, T3, T4, T5, T6) => R): DFThread6[T1, T2, T3, T4, T5, T6, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, R](f: (T1, T2, T3, T4, T5, T6) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6]): DFThread6[T1, T2, T3, T4, T5, T6, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, R](f: (T1, T2, T3, T4, T5, T6) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6]): DFThread6[T1, T2, T3, T4, T5, T6, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, R](f: (T1, T2, T3, T4, T5, T6, T7) => R): DFThread7[T1, T2, T3, T4, T5, T6, T7, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, R](f: (T1, T2, T3, T4, T5, T6, T7) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7]): DFThread7[T1, T2, T3, T4, T5, T6, T7, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, R](f: (T1, T2, T3, T4, T5, T6, T7) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7]): DFThread7[T1, T2, T3, T4, T5, T6, T7, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, R](f: (T1, T2, T3, T4, T5, T6, T7, T8) => R): DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, R](f: (T1, T2, T3, T4, T5, T6, T7, T8) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8]): DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, R](f: (T1, T2, T3, T4, T5, T6, T7, T8) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8]): DFThread8[T1, T2, T3, T4, T5, T6, T7, T8, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => R): DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9]): DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9]): DFThread9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => R): DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10]): DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10]): DFThread10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => R): DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11]): DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11]): DFThread11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => R): DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12]): DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12]): DFThread12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => R): DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13]): DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13]): DFThread13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => R): DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14]): DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14]): DFThread14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => R): DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15]): DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15]): DFThread15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => R): DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16]): DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16]): DFThread16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => R): DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17]): DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17]): DFThread17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => R): DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18]): DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18]): DFThread18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => R): DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19]): DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18], a19: OptionArg[T19]): DFThread19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption, a19.toOption)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => R): DFThread20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] = DFManager.createThread(f)

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => R, a1: Option[T1], a2: Option[T2], a3: Option[T3], a4: Option[T4], a5: Option[T5], a6: Option[T6], a7: Option[T7], a8: Option[T8], a9: Option[T9], a10: Option[T10], a11: Option[T11], a12: Option[T12], a13: Option[T13], a14: Option[T14], a15: Option[T15], a16: Option[T16], a17: Option[T17], a18: Option[T18], a19: Option[T19], a20: Option[T20]): DFThread20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] = {
    val t = thread(f)
    t(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)
    t
  }

  // Automatically generated - edit tools/thread-code.py, rather than this code
  def thread[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](f: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => R, a1: OptionArg[T1], a2: OptionArg[T2], a3: OptionArg[T3], a4: OptionArg[T4], a5: OptionArg[T5], a6: OptionArg[T6], a7: OptionArg[T7], a8: OptionArg[T8], a9: OptionArg[T9], a10: OptionArg[T10], a11: OptionArg[T11], a12: OptionArg[T12], a13: OptionArg[T13], a14: OptionArg[T14], a15: OptionArg[T15], a16: OptionArg[T16], a17: OptionArg[T17], a18: OptionArg[T18], a19: OptionArg[T19], a20: OptionArg[T20]): DFThread20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] = thread(f, a1.toOption, a2.toOption, a3.toOption, a4.toOption, a5.toOption, a6.toOption, a7.toOption, a8.toOption, a9.toOption, a10.toOption, a11.toOption, a12.toOption, a13.toOption, a14.toOption, a15.toOption, a16.toOption, a17.toOption, a18.toOption, a19.toOption, a20.toOption)
}
