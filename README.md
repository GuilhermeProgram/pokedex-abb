# Pokédex ABB

Pokédex em Java que armazena os Pokémon em uma **Árvore Binária de Busca (ABB)**, com interface gráfica (Swing).
Trabalho em grupo da faculdade, entregue em sprints.

## Divisão do grupo
- **Guilherme Rodrigues Leite**: classe `Pokemon`, dados (CSV) e carga inicial na ABB
- **Luiz Carlos**: ABB (inserção, remoção e busca)
- **João Augusto Nass**: ABB e interface de exibição
- **Anna Clara**: buscas por nome e por número da Dex
- **Thiago Nunes**: integração, testes e documentação

## Estrutura
- `src/PokePackage/Pokemon.java`: dados de um Pokémon
- `src/PokePackage/CarregadorPokemon.java`: lê o `pokemons.csv`
- `src/PokePackage/BaixadorImagens.java`: baixa as imagens oficiais (PokeAPI)
- `src/PokePackage/PokemonViewer.java`: interface da Pokédex
- `pokemons.csv`: 1025 Pokémon (separador `;`, UTF-8)

## Como rodar
1. Rode `BaixadorImagens` uma vez (cria a pasta `images/`).
2. Rode `PokemonViewer`.

A pasta de trabalho (working directory) deve ser a raiz do projeto, onde estão `pokemons.csv` e `images/`.
