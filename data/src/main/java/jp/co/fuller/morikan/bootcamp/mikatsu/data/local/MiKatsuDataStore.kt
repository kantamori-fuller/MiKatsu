package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** DataStoreが管理する設定ファイルの名前。 */
private const val PREFERENCES_NAME = "mikatsu_prefs"

/**
 * アプリ内の各種設定値(次に採番するキャラクターIDなど)を保存するDataStore。
 *
 * 同一ファイルに対して複数の[DataStore]インスタンスが生成されると実行時例外になるため、
 * `by preferencesDataStore(...)`によるプロパティデリゲートを[Context]の拡張として
 * 1箇所にのみ定義し、アプリ全体で同一インスタンスを共有できるようにすることを目的とする。
 */
val Context.mikatsuDataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)
