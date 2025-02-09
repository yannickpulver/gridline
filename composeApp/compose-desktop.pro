-keep class org.cef.** { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory
-keep class com.yannickpulver.gridline.domain.model.** { *; }
-keep class com.arkivanov.decompose.extensions.compose.jetbrains.mainthread.SwingMainThreadChecker
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
   volatile <fields>;
}
-ignorewarnings
-dontobfuscate