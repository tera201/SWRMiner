package org.repodriller.util

data class BlameEntity(val projectId:Int, val authorId:Int, val blameFileId:Int, val blameHash:String, val lineId:Int, val lineSize:Long)
