# Hotbar Locker — Discovery e PRD

Status: discovery concluído; pronto para validação do escopo antes da implementação.

Versão de referência: Minecraft 1.21.1, Java 21.

Namespace planejado: `com.radaeli.hotbarlocker`.

## 1. Resumo

Hotbar Locker permite travar individualmente os nove slots da hotbar. Um slot travado conserva seu conteúdo contra substituições automáticas de ações de pick/replace, enquanto o jogo continua podendo selecionar o slot e o jogador continua podendo usar o item normalmente.

Quando uma ação de pick precisa colocar um item na hotbar, o mod procura o próximo slot elegível na ordem circular da hotbar. Se todos estiverem travados, o item deve ser inserido no inventário principal; se não houver espaço, o comportamento final será o fallback vanilla seguro, normalmente deixar o item no fluxo de drop sem alterar slots travados.

## 2. Objetivos

### Must-have do MVP

- Travar e destravar cada slot da hotbar individualmente.
- Persistir o estado de travas por jogador e por mundo/servidor.
- Sincronizar o estado servidor → cliente ao entrar, trocar de dimensão, respawnar e reconectar.
- Impedir a substituição automática dos slots travados no pick block/item vanilla.
- Redirecionar a colocação para o próximo slot desbloqueado.
- Colocar no inventário principal quando nenhum slot da hotbar estiver disponível.
- Manter a seleção apontando para o slot que recebeu o item.
- Exibir uma indicação visual de slot travado sem substituir a seleção vanilla.
- Exibir uma mensagem curta ao travar/destravar.
- Não interferir em uso, dano, consumo normal, scroll, seleção manual e demais interações não relacionadas a replacement automático.
- Ter testes de compatibilidade para Fabric, Forge e NeoForge na versão prioritária.

### Fora do MVP

- Regras por item, tags ou categorias.
- Travas de slots do inventário principal, armadura ou offhand.
- Perfis de travas por ferramenta/contexto.
- Configuração visual avançada ou editor de HUD.
- Garantia automática para qualquer mod que mutile a lista de inventário diretamente sem passar pelo contrato de integração.

## 3. Discovery técnico

### 3.1 Fluxo vanilla de pick em 1.21.1

No cliente, o middle-click entra em `Minecraft.pickBlock()`.

- Para um item que já está na hotbar, o vanilla apenas altera `Inventory.selected`.
- Para um item existente no inventário principal, o vanilla chama `MultiPlayerGameMode.handlePickItem(slot)`, que envia `ServerboundPickItemPacket`.
- No servidor, `ServerGamePacketListenerImpl.handlePickItem(...)` chama `Inventory.pickSlot(sourceSlot)` e sincroniza os dois slots e o slot selecionado.
- Para creative pick de um item que ainda não está no inventário, `Inventory.setPickedItem(...)` escolhe `Inventory.getSuitableHotbarSlot()`, podendo mover o conteúdo atual para um slot livre.
- `Inventory.getSuitableHotbarSlot()` não conhece travas: procura slots vazios a partir do slot selecionado e depois slots não encantados.
- `Inventory.placeItemBackInInventory(...)` também pode escolher a hotbar antes do inventário principal; ele não pode ser usado sem uma política filtrada, pois poderia atingir um slot travado.

Conclusão: não existe um único evento vanilla cancelável que cubra esses caminhos. O MVP precisa de uma política comum de destino e interceptações específicas para os pontos de mutação vanilla no cliente e no servidor.

### 3.2 Create Toolbox

Na versão local auditada do Create 6.0.10 para Minecraft 1.21.1:

- `ToolboxHandlerClient.onPickItem()` implementa um fluxo próprio para localizar o item no Toolbox.
- O cliente envia `ToolboxEquipPacket(toolboxPos, compartment, hotbarSlot)`.
- O servidor chama `ToolboxHandler.unequip(...)`, valida o slot recebido e conecta o jogador ao compartimento do Toolbox.
- O caminho de retorno pode usar `Inventory.setItem(hotbarSlot, ...)` por meio de `ItemReturnInvWrapper`.

Conclusão: Create Toolbox não é coberto apenas por interceptar `Minecraft.pickBlock()` ou `ServerboundPickItemPacket`. A compatibilidade exige uma integração opcional carregada somente quando Create estiver presente, com teste específico de equip, troca e unequip em slot travado.

### 3.3 HUD e mensagem

O HUD vanilla 1.21.1 renderiza a hotbar em `Gui.renderItemHotbar(...)` e a mensagem/overlay em uma região próxima à hotbar e à barra de experiência. A implementação deve:

- desenhar a marca de trava depois da hotbar vanilla, usando as coordenadas do HUD e não uma substituição do renderer vanilla;
- manter a seleção vanilla visível por cima ou claramente distinguível;
- usar a mensagem sobreposta da HUD para “slot travado”/“slot destravado”;
- não chamar o ToastManager do canto superior direito, salvo decisão posterior de UX.

### 3.4 Loader e versões

A estrutura recomendada é um projeto multiloader com:

```text
hotbar-locker/
  common/
  fabric/
  forge/
  neoforge/
```

