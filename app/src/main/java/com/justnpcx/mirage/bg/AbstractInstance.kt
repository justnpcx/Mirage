package com.justnpcx.mirage.bg

import java.io.Closeable

interface AbstractInstance : Closeable {

    fun launch()

}