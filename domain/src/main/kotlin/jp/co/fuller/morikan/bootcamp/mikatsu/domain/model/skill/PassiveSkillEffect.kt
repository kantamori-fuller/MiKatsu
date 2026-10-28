package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * パッシブスキルが常時発揮している効果。
 *
 * 効果の種類をデータとして表し、効果の適用処理を戦闘ロジック側へ集約することを目的とする。
 */
sealed interface PassiveSkillEffect {

    /**
     * 自身が受けるダメージを固定値だけ減らす。
     *
     * @property amount 減らすダメージ量。複数持つ場合は合計して適用する。
     */
    data class DamageReduction(val amount: Int) : PassiveSkillEffect
}
