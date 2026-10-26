# Forge 1.20.1 — porte e validação

Data: 14/10/2026. Versão preparada: `0.3.0`. Minecraft `1.20.1`, Forge `47.4.0`, Java `17.0.19`.

O porte adiciona o módulo `forge1201`, mantendo os três builds 1.21.1 existentes. O namespace continua `com.radaeli.hotbarlocker`.

## Cobertura funcional

Esta matriz cobre as funcionalidades existentes identificadas no código e na documentação. Não representa uma medição de 100% de linhas/branches, nem uma garantia de compatibilidade com todo mod que escreva diretamente no inventário.

| Funcionalidade | Implementação 1.20.1 | Verificação |
| --- | --- | --- |
| Trava individual dos 9 slots | Máscara e regras compartilhadas; keybind configurável inicialmente sem tecla | Input sintético pelo handler real em slot ocupado e vazio, pacote C2S, confirmação de estado |
| Menu de pausa | A tela de pausa vanilla continua aberta enquanto há um mundo carregado | Etapa adicionada ao smoke test; aguarda nova execução |
| Mensagens ao travar/destravar | Mesmas chaves `en_us` e `pt_br`; overlay acima da hotbar | Cliente valida o texto traduzido; JAR compara traduções byte a byte |
| Som ao travar/destravar | `EXPERIENCE_ORB_PICKUP`, volume `0.35`, pitches `0.65` e `1.25` | Cliente recebe os dois eventos, resolve o recurso e confere os pitches com OpenAL ativo |
| Moldura dourada | Mesmas cores, geometria e dimensões da versão 1.21.1, via `Gui.renderHotbar(float, GuiGraphics)` | Capturas reais inspecionadas: 1 trava, 9 travas e máscara alternada após reload |
| Seleção vanilla | Borda de seleção externa permanece distinguível | Inspeção das capturas |
| Pick Criativo com destino livre | Ignora travados, prefere vazio e faz busca circular | GameTests e middle-click real no cliente; checagem do servidor |
| Pick de item já na hotbar travada | Seleção continua permitida | GameTest |
| Hotbar toda travada | Pick Criativo usa slots 9–35, preserva seleção e hotbar | GameTest e pick real; confirmação do item no servidor |
| Inventário cheio | Aborta sem perder, duplicar ou sobrescrever stacks protegidas | GameTests com todos os slots ocupados e com slot vazio travado |
| Deslocamento de stack ocupada | Move stack para inventário principal e sincroniza todos os slots alterados | GameTest e comparação cliente/servidor após pick real |
| Pick de Sobrevivência | Swap em destino destravado; sem swap se todos travados | GameTests e pacote vanilla real |
| Limpeza no Criativo | Shift-lixeira ignora hotbar travada | Tela de inventário real e confirmação no servidor |
| Persistência por jogador | Mesmo campo NBT `HotbarLockerLocks` | Save/load GameTest, tag ausente, save/reload real |
| Login, dimensão e respawn | Eventos Forge enviam estado; clone preserva máscara | Cliente muda Overworld/Nether, morre/respawna e reabre o save |
| Uso e movimento manual | Sem bloqueio genérico de `Inventory.setItem` | GameTest de shrink, remoção manual e seleção |
| Create Toolbox | Adapter 1.20.1 para `NetworkEvent.Context.enqueueWork`, executado na thread do servidor | Matriz opcional com os pacotes reais do Create, descrita abaixo |
| Servidor dedicado | Bootstrap e transportes sem links a classes Minecraft client | GameTest server físico + inspeção do bytecode distribuível |

## Resultados e evidências

- `:forge1201:build`: JAR reobfuscado, quatro testes do artefato aprovados.
- `verifyForge1201`: 12 GameTests obrigatórios aprovados em servidor dedicado.
- `:forge1201:runClientSmoke`: 35 etapas concluídas no último build executado; essa rodada incluiu travar o slot 1 ocupado e save/reload. A nova checagem do menu de pausa está adicionada e aguarda uma execução após a revisão do patch.
O smoke test salva o resultado e as três capturas HUD em `forge1201/build/client-smoke/`; o Gradle mantém essas saídas locais e fora do repositório. O GameTest server registra seus resultados em `forge1201/build/`.

## Create Toolbox

As assinaturas e os campos foram conferidos nos JARs oficiais de Create `0.5.1.j` e `6.0.8` para Forge 1.20.1. Ambos usam `handle(NetworkEvent.Context)` e campos mutáveis, diferentes do adapter 1.21.1.

A suíte opcional adiciona oito cenários ao servidor GameTest: equip normal, redirecionamento de slot travado, wrap 8→0, nove travas, fallback ocupado, bloqueio de unequip travado, unequip normal e rejeição pelo alcance nativo. Ela instancia o pacote real e inspeciona o NBT real `CreateToolboxData`.

O servidor GameTest aprovou 20 cenários com Create `6.0.8`. A execução planejada para `0.5.1.j` não chegou a iniciar porque a revisão automática de execução atingiu o limite de uso; essa versão segue pendente.

## Empacotamento

O artefato instalável a gerar é `forge1201/build/libs/hotbarlocker-forge-1.20.1-0.3.0.jar`. A validação anterior do artefato `0.2.0` confirmou:

- bytecode Java 17 em todas as classes;
- Minecraft exato `1.20.1`, Forge mínimo `47.4.0` e limite anterior a Forge 48;
- `MixinConfigs` no manifesto, config `JAVA_17` e refmap com todos os alvos vanilla;
- remapeamento do campo `@Shadow` da lixeira Criativa;
- resource pack format `15`, traduções compartilhadas e ausência de código de validação no JAR.

## Limites da evidência

A inspeção visual usa screenshots do cliente real; o teste de som verifica recurso, evento e pitch, sem avaliação auditiva humana. Uso/dano/scroll e menus de mods adicionais ainda podem receber a checagem humana do [checklist manual](phase-1-manual-checklist.md). O servidor GameTest prova execução no lado físico dedicado; não equivale a uma sessão multiplayer com duas instalações independentes.

A tentativa extra de iniciar o runtime instalado de produção carregou o manifesto e o refmap, mas parou na EULA antes do bootstrap do mod. Nenhuma aceitação de EULA foi escrita. Esse lançamento não é contado como prova de inicialização completa do servidor de produção.

O alcance de compatibilidade continua o definido no PRD: fluxos vanilla e integração explícita Create; escritas arbitrárias por outros mods exigem uso da API ou integração própria.

## Reproduzir

Consulte [os comandos de build e testes](development.md). Java 17, SimpleChannel e os lados físicos foram conferidos com a documentação oficial de [ambiente Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/) e [rede SimpleImpl 1.20.1](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/).
