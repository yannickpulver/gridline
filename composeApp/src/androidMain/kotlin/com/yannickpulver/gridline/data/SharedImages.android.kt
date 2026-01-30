package com.yannickpulver.gridline.data

import kotlinx.coroutines.flow.Flow

actual fun observeSharedImages(): Flow<List<Pair<ByteArray, String>>> = PendingShareHolder.receive()
