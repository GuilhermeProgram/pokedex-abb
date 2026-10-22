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
- **PC**: os Pokémon **capturados**. Começa vazia. **Cadastrar** (o scan) chama `inserir`; **Liberar** chama `remover`. A janela do PC mostra os capturados em caixas de 6 x 5 (percurso em ordem, `emOrdem`) e a altura atual da árvore.

Buscas (`BuscaPokemon`): número usa a ABB (`buscar`); texto procura o nome exato e depois o começo do nome ("char" acha Charmander), sem diferenciar acentos nem maiúsculas.

## Estrutura
- `../Downloads/src/PokePackage`
  - `Pokemon`: dados de um Pokémon
  - `No` e `ArvoreBinaria`: a ABB (`inserir`, `buscar`, `buscarPorNome`, `remover`, `emOrdem`, `altura`)
  - `BuscaPokemon`: buscas por número, nome, prefixo e busca geral da interface
  - `CarregadorPokemon`: lê o `pokemons.csv` e define a ordem balanceada de inserção
  - `BaixadorImagens`: baixa as imagens oficiais, as das formas Mega e os sprites pixelados (PokeAPI)
  - `PokemonViewer`: interface da Pokédex (busca, filtro por região, descoberta de Pokémon, **PC** e **Batalhar**)
  - `JanelaPC`: o PC (caixas com sprites pixelados e botão **Liberar**)
  - Batalha (extra): `Batalha`, `DadosBatalha`, `Golpe`, `Atributos`, `SeletorBatalha`, `TelaBatalha`
  - Testes e demonstrações: `ArvoreTeste`, `TesteBuscaPokemon`, `BatalhaConsole`
- `pokemons.csv`: 1025 Pokémon (separador `;`, UTF-8)
- `dados`: dados da batalha (`atributos.csv`, `golpes.csv`, `golpes_pokemon.csv`, `vantagens.csv`, `megas.csv`)
- `images`: criada pelo `BaixadorImagens` (artes, `mini/` e `pixel/`; não vai para o Git)

## Como rodar
A pasta de trabalho (working directory) deve ser a **raiz do projeto**, onde estão `pokemons.csv`, `dados` e `images/`.

1. Rode `BaixadorImagens` uma vez (cria a pasta `images` com a arte dos Pokémon, das Megas e os sprites pixelados).
2. Rode `PokemonViewer`.

Na tela:
- Digite um nome ou número na busca e aperte Enter. Pokémon ainda não descobertos aparecem como silhueta; use **Cadastrar** (o scan) para descobri-los e colocá-los no PC.
- **PC**: abre as caixas com os Pokémon capturados. Clique num deles e use **Liberar** para soltá-lo (ele volta a ser silhueta). Setas do teclado movem a seleção; `PageUp`/`PageDown` trocam de caixa.
- O seletor de região filtra a lista.
- **Batalhar** (precisa de pelo menos 2 Pokémon no PC): monte um time de até 3 Pokémon e lute contra um time adversário. Você escolhe os golpes, o adversário é controlado por uma IA simples. Alguns Pokémon podem usar **Mega Evoluir** (uma vez por luta; tecla `M`). Os golpes também funcionam com as teclas `1` a `4`.

## Testes
- `ArvoreTeste`: testa a ABB sozinha, sem a interface (inserir, buscar, remover, em ordem, altura). Também compara a altura com a ordem do CSV (1025) e com a ordem balanceada (11). Termina com "Todos os testes passaram."
- `TesteBuscaPokemon`: testa as buscas (número, nome, prefixo, acentos). Termina com "Todos os testes de busca passaram."
- `BatalhaConsole`: roda uma luta completa no console (usa o cálculo de dano da batalha).

## Documentação
- Sprint 1: definição do projeto (pasta `documents/`).
- Sprint 2: implementação e testes da ABB.
