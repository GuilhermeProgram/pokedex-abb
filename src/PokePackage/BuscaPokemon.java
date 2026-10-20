package PokePackage;

import java.util.List;

public class BuscaPokemon {

    // Busca um Pokémon pelo número da Pokédex
    public static Pokemon buscarPorNumero(
            List<Pokemon> pokemons, int numero) {

        for (Pokemon pokemon : pokemons) {
            if (pokemon.getNumeroDex() == numero) {
                return pokemon;
            }
        }

        return null;
    }

    // Busca um Pokémon pelo nome
    public static Pokemon buscarPorNome(
            List<Pokemon> pokemons, String nome) {

        if (nome == null) {
            return null;
        }

        for (Pokemon pokemon : pokemons) {
            if (pokemon.getNome().equalsIgnoreCase(nome.trim())) {
                return pokemon;
            }
        }

        return null;
    }
}