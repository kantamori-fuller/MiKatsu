package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill

/**
 * スキルのマスターデータへのアクセスを抽象化するRepositoryインターフェース。
 *
 * スキルIDはアクティブ・パッシブで独立して採番され、IDだけでは種別をまたいでスキルを特定できない。
 * そのため取得は種別ごとのメソッドに分けている。ドメイン層・UI層は
 * このインターフェースにのみ依存し、スキルデータの出所の詳細を知らない(依存性逆転の原則)。
 * 実装は`data`モジュールが提供する。
 */
interface SkillRepository {

    /**
     * 全キャラクター・全敵が標準で持つ通常攻撃(Aスキル1)を取得する。
     *
     * 通常攻撃は設定枠を使わず、[getActiveSkills]の一覧にも[getActiveSkill]の検索結果にも含まれない。
     * 「攻撃」コマンドと敵の手番の攻撃が、威力や攻撃回数をこのデータから読むために使う。
     *
     * @return 通常攻撃のアクティブスキル。
     */
    suspend fun getNormalAttack(): ActiveSkill

    /**
     * キャラクターに設定できるアクティブスキルのマスターデータを取得する。通常攻撃は含まれない。
     *
     * @return 設定可能なアクティブスキルの一覧。
     */
    suspend fun getActiveSkills(): List<ActiveSkill>

    /**
     * 全パッシブスキルのマスターデータを取得する。
     *
     * @return パッシブスキルの一覧。
     */
    suspend fun getPassiveSkills(): List<PassiveSkill>

    /**
     * キャラクターが設定したアクティブスキルのIDを解決する。通常攻撃(Aスキル1)は返さない。
     *
     * 旧データでAスキル1がディアを指していた場合に、通常攻撃として解決されてしまわないようにするためである。
     *
     * @param id アクティブスキルのID。
     * @return 該当するアクティブスキル。存在しない・通常攻撃のIDである場合は`null`。
     */
    suspend fun getActiveSkill(id: Int): ActiveSkill?

    /**
     * スキルIDからパッシブスキルを取得する。
     *
     * @param id スキルID。
     * @return 該当するパッシブスキル。存在しない(またはアクティブスキルのIDである)場合は`null`。
     */
    suspend fun getPassiveSkill(id: Int): PassiveSkill?
}
