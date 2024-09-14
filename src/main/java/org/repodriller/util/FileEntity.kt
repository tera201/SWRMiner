package org.repodriller.util

data class FileEntity(var fileAdded:Int, var fileDeleted:Int, var fileModified:Int, var linesAdded:Int, var linesDeleted:Int, var linesModified:Int, var changes:Int, var changesSize: Int) {

    fun add(fileEntity: FileEntity) {
        fileAdded += fileEntity.fileAdded
        fileDeleted += fileEntity.fileDeleted
        fileModified += fileEntity.fileModified
        linesAdded += fileEntity.linesAdded
        linesDeleted += fileEntity.linesDeleted
        linesModified += fileEntity.linesModified
        changes += fileEntity.changes
    }
    fun plus(fileAdded:Int, fileDeleted:Int, fileModified:Int, linesAdded:Int, linesDeleted:Int, linesModified:Int, changes:Int, changesSize:Int) {
        this.fileAdded += fileAdded
        this.fileDeleted += fileDeleted
        this.fileModified += fileModified
        this.linesAdded += linesAdded
        this.linesDeleted += linesDeleted
        this.linesModified += linesModified
        this.changes += changes
        this.changesSize += changesSize
    }
}