O build pode usar Architectury Loom/Plugin como ferramenta de projeto. Architectury API runtime é opcional: ela pode ajudar em loader calls e networking, mas não elimina os Mixins/patches necessários para a semântica de inventário nem cobre automaticamente as diferenças de Forge, NeoForge e Fabric.

Para preservar compatibilidade e facilitar atualizações:

- o núcleo de regra, persistência lógica e algoritmo deve ficar em `common`;
- eventos de keybind, HUD, networking e Mixins ficam em adapters de loader/version;
- cada versão menor de Minecraft deve gerar artefato próprio;
- 1.21.1 é o primeiro alvo obrigatório;
- a expansão para 1.21.2+ será feita por matrizes de compilação/teste, não presumindo que um JAR seja binariamente compatível entre versões.

O projeto local Sable confirma uma convenção viável de `common` + `fabric` + `neoforge`, Java 21 e ModDevGradle/Loom em 1.21.1. Ele não prova, por si só, que o adapter Forge 1.21.1 terá os mesmos hooks; esse é um gate de Phase 0.

## 4. Definição de comportamento

### 4.1 Estado

- Cada jogador possui uma máscara de 9 bits; bit `n = 1` significa slot `n` travado.
- A máscara é autoridade do servidor e é persistida junto do jogador.
- O cliente mantém uma cópia somente para renderização e resposta imediata de input.
- A alteração deve ser feita por payload de rede validado no servidor, com confirmação servidor → cliente.

### 4.2 Algoritmo de destino

O serviço comum deve receber uma operação explícita, por exemplo `PICK_REPLACE`, `TOOLBOX_EQUIP` ou `OTHER_REGISTERED_REPLACEMENT`, e retornar um plano de movimentação:

1. preservar a preferência vanilla pelo slot selecionado quando ele for elegível;
2. percorrer a hotbar circularmente a partir do slot selecionado;
3. ignorar slots travados;
4. aceitar primeiro um slot vazio e, depois, um slot substituível de acordo com a operação;
5. se não houver destino na hotbar, procurar apenas os slots do inventário principal;
6. se também não houver espaço, executar o fallback seguro da operação sem sobrescrever slot travado.

O algoritmo nunca deve apagar ou sobrescrever silenciosamente um slot travado. Em caso de conflito, a operação deve abortar ou redirecionar de forma transacional.

### 4.3 Interações permitidas

Por padrão, “travado” significa protegido contra replacement automático. Uso normal do item, dano, consumo, cooldown, seleção manual e movimentação deliberada do jogador na tela de inventário não devem ser bloqueados pelo MVP. Um modo de “trava rígida contra cliques manuais” fica fora do MVP até haver uma decisão explícita, porque ele aumenta muito o risco de quebrar menus e mods.

### 4.4 Input

Proposta inicial:

- uma keybind configurável `Toggle hotbar slot lock` para o slot atualmente selecionado;
- tecla padrão sem conflito forte, sujeita a validação no protótipo;
- a seleção numérica continua selecionando slots travados normalmente;
- travar/destravar afeta o slot selecionado, sem trocar o item.

Se for desejado travar qualquer slot sem selecioná-lo, adicionar depois uma combinação de modificador + tecla numérica ou interação direta com a hotbar.

## 5. Arquitetura proposta

### Common

- `HotbarLockState`: máscara, serialização e operações puras.
- `HotbarLockService`: leitura/escrita por jogador e cálculo de destino.
- `ReplacementPlan`: plano transacional com origem, destino, fallback e motivo.
- `HotbarLockerApi`: API pública para integrações de mods.
- `HotbarLockNetwork`: contratos lógicos de sync e toggle.
- `HotbarLockPersistence`: contrato abstrato para guardar o estado no jogador.

### Adapters de loader

- registro de keybind;
- registro de payloads e envio servidor/cliente;
- ciclo de vida de jogador, login, respawn e troca de dimensão;
- evento/camada de renderização do HUD;
- declaração de Mixins e condições de carregamento por mod;
- config e mensagens de plataforma.

### Interceptações mínimas previstas

1. vanilla client pick/creative replacement;
2. vanilla server pick-slot swap;
3. sincronização de slots após uma operação redirecionada;
4. Create Toolbox equip/unequip, condicionado à presença do Create;
5. pontos adicionais identificados durante a matriz de mods.

Não usar um bloqueio indiscriminado de `Inventory.setItem(...)` como primeira solução. Esse método é compartilhado por muitas interações legítimas e um cancelamento genérico pode causar perda de item, dessync, menus quebrados ou incompatibilidade. A proteção genérica só deve ser adicionada se houver contexto de operação ou um mecanismo de rollback transacional comprovado.

## 6. Compatibilidade com outros mods

Serão definidos três níveis:

- Nível A — vanilla: comportamento obrigatório e testado.
- Nível B — integração explícita: mods com fluxo próprio, começando por Create Toolbox.
- Nível C — mod desconhecido: best effort; o mod não pode prometer proteção quando o terceiro escreve diretamente no inventário sem fornecer hook ou contexto.

A API pública deve permitir que uma integração peça um destino antes de escrever:

