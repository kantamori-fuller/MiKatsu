---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# バトルのID体系(ID / EID / FPID)

バトル関連の実装には目的の異なる3種類のIDが登場する。バトルロジック(ダメージ計算・対象指定など)を実装する際は、これらを混同しないこと。スキルIDは`skills.md`。

| ID | 実体 | 指すもの | 寿命 |
|---|---|---|---|
| **ID(キャラクターID)** | `Character.id` | 端末内に保存された味方キャラクター自身 | 永続。保存時に一度だけ採番され、削除されても再利用されない(`CharacterRepository`/`CharacterLocalDataSource`が管理) |
| **EID(Enemy ID)** | `Enemy.id` | 敵の「種族(マスターデータ)」。`EnemyRepositoryImpl`の固定データに紐づく(現状: ゴブリン=EID1、オーク=EID2) | 固定。同じEIDの敵がフィールド上に複数体登場し得るため、EIDだけでは個体を区別できない |
| **FPID(Field Pawn ID)** | `BattleFieldPawn.fpid` | ある1回の戦闘でフィールド上に存在する個体(味方・敵の両方) | その戦闘限り。永続化しない |

FPIDは`GetBattleFieldUseCase`が味方→敵の順に単一のカウンターで採番し、味方と敵の間、および同じ敵種同士でも重複しない。

## 使い分けの指針

- キャラクターの永続データを扱う(一覧表示・編集・保存・削除など) → `Character.id`
- 敵の種族・ステータスのマスターデータを扱う → `Enemy.id`(EID)
- バトル中に「誰が」「誰に」効果を及ぼすかを扱う(攻撃対象の指定、ダメージの適用先、状態異常の対象など) → **必ずFPIDを使う**。`Character.id`やEIDで対象を指定すると、同じキャラクター・同じ敵種がフィールド上に複数体登場した際に対象を一意に特定できず、意図しない相手に効果が及ぶ不具合の原因になる。

## 実装上の構造

- `BattleFieldPawn`: `fpid`プロパティを持つ、味方・敵共通のsealed interface。
- `BattleAlly(fpid, character)` / `BattleEnemy(fpid, enemy)`: それぞれ味方・敵の1個体を表すモデル。
- `BattleField(allies, enemies)`: 味方陣営・敵陣営をまとめた集約。`allPawns`で陣営を問わず全個体を横断的に扱える。
- `GetBattleFieldUseCase`: FPIDを割り当てて`BattleField`を組み立てる、採番ロジックの唯一の実装箇所。採番方法(採番順・敵の出現数など)を変える場合はここだけを変更すればよい。
- `BattleUnitUiModel.id`(UI層): 表示上のIDとして常にFPIDを使用する(`Character.id`やEIDをそのまま転用しない)。

例: ゴブリンx2・オークx2・味方2名の場合 → 味方がFPID=1,2、ゴブリンがFPID=3,4、オークがFPID=5,6となる(採番順は味方が先、敵は`EnemyRepositoryImpl`の登録順)。
