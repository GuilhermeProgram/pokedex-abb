
package PokePackage;

public class BuscaPokemon {

    // Busca o Pokémon pelo número
    public static Pokemon buscarPorNumero(
            ArvoreBinaria arvore, int numero) {

        if (arvore == null) {
            return null;
        }

        return arvore.buscar(numero);
    }

    // Busca o Pokémon pelo nome
    public static Pokemon buscarPorNome(
            ArvoreBinaria arvore, String nome) {

        if (arvore == null || nome == null) {
            return null;
        }

        return buscarNomeNo(arvore.raiz, nome.trim());
    }

    // Vai passando pelos nós até achar o nome
    private static Pokemon buscarNomeNo(
            No atual, String nome) {

        if (atual == null) {
            return null;
        }

        // Confere se é o Pokémon que estamos procurando
        if (atual.pokemon.getNome()
                .equalsIgnoreCase(nome)) {
            return atual.pokemon;
        }

        // Procura primeiro do lado esquerdo
        Pokemon encontrado = buscarNomeNo(
                atual.esquerda, nome);

        if (encontrado != null) {
            return encontrado;
        }

        // Se não achou, procura do lado direito
        return buscarNomeNo(atual.direita, nome);
    }
}
