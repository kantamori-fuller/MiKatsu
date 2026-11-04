package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement

/**
 * 属性耐性の値の分類と、耐性によるダメージの計算を集約した、属性耐性の唯一の実装箇所。
 *
 * 耐性値は-999〜999の整数で、既定は等倍の100。値の意味は次のとおり([classify])。
 * - 1〜999: 受けるダメージの倍率(%)。
 * - 0、および-1・-999以外の負数: 無効。
 * - -1: 反射。
 * - -999: 吸収。
 */
object ElementResistanceFormula {

    /** 等倍(耐性の既定値)。 */
    const val NEUTRAL = 100

    /** 反射を表す耐性値。 */
    const val REFLECT = -1

    /** 吸収を表す耐性値。 */
    const val ABSORB = -999

    /** 耐性値の下限。 */
    const val MIN = -999

    /** 耐性値の上限。 */
    const val MAX = 999

    /** 倍率として扱うとき、ダメージが0になっても引き上げる最小のダメージ。 */
    private const val MIN_DAMAGE = 1

    /**
     * 耐性値の分類。
     */
    enum class Kind {
        /** 1〜999。ダメージを耐性値の倍率(%)にする。 */
        MULTIPLIER,

        /** 0、および-1・-999以外の負数。ダメージ0で、効果もかからない。 */
        NULLIFY,

        /** -1。攻撃・効果を使用者へ跳ね返す。 */
        REFLECT,

        /** -999。攻撃のダメージ分のHPを回復する。 */
        ABSORB,
    }

    /**
     * 耐性値を[Kind]に分類する。
     *
     * @param value 耐性値。
     * @return 1以上は[Kind.MULTIPLIER]、-1は[Kind.REFLECT]、-999は[Kind.ABSORB]、それ以外(0・その他の負数)は[Kind.NULLIFY]。
     */
    fun classify(value: Int): Kind = when {
        value >= 1 -> Kind.MULTIPLIER
        value == REFLECT -> Kind.REFLECT
        value == ABSORB -> Kind.ABSORB
        else -> Kind.NULLIFY
    }

    /**
     * 倍率の耐性をダメージに適用する。`floor(ダメージ × 耐性 / 100)`で、結果が0なら1にする。
     *
     * @param damage 耐性を適用する前のダメージ。
     * @param value 倍率の耐性値(1〜999)。
     * @return 適用後のダメージ(最低1)。
     */
    fun scaleDamage(damage: Int, value: Int): Int =
        (damage.toLong() * value / 100).toInt().coerceAtLeast(MIN_DAMAGE)

    /**
     * 耐性値を入力可能な範囲(-999〜999)に収める。
     *
     * @param value 収める前の値。
     * @return 範囲内に収めた値。
     */
    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)

    /**
     * 属性耐性がスキルの効果(デバフ・ネガティブエフェクト)に影響するスキルか。
     *
     * 回復・バフ・ポジティブエフェクトなど、デバフでもネガティブエフェクトでもないスキルには耐性は影響しない。
     *
     * @param skill 調べるスキル。
     * @return スキルタイプにデバフまたはネガティブエフェクトを含めば`true`。
     */
    fun affectsEffects(skill: ActiveSkill): Boolean =
        SkillCategory.DEBUFF in skill.categories || SkillCategory.NEGATIVE_EFFECT in skill.categories
}

/**
 * この個体の、指定した属性に対する耐性値を返す。
 *
 * @receiver 耐性を調べる個体。
 * @param element 技の属性。無・龍は常に等倍。
 * @return 耐性値。
 */
fun BattleFieldPawn.resistanceTo(element: SkillElement): Int = elementResistances.of(element)

/**
 * この個体の、指定した属性に対する耐性値の分類を返す。
 *
 * @receiver 耐性を調べる個体。
 * @param element 技の属性。
 * @return 耐性値の分類。
 */
fun BattleFieldPawn.resistanceKindTo(element: SkillElement): ElementResistanceFormula.Kind =
    ElementResistanceFormula.classify(resistanceTo(element))
