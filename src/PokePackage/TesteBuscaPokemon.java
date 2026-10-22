
package PokePackage;

public class TesteBuscaPokemon {

    public static void main(String[] args) {

        // Cria a árvore para testar as buscas
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

        // Coloca os Pokémon na árvore
        arvore.inserir(pikachu);
        arvore.inserir(bulbasaur);

        // Testa a busca pelo número
        Pokemon resultadoNumero =
            BuscaPokemon.buscarPorNumero(arvore, 25);

        if (resultadoNumero != null) {
            System.out.println("Encontrado pelo número: "
                + resultadoNumero.getNome());
        } else {
            System.out.println("Pokémon não encontrado!");
        }

        // Testa a busca pelo nome
        Pokemon resultadoNome =
            BuscaPokemon.buscarPorNome(arvore, "bulbasaur");

        if (resultadoNome != null) {
            System.out.println("Encontrado pelo nome: "
                + resultadoNome.getNome());
        } else {
            System.out.println("Pokémon não encontrado!");
        }
    }
}
