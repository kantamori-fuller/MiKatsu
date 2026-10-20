package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * バトルフィールドに配置された、味方の1個体を表すドメインモデル。
 *
 * [BattleEnemy]と対になるモデルであり、[Character]自身のID(端末内での永続的な識別子)とは
 * 別に、その戦闘限定で一意な[fpid](フィールドポーンID)を割り当てることで、敵味方を問わず
 * 同じ仕組みでフィールド上の対象を指定できるようにすることを目的とする。
 *
 * @property fpid その戦闘限定で一意な、フィールド上の個体を識別するID。敵([BattleEnemy])を
 *   含めたフィールド全体で重複しない。
 * @property character 個体の元になった味方キャラクターのデータ。
 * @property currentHp 現在のHP。戦闘開始時は最大HPと同じ。
 * @property slot 陣形([Formation])上で配置されているマスのスロット番号。
 */
data class BattleAlly(
    override val fpid: Int,
    val character: Character,
    override val currentHp: Int,
    val slot: Int,
) : BattleFieldPawn {
    override val maxHp: Int get() = character.hp
    override val attack: Int get() = character.attack
    override val defense: Int get() = character.defense
}