```java
OptionalInt destination = HotbarLockerApi.findReplacementSlot(
    player, requestedHotbarSlot, ReplacementReason.TOOLBOX_EQUIP);
```

Também deve existir uma forma de registrar uma operação atômica para que a integração não precise duplicar a regra de locks.

## 7. Critérios de aceitação do MVP

- Dado um slot travado e um slot livre à frente, middle-click coloca o item no primeiro slot livre permitido e seleciona esse slot.
- Dado um slot travado ocupado, nenhuma operação vanilla de pick o substitui.
- Dado todos os nove slots travados, o item vai para o inventário principal quando houver espaço.
- Dado todos os slots travados e inventário cheio, nenhum slot travado é sobrescrito e não há duplicação/perda silenciosa.
- Pick de um item que já está em slot travado seleciona o slot, sem mutar seu conteúdo.
- O estado sobrevive a salvar/reabrir mundo e reconectar ao servidor.
- Dois jogadores têm máscaras independentes.
- Uso, dano, consumo, scroll e seleção manual continuam funcionando.
- O ícone de trava aparece somente nos slots travados e não cobre a seleção de forma ambígua.
- A mensagem de toggle aparece acima da hotbar/barra de experiência e não deixa texto preso após sair do mundo.
- Create Toolbox respeita a trava em equip, troca e unequip quando Create está instalado.
- Sem Create instalado, nenhuma classe do Create é carregada e o mod inicia normalmente.
- Fabric, Forge e NeoForge 1.21.1 compilam e iniciam um cliente de teste.

## 8. Plano de desenvolvimento

### Phase 0 — gates de plataforma e contrato

- criar o esqueleto multiloader em `hotbar-locker`;
- validar build/run client de Fabric, Forge e NeoForge 1.21.1;
- confirmar hooks de persistência, payload, HUD e Mixins em cada loader;
- fechar o contrato de “trava automática” versus “trava manual”;
- confirmar se a mensagem será overlay vanilla ou ToastManager.

### Phase 1 — núcleo e vanilla

- implementar máscara, persistência, sync e keybind;
- implementar o algoritmo puro com testes unitários;
- redirecionar creative pick e survival pick;
- renderizar a indicação visual;
- adicionar GameTest/integration harness e checklist manual do cliente.

### Phase 2 — Create e compatibilidade

- criar compatibilidade opcional do Create Toolbox;
- testar equip, unequip, troca de slot, distância e reconnect;
- mapear outros mods de interesse somente após reproduzir um fluxo que bypassa o caminho vanilla.

### Phase 3 — matriz de versões

- portar para as versões 1.21.x selecionadas;
- separar Mixins/renderer quando assinaturas mudarem;
- manter um artefato e uma suíte de testes por versão/loader.

## 9. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---:|---|
| Interceptar `setItem` global quebra menus | Alto | Interceptar operações conhecidas; usar API de integração; testar dessync |
| Cliente e servidor discordam sobre destino | Alto | Estado autoritativo no servidor, plano transacional e resync |
| Mixins mudam entre 1.21.x | Médio/alto | adapters por versão, não um único JAR universal |
| Create escreve por pacote próprio | Alto | compatibilidade opcional dedicada e teste de regressão |
| “Toast” tem significado visual ambíguo | Médio | usar overlay acima da hotbar no MVP e documentar a decisão |
| Forge diverge de NeoForge em 1.21.1 | Alto | gate de Phase 0 antes de assumir paridade de hooks |

## 10. Dúvidas para validar antes do código

1. A trava deve proteger somente replacements automáticos, como proposto, ou também impedir que o jogador arraste/remova manualmente o item na tela de inventário?
2. O default da keybind pode ser definido pelo projeto ou deve começar sem tecla atribuída?
3. “Ir para o inventário” significa procurar exclusivamente os slots 9–35, deixando a hotbar inteira protegida quando todas as travas estiverem ativas?
4. Forge 1.21.1 é requisito de lançamento junto com Fabric/NeoForge ou pode entrar como adapter experimental após o MVP vanilla?
5. Create Toolbox é a única integração obrigatória inicial ou existe uma lista de mods que deve entrar na matriz de Phase 2?

## 11. Fontes de discovery

- Código local de Minecraft 1.21.1 remapeado: métodos `Minecraft.pickBlock`, `Inventory.setPickedItem`, `Inventory.pickSlot`, `Inventory.getSuitableHotbarSlot`, `MultiPlayerGameMode.handlePickItem` e `ServerGamePacketListenerImpl.handlePickItem`.
- Código local do Create 6.0.10-280: `ToolboxHandlerClient.onPickItem`, `ToolboxEquipPacket.handle` e `ItemReturnInvWrapper`.
- [Architectury API](https://github.com/architectury/architectury-api) e [Architectury Loom](https://github.com/architectury/architectury-loom) como referências de toolchain multiloader.
- [Fabric key mappings](https://docs.fabricmc.net/develop/key-mappings) e [Fabric HUD rendering](https://docs.fabricmc.net/develop/rendering/hud).
- [NeoForged key mappings](https://docs.neoforged.net/docs/1.21.3/misc/keymappings/).
