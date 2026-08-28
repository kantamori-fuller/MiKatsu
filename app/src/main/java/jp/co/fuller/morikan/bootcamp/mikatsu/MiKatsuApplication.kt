package jp.co.fuller.morikan.bootcamp.mikatsu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリ全体のHilt依存関係グラフのルートとなる[Application]。
 *
 * `@HiltAndroidApp`を付与することで、アプリ起動時にHiltのコンポーネント階層
 * (SingletonComponentなど)を生成し、[MainActivity]をはじめとする各コンポーネントが
 * `@Inject`/`hiltViewModel()`による依存性注入を受けられるようにすることを目的とする。
 */
@HiltAndroidApp
class MiKatsuApplication : Application()
