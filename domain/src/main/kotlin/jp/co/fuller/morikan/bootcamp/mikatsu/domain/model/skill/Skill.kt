package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルのマスターデータに共通のインターフェース。
 *
 * スキルIDはアクティブ・パッシブで独立して採番され、敵のEIDと同じく固定のマスターデータを
 * 識別する。同じ数値でも種別が違えば別のスキルであり、IDは必ず種別(A/P)とセットで意味を持つ。
 * 戦闘中の個体を識別するFPIDとは別物である。
 */
sealed interface Skill {

    /** 同じ種別(アクティブまたはパッシブ)の中でスキルを一意に識別するID(マスターデータのID)。 */
    val id: Int

    /** スキルの名前。 */
    val name: String

    /** スキルタイプ。1つのスキルが複数持てる。持たない場合は空。 */
    val categories: Set<SkillCategory>
}

/**
 * 戦闘中に自分の意思で使うアクティブスキル(Aスキル)。スキル一覧の「Aスキル」シート1行と1対1に対応するテンプレート。
 *
 * 各フィールドのKDocに、対応するシートの列名を記す。シートの1行をこのクラスに書き写す手順は
 * `SkillRepositoryImpl`のテンプレート(コメント)を参照。
 * 通常攻撃(Aスキル1)は全キャラクター・全敵が標準で持つ特別なスキルで、設定枠を使わない。
 *
 * @property id スキルID(列「ID」)。アクティブスキルの採番。Pスキルとは独立。
 * @property name スキルの名前(列「名前」)。
 * @property element スキルの属性(シートのタブ)。
 * @property categories スキルタイプ(列「スキルタイプ」)。複数持てる。
 * @property physicalAttribute 物理属性(列「物理属性」)。空欄は`null`。
 * @property costs コスト(列「コストタイプ」「コスト消費」)。「MP30&HP10」は2要素。コストなし(0)は空リスト。
 * @property coolTime クールタイム(列「CT」)。ターン数。「1t」なら1。現段階では値を持つだけで制限には使わない。
 * @property contactType 接触タイプ(列「接触」)。
 * @property range レンジ(列「レンジ」)。使用者から届く距離で、選べる相手を制限する。
 * @property area 範囲(列「範囲」)。対象選択と組み合わせて、選んだ相手から実際の対象を決める。
 * @property targetSelection 対象選択(列「対象選択」)。一体選択か、範囲内の全て対象か。
 * @property basePower 基礎威力(列「基礎威力」)。ダメージを与えないスキルは`null`。
 * @property hitCount 攻撃回数(列「攻撃回数」)。「2回」なら2。
 * @property accuracy 命中率(列「命中率」)。0〜100の整数。空欄(命中が関係しない)は`null`で、必中を表し命中判定をしない。
 * @property effects 効果(列「効果」)。「なし」は空リスト。
 * @property targetSide 対象にする陣営。シートにはまだない列(詳細は[SkillTargetSide])。
 */
data class ActiveSkill(
    override val id: Int,
    override val name: String,
    val element: SkillElement,
    override val categories: Set<SkillCategory>,
    val physicalAttribute: PhysicalAttribute?,
    val costs: List<SkillCost>,
    val coolTime: Int,
    val contactType: SkillContactType,
    val range: SkillRange,
    val area: SkillArea,
    val targetSelection: SkillTargetSelection,
    val basePower: Int?,
    val hitCount: Int = 1,
    val accuracy: Int?,
    val effects: List<ActiveSkillEffect>,
    val targetSide: SkillTargetSide,
) : Skill {

    /**
     * 指定した種類のコストの合計消費量を返す。
     *
     * @param type コストの種類。
     * @return その種類の消費量の合計。その種類のコストがなければ0。
     */
    fun costOf(type: SkillCostType): Int = costs.filter { it.type == type }.sumOf { it.amount }
}

/**
 * 設定しておくだけで常に効果を発揮するパッシブスキル(Pスキル)。スキル一覧の「Pスキル」シート1行と1対1に対応するテンプレート。
 *
 * @property id スキルID(列「ID」)。パッシブスキルの採番。Aスキルとは独立。
 * @property name スキルの名前(列「名前」)。
 * @property categories スキルタイプ(列「スキルタイプ」)。空もあり。
 * @property effects 常時発揮される効果(列「効果」)。
 */
data class PassiveSkill(
    override val id: Int,
    override val name: String,
    override val categories: Set<SkillCategory>,
    val effects: List<PassiveSkillEffect>,
) : Skill
