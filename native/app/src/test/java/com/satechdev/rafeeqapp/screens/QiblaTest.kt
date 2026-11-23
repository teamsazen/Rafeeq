package com.satechdev.rafeeqapp.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Qibla bearings against published values, and the turn advice (flutter_qiblah's rules). */
class QiblaTest {
    @Test
    fun bearings() {
        assertEquals(119.0, qiblaBearing(51.5074, -0.1278), .5) // London
        assertEquals(58.5, qiblaBearing(40.7128, -74.0060), .5) // New York
        assertEquals(295.2, qiblaBearing(-6.2088, 106.8456), .5) // Jakarta
        assertEquals(267.7, qiblaBearing(24.8607, 67.0011), .5) // Karachi
    }

    @Test
    fun turns() {
        assertEquals(Turn.Facing, Turn.of(4.0))
        assertEquals(Turn.Facing, Turn.of(-4.0))
        assertEquals(Turn.Left(30), Turn.of(30.0))
        assertEquals(Turn.Right(30), Turn.of(-30.0))
        assertEquals(Turn.Left(180), Turn.of(180.0))
    }
}
