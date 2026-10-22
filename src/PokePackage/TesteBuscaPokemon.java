package PokePackage;

import java.util.Comparator;
import java.util.List;

public class TesteBuscaPokemon {

    static int falhas = 0;

    static void confere(String descricao, boolean ok) {
        System.out.println((ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }

    public static void main(String[] args) throws Exception {

        // Parte 1: árvore pequena para testar as buscas
        ArvoreBinaria arvore = new ArvoreBinaria();

        Pokemon pikachu = new Pokemon(
            25, "Pikachu", "Kanto",
            "Elétrico", null, 0.4, 6.0,
            "Pokémon elétrico", "Armazena eletricidade"
        );

        Pokemon bulbasaur = new Pokemon(
            1, "Bulbasaur", "Kanto",
            "Planta", "Venenoso", 0.7, 6.9,
            "Pokémon inicial", "Possui uma semente nas costas"
        );

        arvore.inserir(pikachu);
        arvore.inserir(bulbasaur);

        confere("número 25 acha Pikachu",
            BuscaPokemon.buscarPorNumero(arvore, 25) == pikachu);
        confere("nome \"bulbasaur\" (minúsculo) acha Bulbasaur",
            BuscaPokemon.buscarPorNome(arvore, "bulbasaur") == bulbasaur);
        confere("número 999 não existe e devolve null",
            BuscaPokemon.buscarPorNumero(arvore, 999) == null);
        confere("árvore null devolve null",
            BuscaPokemon.buscarPorNome(null, "pikachu") == null);

        // Parte 2: os 1025 Pokémon do CSV (rode da raiz do projeto)
        List<Pokemon> todos = CarregadorPokemon.carregar("pokemons.csv");
        ArvoreBinaria completa = new ArvoreBinaria();
        for (Pokemon p : CarregadorPokemon.ordemBalanceada(todos, Comparator.comparingInt(Pokemon::getNumeroDex))) {
            completa.inserir(p);
        }

        confere("número 6 acha Charizard",
            BuscaPokemon.buscarPorNumero(completa, 6).getNome().equals("Charizard"));
        confere("nome \"CHARIZARD\" (maiúsculo) acha Charizard",
            BuscaPokemon.buscarPorNome(completa, "CHARIZARD").getNumeroDex() == 6);
        confere("nome sem acento \"flabebe\" acha Flabébé",
            BuscaPokemon.buscarPorNome(completa, "flabebe").getNumeroDex() == 669);
        confere("nome \"xyz\" não existe e devolve null",
            BuscaPokemon.buscarPorNome(completa, "xyz") == null);

        List<Pokemon> char_ = BuscaPokemon.buscarPorPrefixo(completa, "char");
        confere("prefixo \"char\" acha Charmander, Charmeleon, Charizard, Charjabug e Charcadet, em ordem de Dex",
            char_.stream().map(Pokemon::getNumeroDex).toList().equals(List.of(4, 5, 6, 737, 935)));

        confere("buscar(\"25\") usa o número",
            BuscaPokemon.buscar(completa, "25").getNome().equals("Pikachu"));
        confere("buscar(\"mr. mime\") usa o nome inteiro",
            BuscaPokemon.buscar(completa, "mr. mime").getNumeroDex() == 122);
        confere("buscar(\"charm\") devolve o primeiro do prefixo (Charmander)",
            BuscaPokemon.buscar(completa, "charm").getNumeroDex() == 4);
        confere("buscar(\"   \") devolve null",
            BuscaPokemon.buscar(completa, "   ") == null);

        System.out.println(falhas == 0 ? "\nTodos os testes de busca passaram." : "\n" + falhas + " teste(s) falharam.");
    }
}
