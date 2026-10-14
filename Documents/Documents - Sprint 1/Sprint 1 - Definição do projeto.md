# Sprint 1 — Definição do Projeto

## 1. Nome e descrição do sistema
**Nome:** Pokédex

O sistema será uma Pokédex digital que permitirá ao usuário pesquisar Pokémon pelo nome ou número Dex. Ao selecionar um Pokémon, serão exibidas suas principais informações e especificações, como nome, número, tipos e demais características cadastradas.

## 2. Estrutura de dados
**Árvore Binária de Busca (ABB)**

A ABB será utilizada para armazenar e pesquisar os Pokémon. Serão consideradas as duas principais formas de pesquisa do sistema: número Dex e nome. A organização dos dados na árvore permitirá realizar buscas de forma eficiente, relacionando diretamente a estrutura de dados com a principal funcionalidade da Pokédex.

## 3. Divisão de responsabilidades
- **Guilherme Rodrigues Leite:** classe `Pokemon`, dados (CSV) e carga inicial na ABB.
- **Luiz Carlos:** ABB (inserção, remoção e busca).
- **João Augusto Nass:** ABB e interface de exibição.
- **Anna Clara:** pesquisas por nome e número Dex.
- **Thiago Nunes:** integração das partes, testes e documentação.

## 4. Funcionamento do sistema
Os dados dos Pokémon serão carregados e armazenados na ABB. O usuário poderá pesquisar um Pokémon informando seu nome ou número Dex. O sistema realizará a busca na estrutura e, caso o Pokémon seja encontrado, exibirá suas informações e especificações na tela.

**Fluxo:** Usuário pesquisa → Sistema recebe nome/número → ABB realiza a busca → Pokémon encontrado → Informações são exibidas.

## 5. Principal dificuldade esperada
A principal dificuldade será implementar corretamente a Árvore Binária de Busca e integrá-la às pesquisas por nome e número Dex, garantindo que os dados sejam armazenados e encontrados corretamente.
