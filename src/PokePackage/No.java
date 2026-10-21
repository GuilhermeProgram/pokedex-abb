package PokePackage;

public class No {
    Pokemon pokemon;
    No esquerda;
    No direita;

    public No(Pokemon pokemon){
        this.pokemon = pokemon;
        this.esquerda = null;
        this.direita = null;

    }
}
