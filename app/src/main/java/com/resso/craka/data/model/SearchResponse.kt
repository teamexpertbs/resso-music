package com.resso.craka.data.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("query") val query: String = "",
    @SerializedName("total") val total: Int = 0,
    @SerializedName("songs") val songs: List<Song> = emptyList()
)
