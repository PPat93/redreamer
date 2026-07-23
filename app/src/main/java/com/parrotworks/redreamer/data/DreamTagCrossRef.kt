package com.parrotworks.redreamer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "dream_tag_cross_ref",
    primaryKeys = ["dreamId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Dream::class,
            parentColumns = ["id"],
            childColumns = ["dreamId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dreamId"), Index("tagId")],
)
data class DreamTagCrossRef(
    val dreamId: Long,
    val tagId: Long,
)
