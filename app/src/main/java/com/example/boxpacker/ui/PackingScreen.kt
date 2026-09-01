package com.example.boxpacker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpacker.packing.BoxType
import com.example.boxpacker.packing.MultiPacker
import com.example.boxpacker.packing.PlacedBox

/**
 * ВАЖНО: поля объявлены через `by mutableStateOf`, а не просто `var`.
 * Без этого Compose не видит изменений полей (switch, текстовые поля
 * внутри карточки), и экран визуально "не реагирует" на ввод.
 */
private class BoxTypeInput(
    val id: Int,
    name: String = "",
    w: String = "",
    d: String = "",
    h: String = "",
    unlimited: Boolean = true,
    qty: String = ""
) {
    var name by mutableStateOf(name)
    var w by mutableStateOf(w)
    var d by mutableStateOf(d)
    var h by mutableStateOf(h)
    var unlimited by mutableStateOf(unlimited)
    var qty by mutableStateOf(qty)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingScreen() {
    var cellW by remember { mutableStateOf("1200") }
    var cellD by remember { mutableStateOf("800") }
    var cellH by remember { mutableStateOf("1000") }

    var nextId by remember { mutableStateOf(1) }
    val boxTypeInputs = remember {
        mutableStateListOf(
            BoxTypeInput(id = 0, name = "Коробка 1", w = "300", d = "200", h = "150", unlimited = true)
        )
    }

    var result by remember { mutableStateOf<List<PlacedBox>?>(null) }
    var cellDims by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var typesUsed by remember { mutableStateOf<List<BoxType>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Упаковка коробок в ячейку") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Размеры ячейки, мм", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactField("Ширина", cellW) { cellW = it }
                CompactField("Глубина", cellD) { cellD = it }
                CompactField("Высота", cellH) { cellH = it }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Типы коробок", fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    boxTypeInputs.add(
                        BoxTypeInput(id = nextId, name = "Коробка ${nextId + 1}")
                    )
                    nextId++
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить")
                }
            }

            boxTypeInputs.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { item.name = it },
                            label = { Text("Название") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (boxTypeInputs.size > 1) {
                            IconButton(onClick = { boxTypeInputs.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactField("Ш", item.w) { item.w = it }
                        CompactField("Г", item.d) { item.d = it }
                        CompactField("В", item.h) { item.h = it }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Неограниченно", fontSize = 13.sp)
                        Switch(
                            checked = item.unlimited,
                            onCheckedChange = { item.unlimited = it }
                        )
                        if (!item.unlimited) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = item.qty,
                                onValueChange = { item.qty = it.filter { c -> c.isDigit() } },
                                label = { Text("Кол-во") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                error = null
                try {
                    val W = cellW.toInt(); val D = cellD.toInt(); val H = cellH.toInt()
                    require(W > 0 && D > 0 && H > 0) { "Размеры ячейки должны быть положительными" }

                    val types = boxTypeInputs.mapIndexed { idx, item ->
                        val bw = item.w.toInt(); val bd = item.d.toInt(); val bh = item.h.toInt()
                        require(bw > 0 && bd > 0 && bh > 0) { "Проверь размеры коробки \"${item.name}\"" }
                        val qty = if (item.unlimited) null else {
                            val q = item.qty.toIntOrNull()
                            require(q != null && q > 0) { "Укажи количество для \"${item.name}\"" }
                            q
                        }
                        BoxType(id = idx, name = item.name.ifBlank { "Коробка ${idx + 1}" }, w = bw, d = bd, h = bh, quantity = qty)
                    }

                    val packer = MultiPacker(types)
                    result = packer.pack(W, D, H)
                    cellDims = Triple(W, D, H)
                    typesUsed = types
                } catch (e: Exception) {
                    error = "Проверьте введённые числа: ${e.message}"
                    result = null
                }
            }) {
                Text("Рассчитать укладку")
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            result?.let { boxes ->
                val (W, D, H) = cellDims!!
                Spacer(Modifier.height(16.dp))
                Text(
                    "Влезает коробок всего: ${boxes.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))
                typesUsed.forEach { t ->
                    val count = boxes.count { it.typeId == t.id }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(colorForType(t.id), RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("${t.name} (${t.w}×${t.d}×${t.h}): $count шт")
                    }
                }

                Spacer(Modifier.height(8.dp))
                ProjectionCanvas(W, D, H, boxes, Projection.TOP, "Вид сверху (X-Y)")
                ProjectionCanvas(W, D, H, boxes, Projection.FRONT, "Вид спереди (X-Z)")
                ProjectionCanvas(W, D, H, boxes, Projection.SIDE, "Вид сбоку (Y-Z)")
            }
        }
    }
}

/** Компактное поле для чисел: короткая подпись, экономит место в узком ряду. */
@Composable
private fun RowScope.CompactField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f)
    )
}
