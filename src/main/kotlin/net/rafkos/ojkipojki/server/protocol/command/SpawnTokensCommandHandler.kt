package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.SpriteModel
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

class SpawnTokensCommandHandler : Handler<SpawnTokensCommand> {

    private data class SpriteCluster(
        val sprites: List<SpriteModel>,
        val tokenWidth: Int,
        val tokenHeight: Int,
    ) {
        val gridCols: Int = ceil(sqrt(sprites.size.toDouble())).toInt().coerceAtLeast(1)
        val gridRows: Int = ceil(sprites.size.toDouble() / gridCols).toInt()
        val spacingX: Int = tokenWidth + WITHIN_PADDING
        val spacingY: Int = tokenHeight + WITHIN_PADDING
        val width: Int = (gridCols - 1) * spacingX + tokenWidth
        val height: Int = (gridRows - 1) * spacingY + tokenHeight
    }

    companion object {
        private val log = LogManager.getLogger(SpawnTokensCommandHandler::class.java)
        private const val SIZE_TOLERANCE = 0.20
        private const val WITHIN_PADDING = 10
        private const val CLUSTER_GAP = 50
        private const val FALLBACK_SIZE = 80
    }

    override fun handle(action: SpawnTokensCommand) {
        val bag = ServerContext.modelRepository.findSpriteBagById(action.spriteBagId) ?: return
        val sprites = selectSprites(bag.sprites, action.spriteId)
        if (sprites.isEmpty()) return

        val baseIndex = nextBaseIndex()
        val clusters = groupBySimilarSize(sprites)
        val count = action.count.coerceAtLeast(1)
        val setCols = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)
        val spawnedIds = mutableSetOf<TokenId>()

        for (setIndex in 0 until count) {
            val setOrigin = computeSetOrigin(action.position, setIndex, setCols, clusters)
            spawnSet(clusters, setOrigin, baseIndex + setIndex * sprites.size, spawnedIds)
        }

        broadcastSpawnedTokens(spawnedIds)
    }

    private fun selectSprites(sprites: List<SpriteModel>, spriteId: SpriteId?): List<SpriteModel> =
        if (spriteId != null) sprites.filter { it.id == spriteId } else sprites

    private fun nextBaseIndex(): Int =
        (ServerContext.modelRepository.findAllTokens().maxOfOrNull { it.index.value } ?: -1) + 1

    private fun groupBySimilarSize(sprites: List<SpriteModel>): List<SpriteCluster> {
        val groups = mutableListOf<MutableList<SpriteModel>>()
        val representatives = mutableListOf<Pair<Int, Int>>()

        for (sprite in sprites) {
            val (w, h) = spriteSize(sprite)
            val idx = representatives.indexOfFirst { (gw, gh) -> similarSize(w, h, gw, gh) }
            if (idx >= 0) {
                groups[idx].add(sprite)
            } else {
                groups.add(mutableListOf(sprite))
                representatives.add(w to h)
            }
        }

        return groups.zip(representatives).map { (group, rep) ->
            SpriteCluster(group, rep.first, rep.second)
        }
    }

    private fun spriteSize(sprite: SpriteModel): Pair<Int, Int> {
        if (sprite.frontImageBytes.isEmpty()) return FALLBACK_SIZE to FALLBACK_SIZE
        return try {
            val img = ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes))
            if (img != null) img.width to img.height else FALLBACK_SIZE to FALLBACK_SIZE
        } catch (e: Exception) {
            log.warn("Failed to decode sprite image for size estimation: ${sprite.id}", e)
            FALLBACK_SIZE to FALLBACK_SIZE
        }
    }

    private fun similarSize(w1: Int, h1: Int, w2: Int, h2: Int): Boolean {
        val wDiff = abs(w1 - w2).toDouble() / max(w1, w2)
        val hDiff = abs(h1 - h2).toDouble() / max(h1, h2)
        return wDiff <= SIZE_TOLERANCE && hDiff <= SIZE_TOLERANCE
    }

    private fun computeSetOrigin(position: Position?, setIndex: Int, setCols: Int, clusters: List<SpriteCluster>): Position {
        if (position == null) return Position(x = Random.nextInt(-400, 401), y = Random.nextInt(-400, 401))
        val setWidth = clusters.sumOf { it.width } + (clusters.size - 1) * CLUSTER_GAP
        val setHeight = clusters.maxOf { it.height }
        val setCol = setIndex % setCols
        val setRow = setIndex / setCols
        return Position(
            x = position.x + setCol * (setWidth + CLUSTER_GAP),
            y = position.y + setRow * (setHeight + CLUSTER_GAP),
        )
    }

    private fun spawnSet(clusters: List<SpriteCluster>, origin: Position, startIndex: Int, spawnedIds: MutableSet<TokenId>) {
        var clusterOffsetX = 0
        var tokenIdx = startIndex
        for (cluster in clusters) {
            spawnCluster(cluster, origin.x + clusterOffsetX, origin.y, tokenIdx, spawnedIds)
            clusterOffsetX += cluster.width + CLUSTER_GAP
            tokenIdx += cluster.sprites.size
        }
    }

    private fun spawnCluster(cluster: SpriteCluster, originX: Int, originY: Int, startIndex: Int, spawnedIds: MutableSet<TokenId>) {
        cluster.sprites.forEachIndexed { i, sprite ->
            val col = i % cluster.gridCols
            val row = i / cluster.gridCols
            val model = TokenModel()
            model.id = TokenId(UUID.randomUUID())
            model.spriteId = sprite.id
            model.position.apply(Position(x = originX + col * cluster.spacingX, y = originY + row * cluster.spacingY))
            model.index.value = startIndex + i
            ServerContext.modelRepository.saveToken(model)
            spawnedIds.add(model.id)
        }
    }

    private fun broadcastSpawnedTokens(spawnedIds: Set<TokenId>) {
        val tokenActions = ServerContext.modelRepository.findAllTokens()
            .filter { it.id in spawnedIds }
            .map { SomeTokensUpdatedEvent.TokenAction.Update(it.toState()) }
        ServerContext.eventBroadcastService.broadcast(SomeTokensUpdatedEvent(tokenActions))
    }
}
