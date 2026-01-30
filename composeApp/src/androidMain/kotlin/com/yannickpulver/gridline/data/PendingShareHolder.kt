package com.yannickpulver.gridline.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object PendingShareHolder {
    private val channel = Channel<List<Pair<ByteArray, String>>>(Channel.UNLIMITED)

    fun send(images: List<Pair<ByteArray, String>>) {
        channel.trySend(images)
    }

    fun receive(): Flow<List<Pair<ByteArray, String>>> = channel.receiveAsFlow()
}
