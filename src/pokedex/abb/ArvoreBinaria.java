package pokedex.abb;

import java.util.ArrayList;
import java.util.List;

import pokedex.modelo.Pokemon;

public class ArvoreBinaria {

    No raiz;

    /** Raiz da árvore (usada por quem precisa percorrê-la, como a BuscaPokemon). */
    public No getRaiz() {
        return raiz;
    }

    // INSERÇÃO
    public void inserir(Pokemon pokemon) {
        raiz = inserirNo(raiz, pokemon);
    }

    private No inserirNo(No atual, Pokemon pokemon) {

        if (atual == null) {
            return new No(pokemon);
        }

        if (pokemon.getNumeroDex()< atual.pokemon.getNumeroDex()) {
            atual.esquerda = inserirNo(atual.esquerda, pokemon);
        }
        else if (pokemon.getNumeroDex() > atual.pokemon.getNumeroDex()) {
            atual.direita = inserirNo(atual.direita, pokemon);
        }

        return atual;
    }

    // BUSCA
    public Pokemon buscar(int numero) {
        return buscarNo(raiz, numero);
    }

    private Pokemon buscarNo(No atual, int numero) {

        if (atual == null) {
            return null;
        }

        if (numero == atual.pokemon.getNumeroDex()) {
            return atual.pokemon;
        }

        if (numero < atual.pokemon.getNumeroDex()) {
            return buscarNo(atual.esquerda, numero);
        }

        return buscarNo(atual.direita, numero);
    }

    // REMOÇÃO
    public void remover(int numero) {
        raiz = removerNo(raiz, numero);
    }

    private No removerNo(No atual, int numero) {

        if (atual == null) {
            return null;
        }

        if (numero < atual.pokemon.getNumeroDex()) {
            atual.esquerda = removerNo(atual.esquerda, numero);
        }
        else if (numero > atual.pokemon.getNumeroDex()) {
            atual.direita = removerNo(atual.direita, numero);
        }
        else {

            // sem filhos
            if (atual.esquerda == null && atual.direita == null) {
                return null;
            }

            // possui apenas um filho
            if (atual.esquerda == null) {
                return atual.direita;
            }

            if (atual.direita == null) {
                return atual.esquerda;
            }

            No sucessor = encontrarMenor(atual.direita);

            atual.pokemon = sucessor.pokemon;

            atual.direita = removerNo(
                atual.direita,
                sucessor.pokemon.getNumeroDex()
            );
        }

        return atual;
    }

    // encontra o menor nó de uma subárvore
    private No encontrarMenor(No atual) {

        while (atual.esquerda != null) {
            atual = atual.esquerda;
        }

        return atual;
    }

    public Pokemon buscarPorNome(String nome) {
        return buscarPorNomeNo(raiz, nome);
    }

    private Pokemon buscarPorNomeNo(No atual, String nome) {

        if (atual == null) {
            return null;
        }

        if (atual.pokemon.getNome().equalsIgnoreCase(nome)) {
            return atual.pokemon;
        }

        Pokemon encontrado = buscarPorNomeNo(atual.esquerda, nome);

        if (encontrado != null) {
            return encontrado;
        }

        return buscarPorNomeNo(atual.direita, nome);
    }

    // PERCURSO EM ORDEM (devolve os Pokémon em ordem crescente do número da Pokédex)
    public List<Pokemon> percorrerEmOrdem() {
        List<Pokemon> lista = new ArrayList<>();
        percorrerEmOrdem(raiz, lista);
        return lista;
    }

    private void percorrerEmOrdem(No atual, List<Pokemon> lista) {
        if (atual == null) {
            return;
        }
        percorrerEmOrdem(atual.esquerda, lista);
        lista.add(atual.pokemon);
        percorrerEmOrdem(atual.direita, lista);
    }

    // ALTURA (quantidade de níveis; árvore vazia = 0)
    public int altura() {
        return altura(raiz);
    }

    private int altura(No atual) {
        return atual == null ? 0 : 1 + Math.max(altura(atual.esquerda), altura(atual.direita));
    }
}
