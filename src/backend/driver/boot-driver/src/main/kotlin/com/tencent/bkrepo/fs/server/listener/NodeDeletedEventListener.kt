package com.tencent.bkrepo.fs.server.listener

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.event.node.NodeDeletedEvent
import com.tencent.bkrepo.common.metadata.condition.ReactiveCondition
import com.tencent.bkrepo.common.metadata.service.blocknode.BlockNodeService
import com.tencent.bkrepo.common.metadata.service.blocknode.RBlockNodeService
import com.tencent.bkrepo.fs.server.service.node.RNodeService
import org.springframework.context.annotation.Conditional
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
@Conditional(ReactiveCondition::class)
class NodeDeletedEventListener(
    private val nodeService: RNodeService,
    private val blockNodeService: RBlockNodeService
) {

    @Async
    @EventListener(NodeDeletedEvent::class)
    suspend fun handle(event: NodeDeletedEvent) {
        consumer(event)
    }

    private suspend fun consumer(event: NodeDeletedEvent) {
        with(event) {
            val node = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, resourceKey))
            if (node?.folder != true) {
                blockNodeService.deleteBlocks(projectId, repoName, resourceKey)
            }
        }
    }
}