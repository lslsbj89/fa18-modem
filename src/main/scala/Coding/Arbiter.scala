package modem

import chisel3._
import chisel3.util._
import cordic.CordicParams
import dsptools.numbers._
import modem.{BranchMetric, CodingParams, Trellis}
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class ArbiterIO[T <: Data: Real](params: CodingParams[T]) extends Bundle{
  val headInfo  = Flipped(Decoupled(DecodeHeadBundle()))
  val lenCnt    = Input(Bool())    // dataInfo.valid indicates whether length counter reaches to target length value set by header
  val in        = Flipped(Decoupled(BitsBundle(params)))
  val pktStart  = Input(Bool())
  val pktEnd    = Input(Bool())

  val isHead    = Output(Bool())
  val hdrEnd    = Output(Bool())
}

object ArbiterIO {
  def apply[T <: Data: Real](params: CodingParams[T]): ArbiterIO[T] = new ArbiterIO(params)
}

// Written by Kunmo Kim : kunmok@berkeley.edu
// Description: Arbiter identifies whether the incoming packet contains header information or payload
class Arbiter[T <: Data: Real](params: CodingParams[T]) extends Module {
  val io = IO(ArbiterIO(params))
  val isHeadReg   = RegInit(true.B)
  val hdrCounter  = RegInit(0.U(6.W))
  val hdrEndReg   = RegInit(false.B)

  when(io.pktStart === true.B && isHeadReg === false.B && io.lenCnt === true.B){
    isHeadReg := true.B
    isHeadReg := 1.U
  }
  when(isHeadReg === true.B){
    hdrCounter := hdrCounter + 1.U
    when(hdrCounter === (params.H-1).U){
      hdrCounter  := 0.U
      isHeadReg   := false.B
    }
  }
  when((isHeadReg === true.B) && (hdrCounter === (params.H-2).U)){
    hdrEndReg   := true.B
  }.otherwise{
    hdrEndReg   := false.B
  }

  io.isHead         := isHeadReg
  io.in.ready       := true.B
  io.hdrEnd         := hdrEndReg
  io.headInfo.ready := true.B
}

