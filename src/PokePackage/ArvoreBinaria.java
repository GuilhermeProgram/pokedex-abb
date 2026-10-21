
public class ArvoreBinaria {

    No raiz;

    // INSERÇÃO
    public void inserir(Pokemon pokemon) {
        raiz = inserirNo(raiz, pokemon);
    }

    private No inserirNo(No atual, Pokemon pokemon) {

        if (atual == null) {
            return new No(pokemon);
        }

        if (pokemon.numero < atual.pokemon.numero) {
            atual.esquerda = inserirNo(atual.esquerda, pokemon);
        }
        else if (pokemon.numero > atual.pokemon.numero) {
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

        if (numero == atual.pokemon.numero) {
            return atual.pokemon;
        }

        if (numero < atual.pokemon.numero) {
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

        if (numero < atual.pokemon.numero) {
            atual.esquerda = removerNo(atual.esquerda, numero);
        }
        else if (numero > atual.pokemon.numero) {
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

            // possui dois filhos
            No sucessor = encontrarMenor(atual.direita);

            atual.pokemon = sucessor.pokemon;

            atual.direita = removerNo(
                atual.direita,
                sucessor.pokemon.numero
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
}
