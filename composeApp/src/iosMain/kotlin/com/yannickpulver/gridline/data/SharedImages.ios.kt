package com.yannickpulver.gridline.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun observeSharedImages(): Flow<List<Pair<ByteArray, String>>> = emptyFlow()
