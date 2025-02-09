package com.alaeri.domain.wiki

sealed class LoadingStatus{
    data class Loading(val searchTerm: String?): LoadingStatus()
    data class Parsing(val contentLength: Long): LoadingStatus()
    data class Filtering(val title: String): LoadingStatus()
    data class Done(val result: WikiArticle): LoadingStatus()
    data class Empty(val emptyStatusReason: EmptyStatusReason): LoadingStatus()
    data class Error(val message: String, val e: Throwable) : LoadingStatus()
}
