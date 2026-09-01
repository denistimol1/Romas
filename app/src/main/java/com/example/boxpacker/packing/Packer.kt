package com.example.boxpacker.packing

/** Описание одного типа коробки. quantity = null означает "неограниченно". */
data class BoxType(val id: Int, val name: String, val w: Int, val d: Int, val h: Int, val quantity: Int?)

/** Уже размещённая коробка конкретного типа. */
data class PlacedBox(val typeId: Int, val x: Int, val y: Int, val z: Int, val w: Int, val d: Int, val h: Int)

private data class FreeSpace(val x: Int, val y: Int, val z: Int, val w: Int, val d: Int, val h: Int)

private data class Orientation(val typeId: Int, val w: Int, val d: Int, val h: Int)

/**
 * Упаковщик НЕСКОЛЬКИХ типов коробок в одну ячейку.
 *
 * Жадный алгоритм: на каждом шаге среди всех свободных областей, всех типов
 * коробок и всех их ориентаций ищем комбинацию, которая позволяет уложить
 * максимальное число коробок одной плотной сеткой (с учётом остатка по
 * количеству для данного типа). Укладываем её, а оставшееся П-образное
 * пространство разбиваем на 3 свободные области (гильотинный разрез) и
 * повторяем, пока хоть что-то помещается.
 *
 * Если для типа не задано ограничение по количеству, алгоритм будет часто
 * выбирать наиболее "мелкую" по объёму коробку, потому что она даёт больше
 * штук в том же объёме — это математически верно для цели "максимум
 * количества". Чтобы получить контролируемую смесь типов, указывай
 * конкретное количество для каждого типа.
 */
class MultiPacker(private val types: List<BoxType>) {

    private fun orientationsFor(t: BoxType): List<Orientation> =
        listOf(
            Triple(t.w, t.d, t.h), Triple(t.w, t.h, t.d),
            Triple(t.d, t.w, t.h), Triple(t.d, t.h, t.w),
            Triple(t.h, t.w, t.d), Triple(t.h, t.d, t.w)
        ).distinct().map { Orientation(t.id, it.first, it.second, it.third) }

    fun pack(cellW: Int, cellD: Int, cellH: Int): List<PlacedBox> {
        val remaining = types.associate { it.id to (it.quantity ?: Int.MAX_VALUE) }.toMutableMap()
        val freeSpaces = mutableListOf(FreeSpace(0, 0, 0, cellW, cellD, cellH))
        val placed = mutableListOf<PlacedBox>()

        while (true) {
            var bestCount = 0L
            var bestVolume = -1L
            var bestSpaceIdx = -1
            var bestOrientation: Orientation? = null
            var bestNx = 0; var bestNy = 0; var bestNz = 0

            for ((si, space) in freeSpaces.withIndex()) {
                for (t in types) {
                    val qty = remaining[t.id] ?: 0
                    if (qty <= 0) continue
                    for (o in orientationsFor(t)) {
                        if (o.w > space.w || o.d > space.d || o.h > space.h) continue
                        var nx = space.w / o.w
                        var ny = space.d / o.d
                        var nz = space.h / o.h
                        if (nx == 0 || ny == 0 || nz == 0) continue

                        var count = nx.toLong() * ny * nz
                        if (count > qty) {
                            while (count > qty && nz > 1) { nz--; count = nx.toLong() * ny * nz }
                            while (count > qty && ny > 1) { ny--; count = nx.toLong() * ny * nz }
                            while (count > qty && nx > 1) { nx--; count = nx.toLong() * ny * nz }
                            if (count > qty) continue
                        }
                        if (count <= 0) continue

                        val volume = count * o.w.toLong() * o.d * o.h
                        if (count > bestCount || (count == bestCount && volume > bestVolume)) {
                            bestCount = count
                            bestVolume = volume
                            bestSpaceIdx = si
                            bestOrientation = o
                            bestNx = nx; bestNy = ny; bestNz = nz
                        }
                    }
                }
            }

            val o = bestOrientation ?: break
            val space = freeSpaces[bestSpaceIdx]
            val nx = bestNx; val ny = bestNy; val nz = bestNz

            for (ix in 0 until nx) for (iy in 0 until ny) for (iz in 0 until nz) {
                placed.add(
                    PlacedBox(
                        o.typeId,
                        space.x + ix * o.w, space.y + iy * o.d, space.z + iz * o.h,
                        o.w, o.d, o.h
                    )
                )
            }
            remaining[o.typeId] = (remaining[o.typeId] ?: 0) - nx * ny * nz

            val fw = nx * o.w; val fd = ny * o.d; val fh = nz * o.h
            val remW = space.w - fw; val remD = space.d - fd; val remH = space.h - fh

            freeSpaces.removeAt(bestSpaceIdx)
            if (remW > 0) freeSpaces.add(FreeSpace(space.x + fw, space.y, space.z, remW, space.d, space.h))
            if (remD > 0) freeSpaces.add(FreeSpace(space.x, space.y + fd, space.z, fw, remD, space.h))
            if (remH > 0) freeSpaces.add(FreeSpace(space.x, space.y, space.z + fh, fw, fd, remH))
        }
        return placed
    }
}
