package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 状態異常耐性([AilmentResistances])の項目。宣言順が扱い・表示・保存の順である。
 *
 * 複数の[StatusAilment]が1つの項目を共有する(スキル封印・精神系)。
 * 対応は[of]が唯一の定義で、耐性を持たない状態異常(スタン・烙印)は`null`になる。
 * 名前(文言)はドメインに持たず、ui側が対応づける。
 */
enum class AilmentResistanceType {
    /** 毒。 */
    POISON,

    /** 火傷。 */
    BURN,

    /** 凍傷。 */
    FROSTBITE,

    /** 凍結。 */
    FREEZE,

    /** 感染(伝染の判定にも使う)。 */
    INFECTION,

    /** 流血。 */
    BLEED,

    /** 麻痺。 */
    PARALYSIS,

    /** 感電。 */
    SHOCK,

    /** 即死([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect.InstantDeath])。 */
    INSTANT_DEATH,

    /** 石化。 */
    PETRIFY,

    /** 汚染。 */
    CONTAMINATION,

    /** 呪怨。 */
    CURSE,

    /** 睡眠。 */
    SLEEP,

    /** スキル封印(MP封印・TP封印で共有)。 */
    SKILL_SEAL,

    /** 目眩。 */
    DIZZINESS,

    /** 精神系(洗脳・発狂・魅了・恐怖・挑発で共有)。 */
    MENTAL;

    companion object {
        /**
         * 状態異常に対応する耐性の項目を返す。成功判定が状態異常の種類ごとにどの耐性を見るかを1か所で決めるため。
         *
         * @param ailment 調べる状態異常。
         * @return 対応する項目。耐性を持たないスタン・烙印は`null`。
         */
        fun of(ailment: StatusAilment): AilmentResistanceType? = when (ailment) {
            StatusAilment.STUN -> null
            StatusAilment.POISON -> POISON
            StatusAilment.BURN -> BURN
            StatusAilment.FROSTBITE -> FROSTBITE
            StatusAilment.FREEZE -> FREEZE
            StatusAilment.INFECTION -> INFECTION
            StatusAilment.BLEED -> BLEED
            StatusAilment.PARALYSIS -> PARALYSIS
            StatusAilment.SHOCK -> SHOCK
            StatusAilment.PETRIFY -> PETRIFY
            StatusAilment.CONTAMINATION -> CONTAMINATION
            StatusAilment.CURSE -> CURSE
            StatusAilment.SLEEP -> SLEEP
            StatusAilment.MP_SEAL, StatusAilment.TP_SEAL -> SKILL_SEAL
            StatusAilment.DIZZINESS -> DIZZINESS
            StatusAilment.BRAND -> null
            StatusAilment.BRAINWASH, StatusAilment.MADNESS, StatusAilment.CHARM,
            StatusAilment.FEAR, StatusAilment.TAUNT -> MENTAL
        }
    }
}
