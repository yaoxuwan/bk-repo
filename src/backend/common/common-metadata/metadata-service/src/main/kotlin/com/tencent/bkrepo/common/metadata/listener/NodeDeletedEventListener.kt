package com.tencent.bkrepo.common.metadata.listener

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.event.node.NodeDeletedEvent
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.service.blocknode.BlockNodeService
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import org.springframework.context.annotation.Conditional
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@Conditional(SyncCondition::class)
class NodeDeletedEventListener(
    private val nodeService: NodeService,
    private val blockNodeService: BlockNodeService
) {

    @Async
    @EventListener(NodeDeletedEvent::class)
    fun handle(event: NodeDeletedEvent) {
        consumer(event)
    }

    private fun consumer(event: NodeDeletedEvent) {
        with(event) {
            val node = nodeService.getDeletedNodeDetail(ArtifactInfo(projectId, repoName, resourceKey)).firstOrNull()
            if (node?.folder != true) {
                blockNodeService.deleteBlocks(projectId, repoName, resourceKey, null)
            }
        }
    }
}