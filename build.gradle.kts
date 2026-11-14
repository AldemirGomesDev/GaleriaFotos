plugins {
    // Declarados aqui (apply false) para que possam ser referenciados via alias
    // em cada subprojeto sem carregar o plugin múltiplas vezes.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
