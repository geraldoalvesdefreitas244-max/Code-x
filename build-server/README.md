# Code X — Servidor de Compilação Gradle (APK Real)

Este servidor executa o **Gradle** e o **Android SDK** reais para compilar os projetos criados no aplicativo **Code X** em arquivos `.apk` instaláveis (Debug e Release).

## Pré-requisitos
1. **Node.js 18+**
2. **JDK 17+** configurado em `JAVA_HOME`
3. **Android SDK** configurado em `ANDROID_SDK_ROOT` (com `build-tools` e `platforms;android-35`)
4. **Gradle 8.7+** no `PATH` (ou definido via `GRADLE_BIN`)

## Instalação e Execução
```bash
cd build-server
npm install
npm start
```

O servidor iniciará por padrão na porta `8787` (`http://0.0.0.0:8787`).

## Conectando o Aplicativo Code X
- **Emulador Android:** use `http://10.0.2.2:8787` em **Configurações de API / Servidor de Build** dentro do Code X.
- **Celular físico na mesma rede Wi-Fi:** use `http://<IP_DO_SEU_COMPUTADOR>:8787`.
