package com.alaeri.presentation.wiki

import com.alaeri.log
import com.alaeri.logBlocking
import com.alaeri.logBlockingFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.Flow

class QueryRepository{
    private val mutableQuery = MutableStateFlow<String>("")
    suspend fun updateQuery(newQuery: String) = log(name = "edit query command"){
        mutableQuery.value = newQuery
    }
    val queryFlow: SharedFlow<String> = mutableQuery
    val queryFlowCommand: Flow<String>
        get() = queryFlow.log("query flow") //logBlockingFlow(name = "query flow") {  }
}