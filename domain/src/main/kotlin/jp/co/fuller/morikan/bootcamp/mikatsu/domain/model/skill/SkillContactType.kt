package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの接触タイプ。スキルが相手に直接触れて効果を及ぼすかどうかを表す。
 *
 * 将来、接触の有無に応じた耐性や反撃などの判定を、スキルごとに個別実装せず共通の属性として
 * 扱えるようにすることを目的とする。
 */
enum class SkillContactType {
    /** 接触。相手に直接触れて効果を及ぼす。 */
    CONTACT,

    /** 非接触。相手に触れずに効果を及ぼす。 */
    NON_CONTACT,
}
