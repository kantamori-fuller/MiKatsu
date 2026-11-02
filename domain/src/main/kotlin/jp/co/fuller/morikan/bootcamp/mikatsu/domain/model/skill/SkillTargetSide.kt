package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルが対象にする陣営(敵か味方か)。
 *
 * スキル一覧にはまだこの列がないが、通常攻撃(敵を殴る)とディア(味方を回復する)で対象にする側が
 * 異なり、戦闘ロジックが対象の妥当性を判定するのに必要なため、シートが陣営を表現するようになる
 * までの間、別のフィールドとして持つ。
 */
enum class SkillTargetSide {
    /** 敵陣営の個体を対象にする。 */
    ENEMY,

    /** 自分を含む味方陣営の個体を対象にする。 */
    ALLY,
}
