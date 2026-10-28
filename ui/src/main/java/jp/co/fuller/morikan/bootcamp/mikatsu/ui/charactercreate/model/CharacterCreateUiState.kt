package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillLimits

/**
 * スキル設定欄に表示する、スキル1件分のUIモデル。
 *
 * @property id スキルID(マスターデータのID)。種別(A/P)ごとに独立した採番なので、同じ数値でも種別が違えば別のスキル。
 *   使う場面(Aスキル欄・Pスキル欄・追加ダイアログ)の種別とセットで扱う。
 * @property name スキルの名前。
 */
data class SkillUiModel(
    val id: Int,
    val name: String,
)

/**
 * スキル追加ダイアログの対象となる、スキルの種別。
 */
enum class SkillPickerKind {
    /** アクティブスキル(Aスキル)。 */
    ACTIVE,

    /** パッシブスキル(Pスキル)。 */
    PASSIVE,
}

/**
 * キャラ作成/編集画面(CharacterCreateScreen)の画面状態を表すUiState。
 *
 * 各ステータスは入力フィールドの表示文字列として保持し、保存時に整数へ変換する。
 *
 * @property isEditing 既存キャラクターの編集かどうか。`false`の場合は新規作成。
 *   画面タイトルの出し分けに使用する。
 * @property name 名前フィールドの入力値。
 * @property hp H. 体力フィールドの入力値。
 * @property mana M. マナ総量フィールドの入力値。
 * @property attack A. 破壊力フィールドの入力値。
 * @property defense B. 耐久力フィールドの入力値。
 * @property manaOutput C. マナ出力フィールドの入力値。
 * @property manaResistance D. マナ耐性フィールドの入力値。
 * @property agility S. 敏捷フィールドの入力値。
 * @property luck L. 幸運フィールドの入力値。
 * @property activeSkillIds 設定済みのアクティブスキル(Aスキル)のスキルID。設定順に並ぶ。
 * @property passiveSkillIds 設定済みのパッシブスキル(Pスキル)のスキルID。設定順に並ぶ。
 * @property activeSkillCatalog 設定可能なアクティブスキルの一覧(マスターデータ)。
 * @property passiveSkillCatalog 設定可能なパッシブスキルの一覧(マスターデータ)。
 * @property isSkillCatalogLoaded スキルのマスターデータ([activeSkillCatalog]・[passiveSkillCatalog])の読み込みが完了したかどうか。
 *   未完了の間は、設定済みスキルIDがマスターデータに存在するか判定できない。
 * @property skillPicker スキル追加ダイアログを表示している場合はその種別。表示していなければ`null`。
 * @property isSaved 保存処理が完了したかどうか。`true`になったタイミングで
 *   Screen側が前の画面へ戻る。
 * @property isSaveEnabled 「保存」ボタンを押せる状態かどうか。[name]が空文字
 *   (または空白のみ)の場合は保存不可となる。
 */
data class CharacterCreateUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val hp: String = "",
    val mana: String = "",
    val attack: String = "",
    val defense: String = "",
    val manaOutput: String = "",
    val manaResistance: String = "",
    val agility: String = "",
    val luck: String = "",
    val activeSkillIds: List<Int> = emptyList(),
    val passiveSkillIds: List<Int> = emptyList(),
    val activeSkillCatalog: List<SkillUiModel> = emptyList(),
    val passiveSkillCatalog: List<SkillUiModel> = emptyList(),
    val isSkillCatalogLoaded: Boolean = false,
    val skillPicker: SkillPickerKind? = null,
    val isSaved: Boolean = false,
) {
    /** 設定済みのアクティブスキル。マスターデータに存在しないIDは表示しない。 */
    val activeSkills: List<SkillUiModel>
        get() = activeSkillIds.mapNotNull { id -> activeSkillCatalog.firstOrNull { it.id == id } }

    /** 設定済みのパッシブスキル。マスターデータに存在しないIDは表示しない。 */
    val passiveSkills: List<SkillUiModel>
        get() = passiveSkillIds.mapNotNull { id -> passiveSkillCatalog.firstOrNull { it.id == id } }

    /**
     * アクティブスキルをさらに追加できるか。
     *
     * マスターデータに存在する設定済みスキル数([activeSkills])が上限に達しておらず、
     * かつ未設定のスキルが1つ以上残っているときだけ`true`。存在しないIDは枠を消費しない。
     */
    val canAddActiveSkill: Boolean
        get() = activeSkills.size < SkillLimits.MAX_ACTIVE_SKILLS &&
            activeSkillCatalog.any { it.id !in activeSkillIds }

    /**
     * パッシブスキルをさらに追加できるか。
     *
     * マスターデータに存在する設定済みスキル数([passiveSkills])が上限に達しておらず、
     * かつ未設定のスキルが1つ以上残っているときだけ`true`。存在しないIDは枠を消費しない。
     */
    val canAddPassiveSkill: Boolean
        get() = passiveSkills.size < SkillLimits.MAX_PASSIVE_SKILLS &&
            passiveSkillCatalog.any { it.id !in passiveSkillIds }

    /** スキル追加ダイアログに表示する、まだ設定していないスキル。ダイアログを表示していなければ空。 */
    val skillPickerCandidates: List<SkillUiModel>
        get() = when (skillPicker) {
            SkillPickerKind.ACTIVE -> activeSkillCatalog.filter { it.id !in activeSkillIds }
            SkillPickerKind.PASSIVE -> passiveSkillCatalog.filter { it.id !in passiveSkillIds }
            null -> emptyList()
        }

    /**
     * 「保存」ボタンを押せる状態かどうか。
     *
     * 名前が空文字(または空白のみ)の場合は保存不可とする。
     */
    val isSaveEnabled: Boolean
        get() = name.isNotBlank()
}
