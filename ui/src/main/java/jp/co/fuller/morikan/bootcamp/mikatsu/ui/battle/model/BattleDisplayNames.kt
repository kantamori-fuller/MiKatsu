package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

/** アルファベットの種類数(A〜Z)。 */
private const val ALPHABET_SIZE = 26

/**
 * フィールド上の個体の表示名を決める。
 *
 * 同じ名前の個体が2体以上いるときだけ、敵味方を区別せずFPIDの小さい順に、名前の末尾へ
 * 大文字アルファベット(A, B, C…)を付ける。同名の個体をUI上で区別できるようにすることを目的とする。
 * 名前が1体しかない個体にはアルファベットを付けない。
 *
 * @param pawns 個体のFPIDと名前の組の一覧。
 * @return FPIDから表示名への対応表。
 */
fun resolveDisplayNames(pawns: List<Pair<Int, String>>): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    pawns.groupBy({ it.second }, { it.first }).forEach { (name, fpids) ->
        if (fpids.size < 2) {
            result[fpids.single()] = name
        } else {
            fpids.sorted().forEachIndexed { index, fpid -> result[fpid] = name + alphabetLabel(index) }
        }
    }
    return result
}

/**
 * 0始まりの連番を、A, B, …, Z, AA, AB, … の形のラベルに変換する。
 *
 * @param index 0始まりの連番。
 * @return 対応するアルファベットのラベル。
 */
private fun alphabetLabel(index: Int): String {
    val head = if (index >= ALPHABET_SIZE) alphabetLabel(index / ALPHABET_SIZE - 1) else ""
    return head + ('A' + index % ALPHABET_SIZE)
}
