package com.dividetask.homeschoolteacher.chess

import kotlin.random.Random

enum class PieceColor { White, Black }
enum class PieceType { Queen, Rook, Bishop, Pawn }

data class Piece(val type: PieceType, val color: PieceColor)

/**
 * 8x8 board, indexed 0..63 in row-major order. Index 0 is rank 8 file a
 * (top-left when drawing white at the bottom); index 63 is rank 1 file h.
 * The exact orientation does not affect puzzle logic, only how the UI
 * draws it.
 */
typealias Board = List<Piece?>

object ChessEngine {

    fun rowCol(index: Int): Pair<Int, Int> = index / 8 to index % 8

    fun index(row: Int, col: Int): Int = row * 8 + col

    /**
     * (row, col) deltas describing the directions a sliding piece can
     * travel one step at a time. Pawn is excluded — pawns never move in
     * our puzzles, they're only capture targets.
     */
    private fun directionsOf(type: PieceType): List<Pair<Int, Int>> = when (type) {
        PieceType.Queen -> listOf(
            -1 to -1, -1 to 0, -1 to 1,
             0 to -1,           0 to 1,
             1 to -1,  1 to 0,  1 to 1,
        )
        PieceType.Rook -> listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        PieceType.Bishop -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        PieceType.Pawn -> emptyList()
    }

    /**
     * Squares the piece at `pieceIdx` can move to in a single move,
     * obeying line-of-sight along its movement axes. The first piece
     * encountered along each ray is included (since the moving piece can
     * capture it), but nothing past that.
     */
    fun reach(pieceIdx: Int, board: Board): Set<Int> {
        val piece = board[pieceIdx] ?: return emptySet()
        val (pr, pc) = rowCol(pieceIdx)
        val out = mutableSetOf<Int>()
        for ((dr, dc) in directionsOf(piece.type)) {
            var r = pr + dr
            var c = pc + dc
            while (r in 0..7 && c in 0..7) {
                val idx = index(r, c)
                out.add(idx)
                if (board[idx] != null) break
                r += dr
                c += dc
            }
        }
        return out
    }

    /** Indices of pawns capturable by the piece at `pieceIdx`. */
    fun capturablePawns(pieceIdx: Int, board: Board): Set<Int> {
        val piece = board[pieceIdx] ?: return emptySet()
        return reach(pieceIdx, board).filter { idx ->
            val target = board[idx]
            target != null && target.type == PieceType.Pawn && target.color != piece.color
        }.toSet()
    }
}

data class ChessPuzzle(
    val board: Board,
    val playerIndex: Int,
    val playerColor: PieceColor,
    val playerType: PieceType,
    val capturable: Set<Int>,
) {
    val pawnIndices: Set<Int> = board.withIndex()
        .filter { it.value?.type == PieceType.Pawn }
        .map { it.index }
        .toSet()
}

/**
 * What kind of puzzle a chess lesson generates: which player piece,
 * and whether at least one friendly pawn is placed alongside the
 * opposing pawns to act as a distractor / blocker.
 */
data class ChessLevelConfig(
    val playerType: PieceType,
    val includeFriendlyPawns: Boolean,
) {
    companion object {
        val Queen: ChessLevelConfig = ChessLevelConfig(PieceType.Queen, false)
        val QueenWithFriendly: ChessLevelConfig = ChessLevelConfig(PieceType.Queen, true)
        val Rook: ChessLevelConfig = ChessLevelConfig(PieceType.Rook, true)
        val Bishop: ChessLevelConfig = ChessLevelConfig(PieceType.Bishop, true)
    }
}

object ChessPuzzleGenerator {

    /**
     * Generates a puzzle for the given level: at least one opposite-colour
     * pawn capturable by the player piece, at least one opposite-colour
     * pawn not capturable, and (when the config asks for it) at least one
     * friendly pawn.
     */
    fun generate(config: ChessLevelConfig = ChessLevelConfig.Queen): ChessPuzzle {
        repeat(500) {
            val opposingPawnCount = Random.nextInt(3, 7)
            val friendlyPawnCount = if (config.includeFriendlyPawns) Random.nextInt(1, 3) else 0
            val playerIdx = Random.nextInt(64)
            val playerColor = if (Random.nextBoolean()) PieceColor.White else PieceColor.Black
            val opposingColor = if (playerColor == PieceColor.White) PieceColor.Black else PieceColor.White
            val board = MutableList<Piece?>(64) { null }
            board[playerIdx] = Piece(config.playerType, playerColor)
            // Pawns never appear on rank 1 (row 7) or rank 8 (row 0) — those
            // are promotion / starting-rank squares in real chess and the
            // app's puzzles follow the same convention. The player piece
            // itself may still occupy any square.
            val open = (0..63)
                .filter { it != playerIdx && it / 8 in 1..6 }
                .toMutableList()
                .also { it.shuffle() }
            if (open.size < opposingPawnCount + friendlyPawnCount) return@repeat
            for (i in 0 until opposingPawnCount) {
                board[open[i]] = Piece(PieceType.Pawn, opposingColor)
            }
            for (i in 0 until friendlyPawnCount) {
                board[open[opposingPawnCount + i]] = Piece(PieceType.Pawn, playerColor)
            }
            val cap = ChessEngine.capturablePawns(playerIdx, board)
            val opposingNonCapturable = opposingPawnCount - cap.size
            if (cap.isNotEmpty() && opposingNonCapturable >= 1) {
                return ChessPuzzle(
                    board = board.toList(),
                    playerIndex = playerIdx,
                    playerColor = playerColor,
                    playerType = config.playerType,
                    capturable = cap,
                )
            }
        }
        return deterministicFallback(config)
    }

    /**
     * Hand-crafted board used in the (very rare) case the random sampler
     * fails. Player at d4; one capturable opposing pawn on a reachable
     * square; one non-capturable opposing pawn off-axis; one friendly
     * pawn off-axis when the level wants one. All pawn squares are
     * within rows 1..6 (never rank 1 or 8).
     */
    private fun deterministicFallback(config: ChessLevelConfig): ChessPuzzle {
        val board = MutableList<Piece?>(64) { null }
        val playerIdx = ChessEngine.index(4, 3) // d4
        board[playerIdx] = Piece(config.playerType, PieceColor.White)
        val capturableIdx = when (config.playerType) {
            // d6 — straight up from d4 (works for queen and rook).
            PieceType.Queen, PieceType.Rook -> ChessEngine.index(2, 3)
            // f6 — up-right diagonal from d4 (works for queen and bishop).
            PieceType.Bishop -> ChessEngine.index(2, 5)
            PieceType.Pawn -> error("Pawn cannot be the player piece")
        }
        board[capturableIdx] = Piece(PieceType.Pawn, PieceColor.Black)
        // a6 (row 2, col 0): not on any rank/file/diagonal from d4 for
        // queen, rook, or bishop — so unreachable for every player piece.
        board[ChessEngine.index(2, 0)] = Piece(PieceType.Pawn, PieceColor.Black)
        if (config.includeFriendlyPawns) {
            // h7 (row 1, col 7): off every ray from d4 for every player
            // piece, same colour as the player.
            board[ChessEngine.index(1, 7)] = Piece(PieceType.Pawn, PieceColor.White)
        }
        return ChessPuzzle(
            board = board.toList(),
            playerIndex = playerIdx,
            playerColor = PieceColor.White,
            playerType = config.playerType,
            capturable = ChessEngine.capturablePawns(playerIdx, board),
        )
    }
}
