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
     * 全アクティブスキルのマスターデータを取得する。
     *
     * @return アクティブスキルの一覧。
     */
    suspend fun getActiveSkills(): List<ActiveSkill>

    /**
     * 全パッシブスキルのマスターデータを取得する。
     *
     * @return パッシブスキルの一覧。
     */
    suspend fun getPassiveSkills(): List<PassiveSkill>

    /**
     * スキルIDからアクティブスキルを取得する。
     *
     * @param id スキルID。
     * @return 該当するアクティブスキル。存在しない(またはパッシブスキルのIDである)場合は`null`。
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
