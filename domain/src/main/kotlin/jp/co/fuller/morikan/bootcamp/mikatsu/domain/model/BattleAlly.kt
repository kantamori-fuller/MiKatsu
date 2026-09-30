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
 */
data class BattleAlly(
    override val fpid: Int,
    val character: Character,
) : BattleFieldPawn
