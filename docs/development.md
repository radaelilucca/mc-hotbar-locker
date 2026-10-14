# Desenvolvimento

## Módulos e versões

| Módulo | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `common` | regras compartilhadas | independente | 21 para testes standalone |
| `fabric` | 1.21.1 | Fabric | 21 |
| `forge` | 1.21.1 | Forge 52 | 21 |
| `neoforge` | 1.21.1 | NeoForge 21.1 | 21 |
| `forge1201` | 1.20.1 | Forge 47.4.0+ (47.x) | 17 |

O módulo `forge1201` compila o código compartilhado de regras, API, persistência e limpeza no Criativo. Ele fornece adapters próprios de rede, ciclo de vida, HUD, sincronização dos slots de pick Criativo e Create Toolbox. Não altere `minecraft_version=1.21.1` global para produzir o JAR 1.20.1.

Execute Gradle com Java 21 e mantenha um JDK 17 disponível para a toolchain do módulo 1.20.1. Se a descoberta automática não encontrar ambos, passe `-Porg.gradle.java.installations.paths="<JDK17>,<JDK21>"`. O bytecode do artefato 1.20.1 é Java 17.

## Comandos

No Windows use `gradlew.bat`; Linux usa `./gradlew`.

```powershell
# JAR Forge 1.20.1 + verificações do artefato
.\gradlew.bat :forge1201:build --no-daemon --no-parallel --console=plain

# JAR + testes no servidor dedicado GameTest
.\gradlew.bat verifyForge1201 --no-daemon --no-parallel --console=plain

# Cliente automatizado: cria mundo isolado, verifica rede/input/som e salva screenshots
.\gradlew.bat :forge1201:runClientSmoke --no-daemon --no-parallel --console=plain

# Testes com o pacote REAL do Create (dependência somente do ambiente de validação)
.\gradlew.bat :forge1201:runGameTestServer '-PcreateValidationVersion=mc1.20.1-6.0.8' --no-daemon --no-parallel --console=plain
.\gradlew.bat :forge1201:runGameTestServer '-PcreateValidationVersion=1.20.1-0.5.1.j' --no-daemon --no-parallel --console=plain

# Regressão de todos os alvos
.\gradlew.bat verifyScaffold --no-daemon --no-parallel --console=plain

# Cliente interativo Forge 1.20.1
.\gradlew.bat :forge1201:runClient --no-daemon --no-parallel
```

Use um único Gradle por cache. No ambiente local, `-g .gradle-hotbarlocker` reaproveita o cache deste projeto. Os diretórios de execução são próprios de cada módulo.

O cliente automatizado publica temporariamente seu mundo isolado em uma porta escolhida pelo sistema para impedir pausas do servidor integrado durante a execução em segundo plano. Fecha o jogo e o servidor ao terminar. Não abre nem altera saves de uso normal.

## Artefato e evidências

O JAR distribuível fica em `forge1201/build/libs/hotbarlocker-forge-1.20.1-<versão>.jar`. O arquivo `-sources.jar` contém fontes e não deve ser instalado como mod.

- Testes do JAR: `forge1201/build/reports/tests/test/index.html`.
- Servidor GameTest: `forge1201/build/gametest/logs/latest.log`.
- Cliente: `forge1201/build/client-smoke/result.txt`, `logs/latest.log` e `screenshots/`.

Os source sets `gameTest` e `test` não entram no JAR distribuível. O Mixin AP gera refmap e mappings extras de campos `@Shadow`; ForgeGradle reobfusca o JAR para instalação normal.

## CI

Pull requests executam `verifyScaffold` para os quatro artefatos, incluindo os testes Forge 1.20.1. O workflow instala Java 17 e 21 explicitamente. O teste gráfico do cliente é separado porque exige OpenGL e áudio.

O workflow de tags coleta os três artefatos 1.21.1 existentes e o quarto JAR `hotbarlocker-<versão>-forge-1.20.1.jar`. Nenhuma tag ou publicação é criada pelo build local.
