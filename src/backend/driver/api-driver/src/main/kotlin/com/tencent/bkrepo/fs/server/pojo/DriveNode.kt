package com.tencent.bkrepo.fs.server.pojo

import java.time.LocalDateTime

data class DriveNode(
    /**
     * 节点ID（ObjectId）
     */
    val id: String,

    /**
     * 父节点ID
     */
    val parentId: String,

    /**
     * 文件/目录名
     */
    val name: String,

    /**
     * 是否为目录
     */
    val folder: Boolean,

    /**
     * 文件大小（字节）
     */
    val size: Long,

    /**
     * 访问时间（纳秒精度）
     */
    val lastAccessDate: LocalDateTime,

    /**
     * 修改时间（纳秒精度）
     */
    val lastModifiedDate: LocalDateTime,

    /**
     * 创建时间
     */
    val createdDate: LocalDateTime,

    /**
     * 创建者
     */
    val createdBy: String,

    /**
     * 最后修改者
     */
    val lastModifiedBy: String,

    /**
     * 文件模式和类型
     */
    val mode: Int,

    /**
     * 硬链接数
     */
    val nlink: Int,

    /**
     * 用户ID
     */
    val uid: String,

    /**
     * 组ID
     */
    val gid: String,

    /**
     * 设备ID
     */
    val rdev: Int,

    /**
     * 文件标志
     */
    val flags: Int,

    /**
     * 文件类型
     * 1: 普通文件, 2: 目录, 3: 符号链接, 4: 块设备, 5: 字符设备, 6: FIFO, 7: Socket
     */
    val type: Int,

    /**
     * 逻辑删除标记
     */
    val deleted: Boolean,

    /**
     * 删除时间（纳秒精度）
     */
    val deletedAt: LocalDateTime?,
)