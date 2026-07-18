package com.ncy.notechaty.data.sync.pear

import javax.inject.Inject
import javax.inject.Singleton

/** Production placeholder until the experimental Pear transport graduates from Ncy Dev. */
@Singleton
class PearRuntimeController @Inject constructor() {
    fun start() = Unit
    fun stop() = Unit
}
