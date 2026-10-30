package com.wade.fsime.engine.zhuyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhuyinTest {

    @Test
    fun testZhuyinEncodingExamples() {
        // 中: ㄓ ㄨ ㄥ (first tone) -> 5j/
        val zhong = ZhuyinSyllable(initial = "ㄓ", medial = "ㄨ", `final` = "ㄥ", tone = " ")
        assertEquals("5j/", ZhuyinCodeEncoder.encodeSyllable(zhong))

        // 好: ㄏ ㄠ ˇ -> cl3
        val hao = ZhuyinSyllable(initial = "ㄏ", `final` = "ㄠ", tone = "ˇ")
        assertEquals("cl3", ZhuyinCodeEncoder.encodeSyllable(hao))

        // 灣: ㄨ ㄢ (zero initial, first tone) -> j0
        val wan = ZhuyinSyllable(medial = "ㄨ", `final` = "ㄢ", tone = " ")
        assertEquals("j0", ZhuyinCodeEncoder.encodeSyllable(wan))

        // 你: ㄋ ㄧ ˇ -> su3
        val ni = ZhuyinSyllable(initial = "ㄋ", medial = "ㄧ", tone = "ˇ")
        assertEquals("su3", ZhuyinCodeEncoder.encodeSyllable(ni))

        // 意: ㄧ ˋ (zero initial, fourth tone) -> u4
        val yi = ZhuyinSyllable(medial = "ㄧ", tone = "ˋ")
        assertEquals("u4", ZhuyinCodeEncoder.encodeSyllable(yi))

        // 台: ㄊ ㄞ ˊ -> w96
        val tai = ZhuyinSyllable(initial = "ㄊ", `final` = "ㄞ", tone = "ˊ")
        assertEquals("w96", ZhuyinCodeEncoder.encodeSyllable(tai))
    }

    @Test
    fun testStateMachineTyping() {
        val sm = ZhuyinStateMachine()
        
        // Type ㄊ ㄞ ˊ
        sm.processKey("w") // ㄊ
        assertEquals("ㄊ", sm.currentDisplay())
        assertEquals("w", sm.currentSearchCode())

        sm.processKey("9") // ㄞ
        assertEquals("ㄊㄞ", sm.currentDisplay())
        assertEquals("w9", sm.currentSearchCode())

        val res = sm.processKey("6") // ˊ (2nd tone)
        assertTrue(res is ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed)
        val closed = (res as ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed)
        assertEquals("w96", closed.searchCode)
    }

    @Test
    fun testAutoSegmentation() {
        val sm = ZhuyinStateMachine()
        // Type ㄓ ㄨ ㄥ ㄋ ㄧ ˇ
        sm.processKey("5") // ㄓ
        sm.processKey("j") // ㄨ
        sm.processKey("/") // ㄥ
        // Now typing next initial 's' (ㄋ) should auto-segment previous as first tone
        val segRes = sm.processKey("s")
        assertTrue(segRes is ZhuyinStateMachine.ZhuyinStepResult.AutoSegmented)
        val autoSeg = segRes as ZhuyinStateMachine.ZhuyinStepResult.AutoSegmented
        assertEquals("ㄓ", autoSeg.completed.initial)
        assertEquals("ㄨ", autoSeg.completed.medial)
        assertEquals("ㄥ", autoSeg.completed.`final`)
        assertEquals(" ", autoSeg.completed.tone)
        assertEquals("5j/", ZhuyinCodeEncoder.encodeSyllable(autoSeg.completed))
        assertEquals("ㄋ", autoSeg.newSyllable.initial)
    }

    @Test
    fun testTone1Space() {
        val sm = ZhuyinStateMachine()
        // Type ㄨ ㄢ (灣)
        sm.processKey("j") // ㄨ
        sm.processKey("0") // ㄢ
        assertTrue(sm.currentSyllableHasNucleus())
        assertEquals(false, sm.currentSyllableHasTone())

        // Press Space -> closeWithTone1
        val res = sm.closeWithTone1()
        assertTrue(res is ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed)
        val closed = (res as ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed)
        assertEquals("j0", closed.searchCode)
    }
}
