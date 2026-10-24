# Pokédex ABB

Pokédex em Java que armazena os Pokémon em uma **Árvore Binária de Busca (ABB)**, com interface gráfica (Swing).
Trabalho em grupo da faculdade, entregue em sprints.

## Divisão do grupo
- **Guilherme Rodrigues Leite**: classe `Pokemon`, dados (CSV), carga na ABB, interface, PC e integração
- **Luiz Carlos**: ABB (inserção, remoção e busca)
- **João Augusto Nass**: ABB e interface de exibição
- **Anna Clara**: buscas por nome e por número da Dex (`BuscaPokemon`)
- **Thiago Nunes**: integração, testes e documentação

## Como funciona
O sistema usa **duas ABBs**, ambas com o **número da Pokédex** como chave:
- **Catálogo**: os 1025 Pokémon do `pokemons.csv`. Para a árvore não virar uma lista (o CSV está em ordem crescente), a carga usa `CarregadorPokemon.ordemBalanceada`, que insere a mediana primeiro. Resultado: altura **11** em vez de **1025**. É a árvore usada nas buscas.
- **PC**: os Pokémon **capturados**. Começa vazia. **Cadastrar** (o scan) chama `inserir`; **Liberar** chama `remover`. A janela do PC mostra os capturados em caixas de 6 x 5 (percurso em ordem, `percorrerEmOrdem`) e a altura atual da árvore.

Buscas (`BuscaPokemon`): número usa a ABB (`buscar`); texto procura o nome exato e depois o começo do nome ("char" acha Charmander), sem diferenciar acentos nem maiúsculas.

## Estrutura
- `../Downloads/src/PokePackage`
  - `Pokemon`: dados de um Pokémon
  - `No` e `ArvoreBinaria`: a ABB (`inserir`, `buscar`, `buscarPorNome`, `remover`, `percorrerEmOrdem`, `altura`)
  - `BuscaPokemon`: buscas por número, nome, prefixo e busca geral da interface
  - `CarregadorPokemon`: lê o `pokemons.csv` e define a ordem balanceada de inserção
  - `BaixadorImagens`: baixa as imagens oficiais, as das formas Mega e os sprites pixelados (PokeAPI)
  - `PokemonViewer`: interface da Pokédex (busca, filtro por região, descoberta de Pokémon, **PC** e **Batalhar**)
  - `JanelaPC`: o PC (caixas com sprites pixelados e botão **Liberar**)
  - Batalha (extra): `Batalha`, `DadosBatalha`, `Golpe`, `Status`, `EstadoPokemon`, `Atributos`, `SeletorBatalha`, `TelaBatalha`
  - Testes e demonstrações: `ArvoreTeste`, `TesteBuscaPokemon`, `TesteBatalha`, `BatalhaConsole`
- `pokemons.csv`: 1025 Pokémon (separador `;`, UTF-8)
- `dados`: dados da batalha (`atributos.csv`, `golpes.csv`, `golpes_pokemon.csv`, `vantagens.csv`, `megas.csv`)
- `ferramentas`: `gerar_golpes.py` (gera `golpes.csv` e `golpes_pokemon.csv`) e `smogon/` (dados do Smogon que ele usa)
- `images`: criada pelo `BaixadorImagens` (artes, `mini/` e `pixel/`; não vai para o Git)

## Como rodar
A pasta de trabalho (working directory) deve ser a **raiz do projeto**, onde estão `pokemons.csv`, `dados` e `images/`.

1. Rode `BaixadorImagens` uma vez (cria a pasta `images` com a arte dos Pokémon, das Megas e os sprites pixelados).
2. Rode `PokemonViewer`.

Na tela:
- Digite um nome ou número na busca e aperte Enter. Pokémon ainda não descobertos aparecem como silhueta; use **Cadastrar** (o scan) para descobri-los e colocá-los no PC.
- **PC**: abre as caixas com os Pokémon capturados. Clique num deles e use **Liberar** para soltá-lo (ele volta a ser silhueta). Setas do teclado movem a seleção; `PageUp`/`PageDown` trocam de caixa.
- O seletor de região filtra a lista.
- **Batalhar** (precisa de pelo menos 1 Pokémon no PC): monte um time de até 6 Pokémon, escolhidos entre os que você capturou, e lute contra um time do mesmo tamanho sorteado entre **todos** os Pokémon (capturados ou não; cada adversário é achado pela ABB do catálogo). Na tela de batalha você escolhe **Lutar**, **Pokémon** (trocar), **Mega** ou **Fugir**; o adversário é controlado por uma IA simples. Alguns Pokémon podem usar **Mega Evoluir** (uma vez por luta). Os sprites pixelados (frente e costas) são baixados por `BaixadorImagens`.

## Golpes e batalha
- Catálogo de 331 golpes (`dados/golpes.csv`) com poder, precisão, prioridade, **PP** e efeito, baseado no Smogon Strategy Dex (formato Champions). Cada Pokémon tem 4 golpes (`dados/golpes_pokemon.csv`).
- Golpes de dano (físico ou especial) e golpes de **status**. Efeitos: queimar, paralisar, envenenar, dormir, congelar, subir/descer atributos (-6 a +6 níveis), curar, drenar e recuo.
- Condições (QMD, PAR, PSN, SLP, CGL) e PP continuam valendo quando o Pokémon sai e volta; os níveis de atributo zeram na troca, como nos jogos. Sem PP em todos os golpes, o Pokémon usa o Esforço (Struggle).
- Os 4 golpes de cada Pokémon vêm do conjunto recomendado pelo Smogon (quando existe) e, no que faltar, de uma regra: golpe do tipo do Pokémon (STAB) na categoria em que ele é mais forte, um golpe de cobertura e um de utilidade conforme o papel (rápido e forte, resistente...). Só entram golpes que ele aprende. A regra está em `ferramentas/gerar_golpes.py`; para regenerar os CSV: `python ferramentas/gerar_golpes.py` (na raiz do projeto).
- A IA do adversário compara dano esperado, cura quando está ferida, aumenta atributos quando está saudável e usa condições em quem ainda não tem uma.

## Testes
- `ArvoreTeste`: testa a ABB sozinha, sem a interface (inserir, buscar, remover, em ordem, altura). Também compara a altura com a ordem do CSV (1025) e com a ordem balanceada (11). Termina com "Todos os testes passaram."
- `TesteBuscaPokemon`: testa as buscas (número, nome, prefixo, acentos). Termina com "Todos os testes de busca passaram."
- `TesteBatalha`: confere os dados dos golpes (4 por Pokémon, PP, efeitos), PP/Esforço, níveis de atributo, condições, recuo e dreno, e simula 3000 lutas completas. Termina com "Todos os testes de batalha passaram".
- `BatalhaConsole`: roda uma luta completa no console (usa o cálculo de dano da batalha).

## Documentação
- Sprint 1: definição do projeto (pasta `documents/`).
- Sprint 2: implementação e testes da ABB.
