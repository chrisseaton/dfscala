#!/usr/bin/env python

# DFScala
# 
# Copyright (c) 2012, The University of Manchester
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of The University of Manchester nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF MANCHESTER BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

n = 20

def typeChain(t, includeR=True):
    return ", ".join(map(lambda i: "T" + str(i), range(1, t + 1)) + (["R"] if includeR else []))

print "// DFThread"
print

for t in range(1, n + 1):
    print "// Automatically generated - edit tools/thread-code.py, rather than this code"
    print "trait DFThread" + str(t) + "[" + typeChain(t) + "] extends DFThread" + str(t - 1) + "[" + typeChain(t - 1) + "]{"
    print "  def arg" + str(t) + ":T" + str(t) + ""
    print "  def arg" + str(t) + "_= (value:T" + str(t) + "):Unit"
    print "  def token" + str(t) + ":Token[T" + str(t) + "] = new Token(arg" + str(t) + "_= _)"
    print "  def apply(" + ", ".join(map(lambda i: "a" + str(i) + ": Option[T" + str(i) + "]", range(1, t + 1))) + "): Unit"
    print "  def apply(" + ", ".join(map(lambda i: "a" + str(i) + ": OptionArg[T" + str(i) + "]", range(1, t + 1))) + "): Unit = apply(" + ", ".join(map(lambda i: "a" + str(i) + ".toOption", range(1, t + 1))) + ")"
    print "}"
    print
    print "// Automatically generated - edit tools/thread-code.py, rather than this code"
    print "private[dataflow] abstract class DFThread" + str(t) + "X[" + typeChain(t) + "](manager: DFManager, args:Int, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier],  startThread:Boolean)"
    print "  extends DFThread" + str(t - 1) + "X[" + typeChain(t - 1) + "](manager, args, inBarriers, outBarriers, startThread)"
    print "  with DFThread" + str(t) + "[" + typeChain(t) + "]{"
    print  
    print "  private var _arg" + str(t) + ": Option[T" + str(t) + "] = None"
    print
    print "  def arg" + str(t) + ":T" + str(t) + " = {"
    print "    _arg" + str(t) + " match {"
    print "      case None => throw new Exception(\"Tried to read an unset argument\")"
    print "      case Some(arg) => arg"
    print "    }"
    print "  }"
    print
    print "  def arg" + str(t) + "_= (value:T" + str(t) + "):Unit = {"
    print "    _arg" + str(t) + " match {"
    print "      case None => { _arg" + str(t) + " = Some(value)"
    print "                     if(startThread)"
    print "                     {"
    print "                       DFLogger.tokenPassed(manager, this, " + str(t) + ")"
    print "                       receiveToken(" + str(t) + ")"
    print "                     }"
    print "                     else"
    print "                       DFManager.currentThread.get().passToken(this," + str(t) + ")"
    print "                   }"
    print "      case Some(arg) => throw new Exception(\"Tried to write an already written argument\")"
    print "    }"
    print "  }"
    print
    print "  def apply(" + ", ".join(map(lambda i: "a" + str(i) + ": Option[T" + str(i) + "]", range(1, t + 1))) + ") {"
    print "    a" + str(t) + " match {"
    print "      case Some(a) => arg" + str(t) + " = a"
    print "      case None => ()"
    print "    }"
    if t > 1:
        print "    apply(" + ", ".join(map(lambda i: "a" + str(i), range(1, t))) + ")"
    print "  }"
    print "}"
    print
    print "// Automatically generated - edit tools/thread-code.py, rather than this code"
    print "private[dataflow] class DFThread" + str(t) + "Runable[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, inBarriers: List[DFBarrier], outBarriers: List[DFBarrier], startThread:Boolean, manager: DFManager)"
    print "  extends DFThread" + str(t) + "X[" + typeChain(t) + "](manager, " + str(t) + ", inBarriers, outBarriers, startThread) {"
    print "  def runX() = {"
    print "    rval = f(" + ", ".join(map(lambda i: "arg" + str(i), range(1, t + 1))) + ")"
    print "    notifyListeners"
    print "  }"
    print "}"
    print

print
print "// DFManager"
print

for t in range(1, n + 1):
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def createThread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R): DFThread" + str(t) + "[" + typeChain(t) + "] = {"
    print "    createThread(f, List(): List[DFBarrier], List(): List[DFBarrier])"
    print "  }"
    print
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def createThread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, bIn: DFBarrier, bOut: DFBarrier): DFThread" + str(t) + "[" + typeChain(t) + "] = {"
    print "    createThread(f, List(bIn), List(bOut))"
    print "  }"
    print
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def createThread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, barIn: List[DFBarrier], barOut: List[DFBarrier]): DFThread" + str(t) + "[" + typeChain(t) + "] = {"
    print "    val thread = currentThread.get()"
    print "    val dt = new DFThread" + str(t) + "Runable(f, barIn, barOut, false, thread.manager)"
    print "    thread.addThread(dt)"
    print "    dt"
    print "  }"
    print
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  private[dataflow] def createStartThread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, manager: DFManager): DFThread" + str(t) + "[" + typeChain(t) + "] = {"
    print "    val dt = new DFThread" + str(t) + "Runable[" + typeChain(t) + "](f, List(), List(), true, manager)"
    print "    registerDFThread(manager, dt)"
    print "    dt"
    print "  }"
    print

print
print "// Dataflow"
print

for t in range(1, n + 1):
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def thread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R): DFThread" + str(t) + "[" + typeChain(t) + "] = DFManager.createThread(f)"
    print
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def thread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, " + ", ".join(map(lambda i: "a" + str(i) + ": Option[T" + str(i) + "]", range(1, t + 1))) + "): DFThread" + str(t) + "[" + typeChain(t) + "] = {"
    print "    val t = thread(f)"
    print "    t(" + ", ".join(map(lambda i: "a" + str(i), range(1, t + 1))) + ")"
    print "    t"
    print "  }"
    print
    print "  // Automatically generated - edit tools/thread-code.py, rather than this code"
    print "  def thread[" + typeChain(t) + "](f: (" + typeChain(t, includeR=False) + ") => R, " + ", ".join(map(lambda i: "a" + str(i) + ": OptionArg[T" + str(i) + "]", range(1, t + 1))) + "): DFThread" + str(t) + "[" + typeChain(t) + "] = thread(f, " + ", ".join(map(lambda i: "a" + str(i) + ".toOption", range(1, t + 1))) + ")"
    print
