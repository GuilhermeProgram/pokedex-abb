package pokedex.abb;

import pokedex.modelo.Pokemon;

public class No {
    Pokemon pokemon;
    No esquerda;
    No direita;

    public No(Pokemon pokemon){
        this.pokemon = pokemon;
        this.esquerda = null;
        this.direita = null;
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    public No getEsquerda() {
        return esquerda;
    }

    public No getDireita() {
        return direita;
    }
}
