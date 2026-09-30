package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 味方キャラクターの陣形(縦[ROWS]マス×横[COLUMNS]マスの配置)を表すドメインモデル。
 *
 * 編成画面での配置操作と、バトル画面での配置表示の双方が同じ「どのマスに誰がいるか」を
 * 扱うため、マス目の大きさや配置のルール(最大人数、移動・入れ替えの挙動)をこの1箇所に
 * 集約することを目的とする。インスタンスは不変であり、配置操作は新しい[Formation]を返す。
 *
 * マスの番号(スロット番号)は左上を0として、行ごとに左から右へ0〜[SLOT_COUNT]-1を振る。
 *
 * @property characterIdsBySlot スロット番号ごとの配置キャラクターID。要素数は常に
 *   [SLOT_COUNT]であり、キャラクターが配置されていないマスは`null`となる。
 */
data class Formation(val characterIdsBySlot: List<Int?>) {

    init {
        require(characterIdsBySlot.size == SLOT_COUNT) {
            "characterIdsBySlot must have $SLOT_COUNT elements but was ${characterIdsBySlot.size}"
        }
    }

    /** 陣形に配置済みのキャラクターID一覧(スロット番号順)。 */
    val memberIds: List<Int>
        get() = characterIdsBySlot.filterNotNull()

    /**
     * 指定したキャラクターが配置されているスロット番号を返す。
     *
     * @param characterId 探すキャラクターのID。
     * @return 配置先のスロット番号。配置されていない場合は`null`。
     */
    fun slotOf(characterId: Int): Int? =
        characterIdsBySlot.indexOf(characterId).takeIf { it >= 0 }

    /**
     * 指定したキャラクターを指定したマスへ配置した新しい陣形を返す。
     *
     * - 既に別のマスにいるキャラクターの場合は移動となり、移動先に他のキャラクターがいれば
     *   移動元のマスと入れ替える。
     * - 未配置のキャラクターを埋まっているマスへ配置する場合、元々いたキャラクターは
     *   陣形から外れる(人数は変わらない)。
     * - 未配置のキャラクターを空きマスへ配置する場合で、既に[MAX_MEMBER_COUNT]人配置済みの
     *   ときは配置できず、元の陣形をそのまま返す。
     *
     * @param characterId 配置するキャラクターのID。
     * @param slot 配置先のスロット番号(0〜[SLOT_COUNT]-1)。
     * @return 配置後の陣形。配置できなかった場合は自分自身。
     */
    fun place(characterId: Int, slot: Int): Formation {
        require(slot in 0 until SLOT_COUNT) { "slot must be in 0 until $SLOT_COUNT but was $slot" }
        val fromSlot = slotOf(characterId)
        val occupant = characterIdsBySlot[slot]
        if (fromSlot == null && occupant == null && memberIds.size >= MAX_MEMBER_COUNT) return this

        val slots = characterIdsBySlot.toMutableList()
        if (fromSlot != null) slots[fromSlot] = occupant
        slots[slot] = characterId
        return Formation(slots)
    }

    /**
     * 指定したマスのキャラクターを陣形から外した新しい陣形を返す。
     *
     * @param slot 空けるマスのスロット番号(0〜[SLOT_COUNT]-1)。
     * @return 指定マスを空にした陣形。
     */
    fun removeAt(slot: Int): Formation {
        require(slot in 0 until SLOT_COUNT) { "slot must be in 0 until $SLOT_COUNT but was $slot" }
        return Formation(characterIdsBySlot.toMutableList().apply { set(slot, null) })
    }

    companion object {
        /** 陣形の行数(縦のマス数)。 */
        const val ROWS = 2

        /** 陣形の列数(横のマス数)。 */
        const val COLUMNS = 3

        /** 陣形の総マス数。 */
        const val SLOT_COUNT = ROWS * COLUMNS

        /** 陣形に配置できるキャラクターの最大人数。 */
        const val MAX_MEMBER_COUNT = 2

        /** 誰も配置されていない空の陣形。 */
        val EMPTY = Formation(List(SLOT_COUNT) { null })
    }
}
