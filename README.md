# Pokédex ABB

Pokédex em Java que armazena os Pokémon em uma **Árvore Binária de Busca (ABB)**, com interface gráfica (Swing).
Trabalho em grupo da faculdade, entregue em sprints.

## Divisão do grupo
- **Guilherme Rodrigues Leite**: classe `Pokemon`, dados (CSV) e carga inicial na ABB
- **Luiz Carlos**: ABB (inserção, remoção e busca)
- **João Augusto Nass**: ABB e interface de exibição
- **Anna Clara**: buscas por nome e por número da Dex
- **Thiago Nunes**: integração, testes e documentação

## Como funciona
- Os 1025 Pokémon do `../../Downloads/pokemons.csv` são carregados e inseridos na ABB, que usa o **número da Pokédex** como chave.
- Para a árvore não virar uma lista (o CSV está em ordem crescente), a carga usa `CarregadorPokemon.ordemBalanceada`, que insere a mediana primeiro. Resultado: altura **11** em vez de **1025**.
- A busca por número usa a ABB (`buscar`). A busca por nome percorre a árvore (`buscarPorNome`), e o campo de busca também aceita o começo do nome.
- A lista lateral usa o percurso em ordem da ABB (`emOrdem`).

## Estrutura
- `../Downloads/src/PokePackage`
  - `Pokemon`: dados de um Pokémon
  - `No` e `ArvoreBinaria`: a ABB (`inserir`, `buscar`, `buscarPorNome`, `remover`, `emOrdem`, `altura`)
  - `CarregadorPokemon`: lê o `../../Downloads/pokemons.csv` e define a ordem balanceada de inserção
  - `BaixadorImagens`: baixa as imagens oficiais (PokeAPI), inclusive as das formas Mega
  - `PokemonViewer`: interface da Pokédex (busca, filtro por região, descoberta de Pokémon e botão **Batalhar**)
  - Batalha (extra): `Batalha`, `DadosBatalha`, `Golpe`, `Atributos`, `SeletorBatalha`, `TelaBatalha`
  - Testes e demonstrações: `ArvoreTeste`, `AlturaDemo`, `BatalhaDemo`, `BatalhaConsole`
- `../../Downloads/pokemons.csv`: 1025 Pokémon (separador `;`, UTF-8)
- `../Downloads/dados`: dados da batalha (`atributos.csv`, `golpes.csv`, `golpes_pokemon.csv`, `vantagens.csv`, `megas.csv`)
- `images`: criada pelo `BaixadorImagens` (não vai para o Git)

## Como rodar
A pasta de trabalho (working directory) deve ser a **raiz do projeto**, onde estão `../../Downloads/pokemons.csv`, `dados` e `images/`.

1. Rode `BaixadorImagens` uma vez (cria a pasta `images` com a arte dos Pokémon e das Megas).
2. Rode `PokemonViewer`.

Na tela:
- Digite um nome ou número na busca e aperte Enter. Pokémon ainda não descobertos aparecem como silhueta; use **Cadastrar** (ou escaneie) para descobri-los.
- O seletor de região filtra a lista.
- **Batalhar** (precisa de pelo menos 2 Pokémon descobertos): monte um time de até 3 Pokémon e lute contra um time adversário. Você escolhe os golpes, o adversário é controlado por uma IA simples. Alguns Pokémon podem usar **Mega Evoluir** (uma vez por luta; tecla `M`). Os golpes também funcionam com as teclas `1` a `4`.

## Testes
- `ArvoreTeste`: testa a ABB sozinha, sem a interface (inserir, buscar, remover, em ordem, altura). Também compara a altura com a ordem do CSV (1025) e com a ordem balanceada (11). Termina com "Todos os testes passaram."
- `AlturaDemo`: compara a altura da árvore por número e por nome.
- `BatalhaDemo` e `BatalhaConsole`: testam o cálculo de dano e uma luta completa no console.

## Documentação
- Sprint 1: definição do projeto (pasta `documents/`).
- Sprint 2: implementação e testes da ABB.
