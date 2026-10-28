package pokedex.teste;

import pokedex.abb.ArvoreBinaria;
import pokedex.modelo.Pokemon;

public class TesteABB {

    public static void main(String[] args) {

        ArvoreBinaria arvore = new ArvoreBinaria();

        Pokemon pikachu = new Pokemon(
            25, "Pikachu", "Kanto",
            "Elétrico", "",
            0.4, 6.0, "Pokémon elétrico", ""
        );

        Pokemon charmander = new Pokemon(
            4, "Charmander", "Kanto",
            "Fogo", "",
            0.6, 8.5, "Pokémon de fogo", ""
        );

        // Testar inserção

        arvore.inserir(pikachu);
        arvore.inserir(charmander);

        // Testar busca e inserção
        Pokemon encontrado = arvore.buscar(25);

        if (encontrado != null) {
            System.out.println(encontrado.getNome() + " inserido e encontrado em numeroDex 25");
        }

        // Testar remoção
        arvore.remover(25);

        System.out.println("Numerodex 25 = " + arvore.buscar(25) + " (removido com sucesso)");
    }
}
