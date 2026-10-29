# Phase 1 — checklist manual

Execute esta lista em um mundo criativo e, depois, em um mundo de sobrevivência para cada loader suportado.

1. Em `Options > Controls > Hotbar Locker`, atribua uma tecla a `Toggle hotbar slot lock`.
2. Selecione um slot, use a tecla e confirme a mensagem acima da barra de experiência a moldura dourada animada (da base ao topo) e o cadeado no canto do slot. O item, a contagem e a barra de durabilidade devem continuar visíveis, assim como a seleção vanilla. Confirme o som de experiência mais grave ao travar e mais agudo ao destravar.
3. Trave um slot ocupado, deixe o próximo slot vazio e faça middle-click em um bloco novo: o item deve ir ao primeiro slot desbloqueado e esse slot deve ser selecionado.
4. Faça middle-click em um item já presente em uma hotbar travada: o item deve apenas ser selecionado.
5. Trave os nove slots e faça creative pick: nenhum slot travado pode mudar; com espaço disponível, o item deve aparecer apenas no inventário principal.
6. Trave os nove slots, deixe o inventário principal cheio e faça creative pick: não pode haver sobrescrita, duplicação ou perda silenciosa de um slot travado.
7. Em sobrevivência, deixe o item alvo no inventário principal e faça pick; confirme que um slot travado não é usado para o swap.
8. Salve e reabra o mundo, reconecte e morra/respawne: as travas devem continuar visíveis e efetivas.
9. Confirme que scroll, teclas numéricas, uso, consumo, dano e movimentação manual de itens no inventário continuam normais.
10. No inventário Criativo, use Shift-clique na lixeira: os slots travados devem permanecer e os demais devem ser limpos.
11. Troque de dimensão e confirme a preservação das travas. Com todos os slots travados, faça pick Criativo e reabra o inventário: o item redirecionado ao inventário principal deve persistir também no servidor.

Registre loader, cenário, resultado e o `latest.log` para qualquer falha. A integração Create Toolbox pertence à Phase 2.
