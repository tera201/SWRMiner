package org.repodriller.util

data class CommitEntity(
    val projectId: Int,
    val authorId: String,
    val authorName: String,
    val authorEmail: String,
    val hash: String,
    val date: Int,
    val projectSize: Long,
    val stability: Double,
    val fileEntity: FileEntity
)
