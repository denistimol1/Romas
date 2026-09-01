package com.example.boxpacker.packing

/**
 * Размеры в миллиметрах (целые числа — так надёжнее для мемоизации,
 * чем работать с плавающей точкой).
 */
data class Dim(val w: Int, val d: Int, val h: Int)

/**
 * Уже размещённая коробка: (x,y,z) — координата угла коробки внутри ячейки,
 * (w,d,h) — её размеры В ТОЙ ОРИЕНТАЦИИ, в которой она поставлена.
 */
data class PlacedBox(val x: Int, val y: Int, val z: Int, val w: Int, val d: Int, val h: Int)

/**
 * Упаковщик одинаковых коробок в прямоугольную ячейку.
 *
 * Идея: на каждом шаге перебираем все 6 ориентаций коробки (какой гранью
 * она "смотрит" по каждой из осей), укладываем максимально плотную
 * regularную сетку nx*ny*nz этой ориентацией в оставшийся объём,
 * а оставшееся П-образное пространство разрезаем одним из 3 гильотинных
 * способов (вдоль X, вдоль Y или вдоль Z) и рекурсивно упаковываем остатки
 * — возможно, уже ДРУГОЙ ориентацией коробки. Берём вариант с максимумом.
 *
 * Это классическая эвристика "guillotine cut + wall building",
 * даёт очень хороший (часто оптимальный) результат для одного типа коробок.
 */
class Packer(private val box: Dim) {

    private val orientations: List<Dim> = listOf(
        Dim(box.w, box.d, box.h),
        Dim(box.w, box.h, box.d),
        Dim(box.d, box.w, box.h),
        Dim(box.d, box.h, box.w),
        Dim(box.h, box.w, box.d),
        Dim(box.h, box.d, box.w)
    ).distinct()

    private val countMemo = HashMap<Triple<Int, Int, Int>, Int>()

    /** Максимально возможное число коробок в объёме W×D×H. */
    fun countMax(W: Int, D: Int, H: Int): Int {
        if (W <= 0 || D <= 0 || H <= 0) return 0
        val key = Triple(W, D, H)
        countMemo[key]?.let { return it }

        var best = 0
        for (o in orientations) {
            if (o.w > W || o.d > D || o.h > H) continue
            val nx = W / o.w
            val ny = D / o.d
            val nz = H / o.h
            if (nx == 0 || ny == 0 || nz == 0) continue

            val count = nx * ny * nz
            val fw = nx * o.w
            val fd = ny * o.d
            val fh = nz * o.h
            val remW = W - fw
            val remD = D - fd
            val remH = H - fh

            // 3 варианта гильотинного разреза оставшегося П-объёма
            val opt1 = count + countMax(remW, D, H) + countMax(fw, remD, H) + countMax(fw, fd, remH)
            val opt2 = count + countMax(W, remD, H) + countMax(remW, fd, H) + countMax(fw, fd, remH)
            val opt3 = count + countMax(W, D, remH) + countMax(remW, D, fh) + countMax(fw, remD, fh)

            val localBest = maxOf(opt1, opt2, opt3)
            if (localBest > best) best = localBest
        }
        countMemo[key] = best
        return best
    }

    /** Возвращает полную раскладку коробок с координатами для отрисовки. */
    fun pack(W: Int, D: Int, H: Int): List<PlacedBox> {
        countMemo.clear()
        countMax(W, D, H) // прогреваем мемо
        return place(0, 0, 0, W, D, H)
    }

    private fun place(ox: Int, oy: Int, oz: Int, W: Int, D: Int, H: Int): List<PlacedBox> {
        if (W <= 0 || D <= 0 || H <= 0) return emptyList()
        val target = countMax(W, D, H)
        if (target == 0) return emptyList()

        for (o in orientations) {
            if (o.w > W || o.d > D || o.h > H) continue
            val nx = W / o.w
            val ny = D / o.d
            val nz = H / o.h
            if (nx == 0 || ny == 0 || nz == 0) continue

            val count = nx * ny * nz
            val fw = nx * o.w
            val fd = ny * o.d
            val fh = nz * o.h
            val remW = W - fw
            val remD = D - fd
            val remH = H - fh

            val opt1 = count + countMax(remW, D, H) + countMax(fw, remD, H) + countMax(fw, fd, remH)
            val opt2 = count + countMax(W, remD, H) + countMax(remW, fd, H) + countMax(fw, fd, remH)
            val opt3 = count + countMax(W, D, remH) + countMax(remW, D, fh) + countMax(fw, remD, fh)
            val localBest = maxOf(opt1, opt2, opt3)

            if (localBest == target) {
                val result = mutableListOf<PlacedBox>()
                for (ix in 0 until nx) for (iy in 0 until ny) for (iz in 0 until nz) {
                    result.add(
                        PlacedBox(
                            ox + ix * o.w, oy + iy * o.d, oz + iz * o.h,
                            o.w, o.d, o.h
                        )
                    )
                }
                when (localBest) {
                    opt1 -> {
                        result += place(ox + fw, oy, oz, remW, D, H)
                        result += place(ox, oy + fd, oz, fw, remD, H)
                        result += place(ox, oy, oz + fh, fw, fd, remH)
                    }
                    opt2 -> {
                        result += place(ox, oy + fd, oz, W, remD, H)
                        result += place(ox + fw, oy, oz, remW, fd, H)
                        result += place(ox, oy, oz + fh, fw, fd, remH)
                    }
                    else -> {
                        result += place(ox, oy, oz + fh, W, D, remH)
                        result += place(ox + fw, oy, oz, remW, D, fh)
                        result += place(ox, oy + fd, oz, fw, remD, fh)
                    }
                }
                return result
            }
        }
        return emptyList()
    }
}
