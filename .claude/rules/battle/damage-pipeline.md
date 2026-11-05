---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# ダメージ技の処理順(`performAttack`)

ダメージ技(基礎威力のあるスキル・通常攻撃)の1ヒットの処理順を1か所にまとめた正本。各段階の詳細は括弧内のファイルにあるが、**順序はこのファイルが正**。新しい補正を追加したら、ここの順序に組み込んで更新する。

## 全体の流れ

1. **コスト消費**(味方のみ。技の最初に払い、外れても戻さない。`skills.md`・`tp.md`)。CTを設定(`skills.md`)。表示用スナップショット`AttackResult.fieldBeforeShots`はこの直後(`narration.md`)。
2. **ヒットのループ**: 回数優先(1回目を`resolveTargets`順の全対象 → 2回目を全対象 …)。戦闘不能になった対象は以降打ち切り、反射で攻撃者が戦闘不能なら技の残りを全て打ち切り。ヒットごとにフィールドへ反映する(多段の途中でブレイク等した効果は残りのヒットからすぐに効く)。各ヒットの反映直後が`AttackShot.fieldAfter`。
3. **TPの精算**(`settleTp`。技の最後にまとめて加算。`tp.md`)。最終が`AttackResult.fieldAfter`。

## 1ヒットの処理

1. **命中判定**(`hit-chance.md`): 外れたらダメージ0で、以降(クリティカル判定・軽減など)は何もしない。発動回数は1回消費する。必中(スキルの命中率`null`・対象がブレイク中)なら乱数を引かない。
2. **クリティカル判定**(`critical.md`): 命中したヒットだけ`random.nextInt(24)`を1回引く。睡眠中の対象へのヒットは乱数を引かずにクリティカル判定(`status-ailments.md`)。防御中なら最終種別`CRITICAL`を無効化(`defense.md`)。
3. **ダメージ計算**(下の表の順に適用)
4. **反映**: HPを減らす(吸収なら回復)。実際に減ったHP(`min(ダメージ, ヒット前の現在HP)`)でブレイクゲージを貯める(`break.md`)。HPが実際に減ったら睡眠・凍結を解除(`status-ailments.md`)。戦闘不能なら状態異常を全解除。

## ダメージ計算の順序

| # | 段階 | 計算 | 詳細 |
|---|---|---|---|
| 1 | `DamageFormula` | +2の後の結果。破壊力→耐久力(`effectiveStat`。凍傷の×4/6・麻痺は`adjustStat`込み)、特殊型攻撃ならマナ出力→マナ耐性 | `skills.md`(特殊型攻撃)・`stat-modifiers.md` |
| 2 | クリティカル/バッドクリティカル | `max(floor(ダメージ × 倍率 / 100), 1)` | `critical.md` |
| 3 | ブレイク中 | ×1.3 切り捨て(`boostDamage`) | `break.md` |
| 4 | 凍結 | ×2(解けたそのヒットだけ) | `status-ailments.md` |
| 5 | 流血 | ×1.2 切り捨て | `status-ailments.md` |
| 6 | 防御中 | `max(floor(ダメージ × 70 / 100), 1)`(`DefenseFormula.reduceDamage`) | `defense.md` |
| 7 | 属性耐性 | 倍率: `floor(ダメージ × 耐性 / 100)`、0なら1。無効: 0で以降も通さない。吸収: 0にして回復。反射: 0にして攻撃者へ | `element-resistance.md` |
| 8 | Pスキルの軽減 | `reduceDamage`。軽減が1以上なら`max(ダメージ - 軽減合計, 1)` | `skills.md` |
| 9 | 石化 | 常に1(無効・吸収・反射も起きない) | `status-ailments.md` |

- **反射の再計算**も同じ順序を、攻撃者が自分自身を攻撃したものとして適用する(新たな命中判定なし。詳細は`element-resistance.md`)。
- **HPコスト・TP代替のHP・状態異常のダメージ**はこの順序を通らず、ブレイクゲージもTP条件2も貯めない。
