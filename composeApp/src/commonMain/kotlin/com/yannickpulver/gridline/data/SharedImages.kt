package com.yannickpulver.gridline.data

import kotlinx.coroutines.flow.Flow

expect fun observeSharedImages(): Flow<List<Pair<ByteArray, String>>>
